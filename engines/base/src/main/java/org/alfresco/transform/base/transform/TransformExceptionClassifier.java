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
 * Decides whether an exception thrown by a {@link org.alfresco.transform.base.CustomTransformer} means "this document cannot be transformed" or "this engine is unwell".
 * <p/>
 * The T-Router only retries 5xx and 408, so reporting a bad document as {@code 500} has it retried with a growing backoff for something that can never succeed. {@code 422} already carries "bad document" elsewhere in the engine - see {@code ProcessHandler.validateOutputLength}.
 * <p/>
 * <b>Deliberately conservative: anything not positively recognised stays {@code 500}.</b> A content failure reported as an outage only costs retries; an outage reported as a content failure drops a transform a retry would have fixed. Because libraries wrap - Tika's {@code CompositeParser} rethrows everything as {@code TikaException} - an infrastructure match anywhere in the cause chain vetoes the classification, and is checked first.
 * <p/>
 * Matching is by class <i>name</i>, since {@code engines/base} must not depend on Tika, PDFBox or POI.
 * <p/>
 * The lists came from reported failures rather than an exhaustive corpus, so they will need adjusting. None of them is written down here: what the engine ships with is data, held in {@link ClassificationDefaults#RESOURCE}, and what a deployment runs is settled by the properties under {@code transform.failure.classification} - setting a list <i>replaces</i> the shipped one, and a named entry under {@code contributions} extends it. So a correction needs neither a release nor a rebuild. The effective lists are logged at startup.
 */
@Component
@ConfigurationProperties(prefix = "transform.failure.classification")
// `logger` is the convention throughout this codebase. The method and complexity counts are those of a
// configuration-properties bean: five configurable lists and their contributions, around a decision that is four
// short private methods.
@SuppressWarnings({"PMD.FieldNamingConventions", "PMD.TooManyMethods", "PMD.GodClass"})
class TransformExceptionClassifier
{
    private static final Logger logger = LoggerFactory.getLogger(TransformExceptionClassifier.class);

    private boolean enabled = true;
    private List<String> classNames = new ArrayList<>(ClassificationDefaults.CLASS_NAMES);
    private List<String> messageFragments = new ArrayList<>(ClassificationDefaults.MESSAGE_FRAGMENTS);

    /**
     * Entries added on top of the lists above rather than in place of them, keyed by whoever added them: an engine uses its own name, a deployment picks another.
     * <p/>
     * A map rather than a second set of lists because Spring merges a map key by key across property sources but replaces a list wholesale. With lists, a deployment adding one name at runtime would silently discard everything an engine had packaged. Held apart from the lists they extend so the effective lists do not depend on the order Spring called the setters in. Sorted by name, so the effective lists are the same on every start.
     */
    private Map<String, Contribution> contributions = Map.of();

    /** Rebuilt from {@link #classNames} after binding, as it is only ever used for lookups. */
    private Set<String> classNameSet = new LinkedHashSet<>(ClassificationDefaults.CLASS_NAMES);
    /** Lower cased copy of {@link #messageFragments}, so the comparison does not re-lower per call. */
    private List<String> lowerCaseMessageFragments = lowerCase(ClassificationDefaults.MESSAGE_FRAGMENTS);

    private List<String> messageFragmentClassNames = new ArrayList<>(
            ClassificationDefaults.MESSAGE_FRAGMENT_CLASS_NAMES);

    /** Rebuilt from {@link #messageFragmentClassNames} after binding, as it is only ever used for lookups. */
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
     * @param e
     *            the exception thrown while transforming.
     * @return {@code 422} if {@code e} or any of its causes positively identifies a problem with the supplied document, otherwise {@code 500}.
     */
    HttpStatus classify(Throwable e)
    {
        if (!enabled)
        {
            return INTERNAL_SERVER_ERROR;
        }

        List<Throwable> causeChain = causeChain(e);

        // An infrastructure failure anywhere in the chain wins, however it was wrapped. Without this a wrapper such as
        // Tika's "TIKA-198: Illegal IOException from ..." would be matched before its real cause was ever examined.
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
     * Refuses a terminal status when the cause chain shows the engine or its environment failed, rather than the document.
     * <p/>
     * {@link #classify(Throwable)} applies this to what it classifies itself. This entry point exists for a status that arrived already decided - a {@code TransformException} a transformer threw - because that decision was made by a {@code catch} block that saw only the type it caught. A transformer that answers {@code MessagingException} with {@code 422} is right about a malformed {@code .eml} and wrong about the same exception raised because the disk filled. Only the cause chain tells those apart, and a wrong {@code 422} is never retried.
     * <p/>
     * Other statuses are returned untouched: a {@code 400} is already the engine's considered answer, and a {@code 5xx} is retryable regardless.
     *
     * @param status
     *            the status the transformer asked for.
     * @param e
     *            the exception it threw.
     * @return {@code status}, or {@code 500} if the chain shows an infrastructure failure.
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
     * Withdraws a {@code 422} a transformer <i>inferred</i> from an exception it caught, leaving one it determined by looking at the document.
     * <p/>
     * Turning classification off has to be a genuine way back to the previous behaviour. A transformer deciding its own status sidesteps {@link #classify(Throwable)}, so without this the switch would silence the central lists while those decisions carried on - and would take the infrastructure veto down with them, leaving the terminal statuses in place and removing the one thing that corrects them.
     * <p/>
     * The cause tells the two kinds apart:
     * <ul>
     * <li><b>Inferred, so withdrawn.</b> A library threw and a {@code catch} block read that as a statement about the document.</li>
     * <li><b>Determined, so kept.</b> The engine read the document and found it wanting - no image reader accepts the format, the zip holds no preview, the transform produced nothing from a non-empty source. No exception was interpreted, so there is nothing to withdraw.</li>
     * </ul>
     *
     * @param e
     *            the exception the transformer threw.
     * @return {@code 500} if the {@code 422} was inferred from a cause, otherwise {@code 422}.
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

    /**
     * @return {@code e} and its causes, stopping at the self referencing chains some libraries produce.
     */
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
        // Deliberately unrestricted: an infrastructure failure must be recognised however it was wrapped.
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
        // Only the parser types get their message read. See messageFragmentClassNames.
        if (classNameMatch(t, messageFragmentClassNameSet) == null)
        {
            return null;
        }
        return messageMatch(t, lowerCaseMessageFragments);
    }

    private static String classNameMatch(Throwable t, Set<String> classNames)
    {
        // Walk the superclasses, not just the runtime type. The parser libraries declare a general content failure
        // and then throw a more specific subclass of it: POI throws `OldExcelFormatException` where the listed type
        // is its parent `OldFileFormatException`. Matching the runtime name alone missed every one of those. Only
        // names are compared, never types, so this keeps working for an engine that shades or relocates a library.
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

    /**
     * Logged at debug, because the lists already hold the answer and the decision can be replayed from them.
     */
    private void logDecision(HttpStatus status, Throwable e, String reason)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Classified {} as {} - {}", describeCauseChain(e), status, reason);
        }
    }

    /**
     * Logged at info rather than debug, because this is the only case that names an exception the lists do not yet know about, and debug is not enabled in production. Only unhandled transform failures reach here, so the volume follows the failure rate rather than the traffic rate.
     */
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
     * Adds to the lists instead of replacing them.
     * <p/>
     * Setting a list replaces it, which is right for a deployment overruling a decision but wrong for the commoner case: an engine or a deployment that knows one more exception its libraries raise. Restating the whole list to add a single name means every later correction to the defaults has to be copied into everything that did so.
     * <p/>
     * Each contributor has its own key, so contributions from different property sources - an engine's packaged file and a deployment's mounted one, say - are all kept. A deployment can still overrule an engine's contribution by setting the same key, and remove it by setting that key's lists empty. Setting a contribution and the list it extends together is allowed, and the result does not depend on binding order.
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

    /**
     * Trims every entry and drops blank ones, which a deployment can supply without meaning to - a trailing comma in an environment variable is enough. Dropping them is not cosmetic: every message contains the empty string, so a blank fragment would make every {@code IOException}, a full disk included, a {@code 422} that is never retried; and an anonymous exception class has an empty simple name, so a blank class name would match it.
     */
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

    /**
     * @return {@code base} followed by the entries of {@code additions} it does not already hold, so repeating something the defaults already cover changes nothing.
     */
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

    /**
     * One contributor's additions. Each list extends the classifier's list of the same name.
     */
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
