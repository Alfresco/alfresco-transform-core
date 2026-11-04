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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Decides whether an exception thrown by a {@link org.alfresco.transform.base.CustomTransformer} means "this document cannot be transformed" or "this engine is unwell".
 * <p/>
 * The distinction matters because the T-Router only retries 5xx and 408. Reporting a bad document as {@code 500} makes the router retry it with a growing backoff for a document that can never succeed; with enough bad documents that occupies the JMS listener pool and delays healthy transforms.
 * <p/>
 * {@code 422 UNPROCESSABLE_ENTITY} already carries this meaning elsewhere in the engine - see {@code ProcessHandler.validateOutputLength} - so it is reused rather than inventing a status.
 * <p/>
 * <b>Deliberately conservative: anything not positively recognised stays {@code 500}.</b> A content failure wrongly reported as an outage only costs some retries, but an outage wrongly reported as a content failure would drop a transform that retrying would have fixed.
 * <p/>
 * That rule needs enforcing, not just stating, because libraries wrap: Tika's {@code CompositeParser} rethrows every exception a parser raises as a {@code TikaException}. So an infrastructure match anywhere in the cause chain vetoes the whole classification, and is checked before any content rule.
 * <p/>
 * Matching is by class <i>name</i>, not by type, because {@code engines/base} does not - and should not - depend on Tika, PDFBox, POI or the other parsing libraries the engines use.
 * <p/>
 * The lists were derived from reported failures and from those libraries, not from an exhaustive corpus, so they will need adjusting. A hardcoded list would have to travel the whole t-core &rarr; t-service &rarr; t-aspose &rarr; ai-renditions release chain to correct one entry, so all five lists and a kill switch ({@code transform.failure.classification.enabled}, {@code .class-names}, {@code .message-fragments}, {@code .message-fragment-class-names}, {@code .infrastructure-class-names}, {@code .infrastructure-message-fragments}) are configuration. Setting a list <i>replaces</i> the default, so a false positive can be removed as well as added; a matching {@code additional-*} property adds to it instead, which is what an engine or an extension that knows one more of its own library's exceptions needs. The effective lists are logged at startup.
 */
@Component
@ConfigurationProperties(prefix = "transform.failure.classification")
// `logger` is the convention throughout this codebase. The method and complexity counts are those of a
// configuration-properties bean: five configurable lists, each with a replacing setter, an extending setter and a
// getter, around a decision that is four short private methods. Splitting it would separate the rules from the
// explanation of why each entry is in them, which is the part that needs to stay readable.
@SuppressWarnings({"PMD.FieldNamingConventions", "PMD.TooManyMethods", "PMD.GodClass"})
class TransformExceptionClassifier
{
    private static final Logger logger = LoggerFactory.getLogger(TransformExceptionClassifier.class);

    /**
     * Exception class names that only ever mean the supplied document could not be parsed.
     * <p/>
     * Simple names, fully qualified names and superclass names are all matched, so an engine that shades or relocates a library still works, and a library that throws a more specific subclass of a listed type still matches.
     */
    private static final List<String> DEFAULT_CLASS_NAMES = List.of(
            // java.util.zip - a corrupt OOXML/ODF file is a corrupt zip
            "ZipException",
            // java.io / java.nio.charset - truncated or undecodable content
            //
            // `UnsupportedEncodingException` is deliberately NOT listed. Despite the name it says nothing about the
            // document: it means the requested *charset name* is not installed in the JVM, and that name reaches the
            // engine through the caller's `targetEncoding` transform option, not through the file. Listing it would
            // turn one typo in a client's options into the silent, permanent loss of every request that client makes,
            // where today it is a loud 500. Content that genuinely cannot be decoded arrives as one of the three
            // entries below instead.
            "EOFException",
            "CharConversionException",
            "MalformedInputException",
            "UnmappableCharacterException",
            // Apache Tika
            //
            // The generic `TikaException` is deliberately NOT listed. It is Tika's universal wrapper, not a content
            // failure type: `CompositeParser` rethrows every exception a parser raises as one, including
            // "TIKA-198: Illegal IOException from ...". Matching it would turn a disk-full or socket error into a
            // terminal 422. Because it is absent, its content subclasses have to be named individually; they cannot
            // inherit a match from it.
            "EncryptedDocumentException",
            "UnsupportedFormatException",
            "ZeroByteFileException",
            "CorruptedFileException",
            // the document's own permissions forbid extraction, so no number of retries will ever read it
            "AccessPermissionException",
            // Apache PDFBox
            "InvalidPasswordException",
            // Apache POI
            //
            // The specific types POI throws are usually subclasses of these - `OldExcelFormatException` extends
            // `OldFileFormatException`, `OLE2NotOfficeXmlFileException` extends `NotOfficeXmlFileException` - and are
            // matched through the superclass walk in `match`, so they do not need listing individually.
            "EmptyFileException",
            "OldFileFormatException",
            "UnsupportedFileFormatException",
            "NotOfficeXmlFileException",
            "OfficeXmlFileException",
            "NotOLE2FileException",
            "RecordFormatException",
            "InvalidFormatException",
            // these three have no listed ancestor: they extend IllegalStateException, RuntimeException and
            // IOException respectively
            "CorruptPowerPointFileException",
            "XSSFBParseException",
            "InvalidZipException",
            // XML content that is not well formed
            "SAXParseException",
            // Apache Commons Imaging
            //
            // `ImageReadException` was renamed `ImagingException` in 1.0.0-alpha6 and so no longer exists in the
            // shipped version. It is kept for engines still pinning an earlier release, where it is the only signal.
            // `ImagingFormatException` needs naming because it extends `ImagingRuntimeException`, not `ImagingException`.
            "ImageReadException",
            "ImagingException",
            "ImagingFormatException");

    /**
     * Lower case fragments that identify a content failure reported only as a plain {@code IOException}.
     * <p/>
     * PDFBox in particular signals most structural damage this way, which is exactly the case reported in MNT-25570 ("Missing root object specification in trailer"). Matching on the message is unpleasant, but the alternative - treating every {@code IOException} as a content failure - would hide real disk and network faults.
     * <p/>
     * Only consulted for the types in {@link #DEFAULT_MESSAGE_FRAGMENT_TYPES}. See there for why.
     */
    private static final List<String> DEFAULT_MESSAGE_FRAGMENTS = List.of(
            "missing root object specification",
            "expected='startxref'",
            "expected 'startxref'",
            "error: end-of-file",
            "unexpected end of file",
            "premature end of file",
            "not a pdf",
            "invalid pdf",
            "corrupt",
            "damaged",
            "malformed",
            "truncated",
            "is encrypted",
            "password-protected",
            "unsupported file format",
            "unknown file format",
            "cannot be parsed",
            "could not be parsed");

    /**
     * Types whose message is worth reading for a content failure.
     * <p/>
     * The fragments above are parser vocabulary, and several of them - {@code corrupt}, {@code damaged}, {@code malformed}, {@code truncated} - are words an engine might just as easily use about itself. Scanning the message of <i>every</i> throwable therefore turns an engine bug such as {@code IllegalStateException("corrupt temporary output")} into a {@code 422}, permanently dropping a transform that a retry would have fixed. Confining the scan to the types that actually report content damage in their text closes that off.
     * <p/>
     * The three listed are where the parsers put it. PDFBox and the JDK report structural damage as a plain {@code IOException}; XML content arrives as a {@code SAXException}; {@code TikaException} is Tika's wrapper, which is safe to read here only because the infrastructure veto has already examined the whole cause chain. A type that announces a content failure by its own name does not need to be listed - it belongs in {@link #DEFAULT_CLASS_NAMES} instead.
     * <p/>
     * This restriction is deliberately <b>not</b> applied to {@link #DEFAULT_INFRA_MESSAGE_FRAGMENTS}: the veto must stay broad so it can still recognise an infrastructure failure however it was wrapped.
     */
    private static final List<String> DEFAULT_MESSAGE_FRAGMENT_TYPES = List.of(
            "IOException",
            "SAXException",
            "TikaException");

    /**
     * Class names that always mean the engine or its environment failed, never the document.
     * <p/>
     * Checked across the whole cause chain <i>before</i> any content match, because libraries wrap: without this, an outer wrapper that looks like a content failure would mask an infrastructural cause and permanently drop a transform a retry would have fixed.
     */
    private static final List<String> DEFAULT_INFRA_CLASS_NAMES = List.of(
            // java.net - the engine could not reach something it needed
            "SocketException",
            "SocketTimeoutException",
            "ConnectException",
            "UnknownHostException",
            "NoRouteToHostException",
            // interruption and timeout - shutdown or an overloaded engine, not the document
            "InterruptedIOException",
            "ClosedByInterruptException",
            "InterruptedException",
            "TimeoutException",
            // java.nio.file - the environment, not the content
            "FileSystemException",
            "AccessDeniedException",
            "NoSuchFileException",
            // the engine itself is in trouble
            "OutOfMemoryError",
            "StackOverflowError");

    /**
     * Lower case message fragments that identify an infrastructure failure reported as a plain {@code IOException}, which is how the JDK reports most of them.
     */
    private static final List<String> DEFAULT_INFRA_MESSAGE_FRAGMENTS = List.of(
            "no space left on device",
            "disk quota exceeded",
            "too many open files",
            "connection reset",
            "connection refused",
            "broken pipe",
            "read-only file system",
            "cannot allocate memory",
            "cannot run program",
            "error=2, no such file or directory",
            "permission denied");

    private boolean enabled = true;
    private List<String> classNames = new ArrayList<>(DEFAULT_CLASS_NAMES);
    private List<String> messageFragments = new ArrayList<>(DEFAULT_MESSAGE_FRAGMENTS);

    /**
     * Entries an engine or extension contributes on top of the shipped list, rather than in place of it.
     * <p/>
     * Held apart from the list they extend so the effective list does not depend on the order Spring happened to call the two setters in. The three shortened names carry the same meaning as the properties that set them - {@code additional-message-fragment-class-names}, {@code additional-infrastructure-class-names} and {@code additional-infrastructure-message-fragments}.
     */
    private List<String> additionalClassNames = List.of();
    private List<String> additionalMessageFragments = List.of();
    private List<String> addedMessageFragmentTypes = List.of();
    private List<String> addedInfraClassNames = List.of();
    private List<String> addedInfraMessageFragments = List.of();

    /** Rebuilt from {@link #classNames} after binding, as it is only ever used for lookups. */
    private Set<String> classNameSet = new LinkedHashSet<>(DEFAULT_CLASS_NAMES);
    /** Lower cased copy of {@link #messageFragments}, so the comparison does not re-lower per call. */
    private List<String> lowerCaseMessageFragments = new ArrayList<>(DEFAULT_MESSAGE_FRAGMENTS);

    private List<String> messageFragmentClassNames = new ArrayList<>(DEFAULT_MESSAGE_FRAGMENT_TYPES);

    /** Rebuilt from {@link #messageFragmentClassNames} after binding, as it is only ever used for lookups. */
    private Set<String> messageFragmentClassNameSet = new LinkedHashSet<>(DEFAULT_MESSAGE_FRAGMENT_TYPES);

    private List<String> infrastructureClassNames = new ArrayList<>(DEFAULT_INFRA_CLASS_NAMES);
    private List<String> infrastructureMessageFragments = new ArrayList<>(DEFAULT_INFRA_MESSAGE_FRAGMENTS);

    private Set<String> infrastructureClassNameSet = new LinkedHashSet<>(DEFAULT_INFRA_CLASS_NAMES);
    private List<String> lowerCaseInfraMessageFragments = new ArrayList<>(DEFAULT_INFRA_MESSAGE_FRAGMENTS);

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
                + "Overridden in all cases by these infrastructure class names: {}, and message fragments: {}.",
                UNPROCESSABLE_ENTITY, INTERNAL_SERVER_ERROR, classNameSet, lowerCaseMessageFragments,
                messageFragmentClassNameSet, infrastructureClassNameSet, lowerCaseInfraMessageFragments);
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
     * {@link #classify(Throwable)} applies this to the exceptions it classifies itself. This entry point exists for a status that arrived already decided - a {@code TransformException} a transformer threw with a status of its own - because that decision was made by a {@code catch} block that saw only the type it caught.
     * <p/>
     * A transformer that answers {@code MessagingException} with {@code 422} is right about a malformed {@code .eml} and wrong about the same exception raised because the disk filled while the attachment was being written. Only the cause chain can tell those apart, and a wrong {@code 422} is never retried.
     * <p/>
     * Statuses that are not terminal content failures are returned untouched: a {@code 400} is already the engine's considered answer about the request, and a {@code 5xx} is retryable regardless.
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
     * Turning classification off has to be a genuine way back to the previous behaviour, or it is no use as a safety valve. A transformer deciding its own status sidesteps {@link #classify(Throwable)} entirely, so without this the switch would silence the central lists while those decisions carried on - and, worse, would take the infrastructure veto down with them, leaving the terminal statuses in place and removing the one thing that corrects them.
     * <p/>
     * The cause tells the two kinds of {@code 422} apart, because the distinction is what produced it rather than where:
     * <ul>
     * <li><b>Inferred, so withdrawn.</b> A library threw, and a {@code catch} block read that as a statement about the document. That is the reasoning this work introduced and the reasoning an administrator would be turning off.</li>
     * <li><b>Determined, so kept.</b> The engine read the document and found it wanting - no image reader accepts the format, the zip holds no preview, the transform produced nothing from a non-empty source. No exception was interpreted, so there is nothing to withdraw, and the last of those has answered {@code 422} since long before any of this.</li>
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
        // Only the parser types get their message read. See DEFAULT_MESSAGE_FRAGMENT_TYPES.
        if (classNameMatch(t, messageFragmentClassNameSet) == null)
        {
            return null;
        }
        return messageMatch(t, lowerCaseMessageFragments);
    }

    private static String classNameMatch(Throwable t, Set<String> classNames)
    {
        // Walk the superclasses, not just the runtime type. The parser libraries habitually declare a general
        // content failure and then throw a more specific subclass of it: POI throws `OldExcelFormatException`
        // where the listed type is its parent `OldFileFormatException`, and `OLE2NotOfficeXmlFileException`
        // where the listed type is `NotOfficeXmlFileException`. Matching the runtime name alone missed every
        // one of those, so a document POI had positively identified as unreadable still came back as a 500 and
        // was retried. Only the names are compared, never the types, so this keeps working for an engine that
        // shades or relocates a library.
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
     * Logged at info rather than debug, because this is the only case that names an exception the lists do not yet know about. The defaults are inferred, so correcting them depends on seeing real failures, and debug is not enabled in production. Only unhandled transform failures reach here, so the volume follows the failure rate rather than the traffic rate.
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
        this.classNames = classNames;
        rebuildClassNames();
    }

    /**
     * Adds to the shipped list instead of replacing it.
     * <p/>
     * Spring replaces a list property wholesale when it is set, which is right for a deployment overruling a decision but wrong for the commoner case: an engine, or an extension such as Aspose, that knows one more exception its own libraries raise. Restating the whole list to append a single name means every later correction to the defaults has to be copied into everything that did so, and anything that forgets silently loses the rest.
     * <p/>
     * Setting both this and the list it extends is allowed, and the result does not depend on the order the two were bound in.
     */
    public void setAdditionalClassNames(List<String> additionalClassNames)
    {
        this.additionalClassNames = additionalClassNames;
        rebuildClassNames();
    }

    public void setMessageFragments(List<String> messageFragments)
    {
        this.messageFragments = messageFragments;
        rebuildMessageFragments();
    }

    /** @see #setAdditionalClassNames(List) */
    public void setAdditionalMessageFragments(List<String> additionalMessageFragments)
    {
        this.additionalMessageFragments = additionalMessageFragments;
        rebuildMessageFragments();
    }

    public void setMessageFragmentClassNames(List<String> messageFragmentClassNames)
    {
        this.messageFragmentClassNames = messageFragmentClassNames;
        rebuildMessageFragmentClassNames();
    }

    /** @see #setAdditionalClassNames(List) */
    public void setAdditionalMessageFragmentClassNames(List<String> addedMessageFragmentTypes)
    {
        this.addedMessageFragmentTypes = addedMessageFragmentTypes;
        rebuildMessageFragmentClassNames();
    }

    public void setInfrastructureClassNames(List<String> infrastructureClassNames)
    {
        this.infrastructureClassNames = infrastructureClassNames;
        rebuildInfrastructureClassNames();
    }

    /** @see #setAdditionalClassNames(List) */
    public void setAdditionalInfrastructureClassNames(List<String> addedInfraClassNames)
    {
        this.addedInfraClassNames = addedInfraClassNames;
        rebuildInfrastructureClassNames();
    }

    public void setInfrastructureMessageFragments(List<String> infrastructureMessageFragments)
    {
        this.infrastructureMessageFragments = infrastructureMessageFragments;
        rebuildInfrastructureMessageFragments();
    }

    /** @see #setAdditionalClassNames(List) */
    public void setAdditionalInfrastructureMessageFragments(List<String> addedInfraMessageFragments)
    {
        this.addedInfraMessageFragments = addedInfraMessageFragments;
        rebuildInfrastructureMessageFragments();
    }

    private void rebuildClassNames()
    {
        this.classNameSet = new LinkedHashSet<>(concat(classNames, additionalClassNames));
    }

    private void rebuildMessageFragments()
    {
        this.lowerCaseMessageFragments = lowerCase(concat(messageFragments, additionalMessageFragments));
    }

    private void rebuildMessageFragmentClassNames()
    {
        this.messageFragmentClassNameSet = new LinkedHashSet<>(
                concat(messageFragmentClassNames, addedMessageFragmentTypes));
    }

    private void rebuildInfrastructureClassNames()
    {
        this.infrastructureClassNameSet = new LinkedHashSet<>(
                concat(infrastructureClassNames, addedInfraClassNames));
    }

    private void rebuildInfrastructureMessageFragments()
    {
        this.lowerCaseInfraMessageFragments = lowerCase(
                concat(infrastructureMessageFragments, addedInfraMessageFragments));
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

    public List<String> getAdditionalClassNames()
    {
        return additionalClassNames;
    }

    public List<String> getAdditionalMessageFragments()
    {
        return additionalMessageFragments;
    }

    public List<String> getAdditionalMessageFragmentClassNames()
    {
        return addedMessageFragmentTypes;
    }

    public List<String> getAdditionalInfrastructureClassNames()
    {
        return addedInfraClassNames;
    }

    public List<String> getAdditionalInfrastructureMessageFragments()
    {
        return addedInfraMessageFragments;
    }
}
