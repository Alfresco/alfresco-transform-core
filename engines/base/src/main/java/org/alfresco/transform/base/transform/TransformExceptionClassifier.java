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
 * The lists were derived from reported failures and from those libraries, not from an exhaustive corpus, so they will need adjusting. A hardcoded list would have to travel the whole t-core &rarr; t-service &rarr; t-aspose &rarr; ai-renditions release chain to correct one entry, so all five lists and a kill switch ({@code transform.failure.classification.enabled}, {@code .class-names}, {@code .message-fragments}, {@code .message-fragment-class-names}, {@code .infrastructure-class-names}, {@code .infrastructure-message-fragments}) are configuration. Setting a list <i>replaces</i> the default, so a false positive can be removed as well as added; the effective lists are logged at startup.
 */
@Component
@ConfigurationProperties(prefix = "transform.failure.classification")
@SuppressWarnings("PMD.FieldNamingConventions") // `logger` is the convention throughout this codebase.
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
     * Only consulted for the types in {@link #DEFAULT_MESSAGE_FRAGMENT_CLASS_NAMES}. See there for why.
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
    private static final List<String> DEFAULT_MESSAGE_FRAGMENT_CLASS_NAMES = List.of(
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

    /** Rebuilt from {@link #classNames} after binding, as it is only ever used for lookups. */
    private Set<String> classNameSet = new LinkedHashSet<>(DEFAULT_CLASS_NAMES);
    /** Lower cased copy of {@link #messageFragments}, so the comparison does not re-lower per call. */
    private List<String> lowerCaseMessageFragments = new ArrayList<>(DEFAULT_MESSAGE_FRAGMENTS);

    private List<String> messageFragmentClassNames = new ArrayList<>(DEFAULT_MESSAGE_FRAGMENT_CLASS_NAMES);

    /** Rebuilt from {@link #messageFragmentClassNames} after binding, as it is only ever used for lookups. */
    private Set<String> messageFragmentClassNameSet = new LinkedHashSet<>(DEFAULT_MESSAGE_FRAGMENT_CLASS_NAMES);

    private List<String> infrastructureClassNames = new ArrayList<>(DEFAULT_INFRA_CLASS_NAMES);
    private List<String> infrastructureMessageFragments = new ArrayList<>(DEFAULT_INFRA_MESSAGE_FRAGMENTS);

    private Set<String> infrastructureClassNameSet = new LinkedHashSet<>(DEFAULT_INFRA_CLASS_NAMES);
    private List<String> lowerCaseInfraMessageFragments = new ArrayList<>(DEFAULT_INFRA_MESSAGE_FRAGMENTS);

    @PostConstruct
    void logEffectiveConfiguration()
    {
        if (!enabled)
        {
            logger.info("Transform failure classification is disabled: every unhandled exception will be reported as {}.",
                    INTERNAL_SERVER_ERROR);
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

        // Guard against the self referencing cause chains some libraries produce.
        List<Throwable> causeChain = new ArrayList<>();
        Set<Throwable> seen = new HashSet<>();
        for (Throwable t = e; t != null && seen.add(t); t = t.getCause())
        {
            causeChain.add(t);
        }

        // An infrastructure failure anywhere in the chain wins, however it was wrapped. Without this a wrapper such as
        // Tika's "TIKA-198: Illegal IOException from ..." would be matched before its real cause was ever examined.
        for (Throwable t : causeChain)
        {
            String veto = infrastructureMatch(t);
            if (veto != null)
            {
                logDecision(INTERNAL_SERVER_ERROR, e, t.getClass().getName() + " matched " + veto);
                return INTERNAL_SERVER_ERROR;
            }
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
        // Only the parser types get their message read. See DEFAULT_MESSAGE_FRAGMENT_CLASS_NAMES.
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
        this.classNameSet = new LinkedHashSet<>(classNames);
    }

    public void setMessageFragments(List<String> messageFragments)
    {
        this.messageFragments = messageFragments;
        this.lowerCaseMessageFragments = messageFragments.stream()
                .map(fragment -> fragment.toLowerCase(Locale.ROOT))
                .toList();
    }

    public void setMessageFragmentClassNames(List<String> messageFragmentClassNames)
    {
        this.messageFragmentClassNames = messageFragmentClassNames;
        this.messageFragmentClassNameSet = new LinkedHashSet<>(messageFragmentClassNames);
    }

    public void setInfrastructureClassNames(List<String> infrastructureClassNames)
    {
        this.infrastructureClassNames = infrastructureClassNames;
        this.infrastructureClassNameSet = new LinkedHashSet<>(infrastructureClassNames);
    }

    public void setInfrastructureMessageFragments(List<String> infrastructureMessageFragments)
    {
        this.infrastructureMessageFragments = infrastructureMessageFragments;
        this.lowerCaseInfraMessageFragments = infrastructureMessageFragments.stream()
                .map(fragment -> fragment.toLowerCase(Locale.ROOT))
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
}
