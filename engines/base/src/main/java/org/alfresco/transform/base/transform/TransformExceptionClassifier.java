/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2026 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * -
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * -
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * -
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * -
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transform.base.transform;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Classifies a transformer exception as a bad document ({@code 422}, not retried by the T-Router) or an engine failure ({@code 500}, retried).
 * <p/>
 * Unrecognised stays {@code 500}; an infrastructure match in the cause chain wins. Matches by class name, so base has no Tika/PDFBox/POI dependency.
 * <p/>
 * Defaults are in {@link ClassificationDefaults#RESOURCE}; properties under {@code transform.failure.classification} replace a list, and {@code contributions} extend it.
 */
@Component
@ConfigurationProperties(prefix = "transform.failure.classification")
// `logger` is the codebase convention; the method count is that of a configuration-properties bean.
@SuppressWarnings({"PMD.FieldNamingConventions", "PMD.TooManyMethods", "PMD.GodClass"})
class TransformExceptionClassifier
{
    private static final Logger logger = LoggerFactory.getLogger(TransformExceptionClassifier.class);

    private boolean enabled = true;
    private List<String> classNames = new ArrayList<>(ClassificationDefaults.CLASS_NAMES);
    private List<String> messageFragments = new ArrayList<>(ClassificationDefaults.MESSAGE_FRAGMENTS);

    /**
     * Additions to the lists above, keyed by contributor. A map because Spring merges maps key by key across property sources but replaces lists wholesale.
     */
    private Map<String, Contribution> contributions = Map.of();

    /** Lookup copy of {@link #classNames} plus contributions. */
    private Set<String> classNameSet = new LinkedHashSet<>(ClassificationDefaults.CLASS_NAMES);
    /** Lower-cased copy of {@link #messageFragments} plus contributions. */
    private List<String> lowerCaseMessageFragments = lowerCase(ClassificationDefaults.MESSAGE_FRAGMENTS);

    private List<String> messageFragmentClassNames = new ArrayList<>(
            ClassificationDefaults.MESSAGE_FRAGMENT_CLASS_NAMES);

    /** Lookup copy of {@link #messageFragmentClassNames} plus contributions. */
    private Set<String> messageFragmentClassNameSet = new LinkedHashSet<>(
            ClassificationDefaults.MESSAGE_FRAGMENT_CLASS_NAMES);

    private List<String> infrastructureClassNames = new ArrayList<>(ClassificationDefaults.INFRASTRUCTURE_CLASS_NAMES);
    private List<String> infrastructureMessageFragments = new ArrayList<>(
            ClassificationDefaults.INFRASTRUCTURE_MESSAGE_FRAGMENTS);

    private Set<String> infrastructureClassNameSet = new LinkedHashSet<>(
            ClassificationDefaults.INFRASTRUCTURE_CLASS_NAMES);
    private List<String> lowerCaseInfraMessageFragments = lowerCase(
            ClassificationDefaults.INFRASTRUCTURE_MESSAGE_FRAGMENTS);

    @PostConstruct
    void logEffectiveConfiguration()
    {
        if (!enabled)
        {
            logger.info("Transform failure classification is disabled: every unhandled exception, and every {} a "
                    + "transformer inferred from one it caught, will be reported as {}.",
                    UNPROCESSABLE_ENTITY, INTERNAL_SERVER_ERROR);
            return;
        }
        logger.info("Transform failure classification is enabled. Exceptions matching these are reported as {}, "
                + "everything else as {}. Class names: {}. Message fragments: {}, read only from {}. "
                + "Overridden in all cases by these infrastructure class names: {}, and message fragments: {}. "
                + "Contributions included: {}.",
                UNPROCESSABLE_ENTITY, INTERNAL_SERVER_ERROR, classNameSet, lowerCaseMessageFragments,
                messageFragmentClassNameSet, infrastructureClassNameSet, lowerCaseInfraMessageFragments,
                contributions.keySet());
    }

    /**
     * @return {@code 422} if {@code e} or a cause positively identifies a bad document, otherwise {@code 500}.
     */
    HttpStatus classify(Throwable e)
    {
        if (!enabled)
        {
            return INTERNAL_SERVER_ERROR;
        }

        List<Throwable> causeChain = causeChain(e);

        // Checked first, so a wrapper (e.g. Tika's "Illegal IOException from ...") cannot hide an infrastructure cause.
        String veto = infrastructureVeto(causeChain);
        if (veto != null)
        {
            logDecision(INTERNAL_SERVER_ERROR, e, veto);
            return INTERNAL_SERVER_ERROR;
        }

        for (Throwable t : causeChain)
        {
            String match = contentFailureMatch(t);
            if (match != null)
            {
                logDecision(UNPROCESSABLE_ENTITY, e, t.getClass().getName() + " matched " + match);
                return UNPROCESSABLE_ENTITY;
            }
        }
        logUnrecognised(e);
        return INTERNAL_SERVER_ERROR;
    }

    /**
     * Turns a transformer's own {@code 422} into {@code 500} when its cause chain shows an infrastructure failure (e.g. a full disk).
     */
    HttpStatus vetoIfInfrastructural(HttpStatus status, Throwable e)
    {
        if (status != UNPROCESSABLE_ENTITY)
        {
            return status;
        }
        if (!enabled)
        {
            return withoutInferredClassification(e);
        }
        String veto = infrastructureVeto(causeChain(e));
        if (veto == null)
        {
            return status;
        }
        logger.warn("A transformer reported {} for {}, but {}. Reporting {} so the T-Router still retries it.",
                UNPROCESSABLE_ENTITY, describeCauseChain(e), veto, INTERNAL_SERVER_ERROR);
        return INTERNAL_SERVER_ERROR;
    }

    /**
     * When disabled, turns a {@code 422} with a cause back into {@code 500}; a {@code 422} without a cause was the engine's own and is kept.
     */
    private HttpStatus withoutInferredClassification(Throwable e)
    {
        if (e.getCause() == null)
        {
            return UNPROCESSABLE_ENTITY;
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Classification is disabled, so reporting {} rather than the {} inferred from {}.",
                    INTERNAL_SERVER_ERROR, UNPROCESSABLE_ENTITY, e.getCause().getClass().getName());
        }
        return INTERNAL_SERVER_ERROR;
    }

    /**
     * @return a description of the first infrastructure match in {@code causeChain}, or {@code null} if there was none.
     */
    private String infrastructureVeto(List<Throwable> causeChain)
    {
        for (Throwable t : causeChain)
        {
            String match = infrastructureMatch(t);
            if (match != null)
            {
                return t.getClass().getName() + " matched " + match;
            }
        }
        return null;
    }

    /** @return {@code e} and its causes, guarding against cyclic chains. */
    private static List<Throwable> causeChain(Throwable e)
    {
        List<Throwable> causeChain = new ArrayList<>();
        Set<Throwable> seen = new HashSet<>();
        for (Throwable t = e; t != null && seen.add(t); t = t.getCause())
        {
            causeChain.add(t);
        }
        return causeChain;
    }

    /**
     * @return the configured entry {@code t} matched, or {@code null} if it did not match.
     */
    private String infrastructureMatch(Throwable t)
    {
        // Unlike content matching, messages of any type are checked.
        String classMatch = classNameMatch(t, infrastructureClassNameSet);
        return classMatch != null ? classMatch : messageMatch(t, lowerCaseInfraMessageFragments);
    }

    /**
     * @return the configured entry {@code t} matched, or {@code null} if it did not match.
     */
    private String contentFailureMatch(Throwable t)
    {
        String classMatch = classNameMatch(t, classNameSet);
        if (classMatch != null)
        {
            return classMatch;
        }
        // Only messages of messageFragmentClassNames types are read.
        if (classNameMatch(t, messageFragmentClassNameSet) == null)
        {
            return null;
        }
        return messageMatch(t, lowerCaseMessageFragments);
    }

    private static String classNameMatch(Throwable t, Set<String> classNames)
    {
        // Superclasses too: libraries throw subclasses of the listed type (POI's OldExcelFormatException).
        for (Class<?> type = t.getClass(); type != null && type != Object.class; type = type.getSuperclass())
        {
            if (classNames.contains(type.getSimpleName()))
            {
                return "class name '" + type.getSimpleName() + "'";
            }
            if (classNames.contains(type.getName()))
            {
                return "class name '" + type.getName() + "'";
            }
        }
        return null;
    }

    private static String messageMatch(Throwable t, List<String> lowerCaseFragments)
    {
        String message = t.getMessage();
        if (message == null)
        {
            return null;
        }
        String lowerCaseMessage = message.toLowerCase(Locale.ROOT);
        return lowerCaseFragments.stream()
                .filter(lowerCaseMessage::contains)
                .findFirst()
                .map(fragment -> "message fragment '" + fragment + "'")
                .orElse(null);
    }

    private void logDecision(HttpStatus status, Throwable e, String reason)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Classified {} as {} - {}", describeCauseChain(e), status, reason);
        }
    }

    /** Info, not debug: this is how unknown exceptions surface in production; volume follows the failure rate. */
    private void logUnrecognised(Throwable e)
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Nothing in the cause chain was recognised as a content failure, so reporting {} and leaving "
                    + "the T-Router to retry: {}. If this document can never succeed, add it to "
                    + "transform.failure.classification.class-names or .message-fragments.",
                    INTERNAL_SERVER_ERROR, describeCauseChain(e));
        }
    }

    private static String describeCauseChain(Throwable e)
    {
        StringBuilder sb = new StringBuilder();
        Set<Throwable> seen = new HashSet<>();
        for (Throwable t = e; t != null && seen.add(t); t = t.getCause())
        {
            sb.append(sb.isEmpty() ? "" : " caused by ")
                    .append(t.getClass().getName())
                    .append(": ")
                    .append(t.getMessage());
        }
        return sb.toString();
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void setClassNames(List<String> classNames)
    {
        this.classNames = sanitise("class-names", classNames);
        rebuildClassNames();
    }

    public void setMessageFragments(List<String> messageFragments)
    {
        this.messageFragments = sanitise("message-fragments", messageFragments);
        rebuildMessageFragments();
    }

    public void setMessageFragmentClassNames(List<String> messageFragmentClassNames)
    {
        this.messageFragmentClassNames = sanitise("message-fragment-class-names", messageFragmentClassNames);
        rebuildMessageFragmentClassNames();
    }

    public void setInfrastructureClassNames(List<String> infrastructureClassNames)
    {
        this.infrastructureClassNames = sanitise("infrastructure-class-names", infrastructureClassNames);
        rebuildInfrastructureClassNames();
    }

    public void setInfrastructureMessageFragments(List<String> infrastructureMessageFragments)
    {
        this.infrastructureMessageFragments = sanitise("infrastructure-message-fragments",
                infrastructureMessageFragments);
        rebuildInfrastructureMessageFragments();
    }

    /**
     * Adds to the lists rather than replacing them. Different keys are all kept; reusing a key overrides it.
     */
    public void setContributions(Map<String, Contribution> contributions)
    {
        Map<String, Contribution> sanitised = new TreeMap<>();
        if (contributions != null)
        {
            contributions.forEach((name, contribution) -> {
                if (contribution != null)
                {
                    sanitised.put(name, contribution.sanitised("contributions." + name + "."));
                }
            });
        }
        this.contributions = Collections.unmodifiableMap(sanitised);
        rebuildClassNames();
        rebuildMessageFragments();
        rebuildMessageFragmentClassNames();
        rebuildInfrastructureClassNames();
        rebuildInfrastructureMessageFragments();
    }

    /** Trims entries and drops blanks (e.g. a trailing comma in an env var): a blank fragment would match every message. */
    private static List<String> sanitise(String property, List<String> values)
    {
        if (values == null)
        {
            return List.of();
        }
        List<String> sanitised = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        if (sanitised.size() != values.size())
        {
            logger.warn("Ignored {} blank {} in transform.failure.classification.{}.",
                    values.size() - sanitised.size(), values.size() - sanitised.size() == 1 ? "entry" : "entries",
                    property);
        }
        return sanitised;
    }

    private void rebuildClassNames()
    {
        this.classNameSet = new LinkedHashSet<>(concat(classNames, contributed(Contribution::getClassNames)));
    }

    private void rebuildMessageFragments()
    {
        this.lowerCaseMessageFragments = lowerCase(
                concat(messageFragments, contributed(Contribution::getMessageFragments)));
    }

    private void rebuildMessageFragmentClassNames()
    {
        this.messageFragmentClassNameSet = new LinkedHashSet<>(
                concat(messageFragmentClassNames, contributed(Contribution::getMessageFragmentClassNames)));
    }

    private void rebuildInfrastructureClassNames()
    {
        this.infrastructureClassNameSet = new LinkedHashSet<>(
                concat(infrastructureClassNames, contributed(Contribution::getInfrastructureClassNames)));
    }

    private void rebuildInfrastructureMessageFragments()
    {
        this.lowerCaseInfraMessageFragments = lowerCase(concat(infrastructureMessageFragments,
                contributed(Contribution::getInfrastructureMessageFragments)));
    }

    private List<String> contributed(Function<Contribution, List<String>> list)
    {
        return contributions.values().stream()
                .flatMap(contribution -> list.apply(contribution).stream())
                .toList();
    }

    /** @return {@code base} plus the {@code additions} it does not already hold. */
    private static List<String> concat(List<String> base, List<String> additions)
    {
        if (additions.isEmpty())
        {
            return base;
        }
        List<String> combined = new ArrayList<>(base);
        additions.stream()
                .filter(addition -> !combined.contains(addition))
                .forEach(combined::add);
        return combined;
    }

    private static List<String> lowerCase(List<String> values)
    {
        return values.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public List<String> getClassNames()
    {
        return classNames;
    }

    public List<String> getMessageFragments()
    {
        return messageFragments;
    }

    public List<String> getMessageFragmentClassNames()
    {
        return messageFragmentClassNames;
    }

    public List<String> getInfrastructureClassNames()
    {
        return infrastructureClassNames;
    }

    public List<String> getInfrastructureMessageFragments()
    {
        return infrastructureMessageFragments;
    }

    public Map<String, Contribution> getContributions()
    {
        return contributions;
    }

    /** One contributor's additions; each list extends the classifier's list of the same name. */
    public static class Contribution
    {
        private List<String> classNames = List.of();
        private List<String> messageFragments = List.of();
        private List<String> messageFragmentClassNames = List.of();
        private List<String> infrastructureClassNames = List.of();
        private List<String> infrastructureMessageFragments = List.of();

        private Contribution sanitised(String prefix)
        {
            Contribution sanitised = new Contribution();
            sanitised.classNames = sanitise(prefix + "class-names", classNames);
            sanitised.messageFragments = sanitise(prefix + "message-fragments", messageFragments);
            sanitised.messageFragmentClassNames = sanitise(prefix + "message-fragment-class-names",
                    messageFragmentClassNames);
            sanitised.infrastructureClassNames = sanitise(prefix + "infrastructure-class-names",
                    infrastructureClassNames);
            sanitised.infrastructureMessageFragments = sanitise(prefix + "infrastructure-message-fragments",
                    infrastructureMessageFragments);
            return sanitised;
        }

        public List<String> getClassNames()
        {
            return classNames;
        }

        public void setClassNames(List<String> classNames)
        {
            this.classNames = classNames;
        }

        public List<String> getMessageFragments()
        {
            return messageFragments;
        }

        public void setMessageFragments(List<String> messageFragments)
        {
            this.messageFragments = messageFragments;
        }

        public List<String> getMessageFragmentClassNames()
        {
            return messageFragmentClassNames;
        }

        public void setMessageFragmentClassNames(List<String> messageFragmentClassNames)
        {
            this.messageFragmentClassNames = messageFragmentClassNames;
        }

        public List<String> getInfrastructureClassNames()
        {
            return infrastructureClassNames;
        }

        public void setInfrastructureClassNames(List<String> infrastructureClassNames)
        {
            this.infrastructureClassNames = infrastructureClassNames;
        }

        public List<String> getInfrastructureMessageFragments()
        {
            return infrastructureMessageFragments;
        }

        public void setInfrastructureMessageFragments(List<String> infrastructureMessageFragments)
        {
            this.infrastructureMessageFragments = infrastructureMessageFragments;
        }
    }
}
