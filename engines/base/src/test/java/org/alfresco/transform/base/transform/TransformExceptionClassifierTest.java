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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INSUFFICIENT_STORAGE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.MalformedInputException;
import java.nio.file.FileSystemLoopException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import org.alfresco.transform.base.transform.TransformExceptionClassifier.Contribution;
import org.alfresco.transform.exceptions.TransformException;

/**
 * Bad documents are {@code 422} (not retried); anything that might be an engine or infrastructure problem stays {@code 500}.
 */
// One test per rule keeps misclassifications traceable.
@SuppressWarnings("PMD.TooManyMethods")
class TransformExceptionClassifierTest
{
    private final TransformExceptionClassifier classifier = new TransformExceptionClassifier();

    private Logger capturedLogger;
    private AppenderBase<ILoggingEvent> appender;
    private Level originalLevel;

    @Test
    void testCorruptPdfReportedByPdfBoxIsAContentFailure()
    {
        // A .docx renamed to .pdf.
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(
                new IOException("Missing root object specification in trailer.")));
    }

    @Test
    void testKnownContentFailureTypesAreClassifiedAs422()
    {
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("invalid stored block lengths")));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new EOFException()));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new MalformedInputException(3)));
    }

    @Test
    void testContentFailureIsFoundThroughTheCauseChain()
    {
        // Engines usually wrap the parser failure before it reaches ProcessHandler.
        Exception wrapped = new RuntimeException("Transform failed",
                new UncheckedIOException(new ZipException("invalid entry size")));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(wrapped));
    }

    @Test
    void testInfrastructureCauseVetoesAMatchingWrapper()
    {
        // Tika's CompositeParser wraps every parser IOException in a TikaException.
        Exception diskFull = new IllegalStateException("TIKA-198: Illegal IOException from parser",
                new TikaException("TIKA-198: Illegal IOException from parser",
                        new IOException("No space left on device")));

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(diskFull));
    }

    @Test
    void testTikaExceptionAloneIsNotTreatedAsAContentFailure()
    {
        // It is Tika's universal wrapper.
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new TikaException("Unexpected RuntimeException from parser")));
    }

    /** POI throws subclasses of the listed types, e.g. {@code OldExcelFormatException extends OldFileFormatException}. */
    @Test
    void testASubclassOfAListedTypeIsAContentFailure()
    {
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new OldExcelFormatException()));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(
                new RuntimeException("Transform failed", new OldExcelFormatException())));
    }

    @Test
    void testASubclassOfAnUnlistedTypeIsStill500()
    {
        // Nothing in this hierarchy is listed.
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new UnrecognisedParserException()));
    }

    @Test
    void testTheInfrastructureVetoAlsoMatchesSubclasses()
    {
        // FileSystemException is listed; its subclasses must veto too.
        Exception wrapped = new ZipException("corrupt entry");
        wrapped.initCause(new FileSystemLoopException("/tmp/work"));

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(wrapped));
    }

    /** Mirrors POI's {@code OldExcelFormatException extends OldFileFormatException}. */
    private static class OldFileFormatException extends IllegalArgumentException
    {
        private static final long serialVersionUID = 1L;
    }

    private static class OldExcelFormatException extends OldFileFormatException
    {
        private static final long serialVersionUID = 1L;
    }

    /** Nothing in this hierarchy is listed. */
    private static class UnrecognisedParserException extends IllegalStateException
    {
        private static final long serialVersionUID = 1L;
    }

    @Test
    void testInfrastructureVetoBeatsAContentFailureEarlierInTheChain()
    {
        // Content rule matches the outer exception, the veto a deeper one.
        Exception wrapped = new ZipException("corrupt entry");
        wrapped.initCause(new SocketException("Connection reset"));

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(wrapped));
    }

    @Test
    void testInterruptionIsNeverAContentFailure()
    {
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IllegalStateException("truncated", new InterruptedException())));
    }

    @Test
    void testABadTargetEncodingIsNotAContentFailure()
    {
        // Tika's shape for `--targetEncoding=rubbish`: a caller mistake that fails every document, so not a 422.
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new IllegalArgumentException(
                "Unsupported encoding rubbish", new UnsupportedEncodingException("rubbish"))));
    }

    @Test
    void testInfrastructureFailuresRemain500SoTheyAreStillRetried()
    {
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new SocketTimeoutException("Read timed out")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new IOException("No space left on device")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new OutOfMemoryError("Java heap space")));
    }

    @Test
    void testAnUnrecognisedFailureDefaultsTo500()
    {
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new RuntimeException("something went wrong")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new RuntimeException()));
    }

    @Test
    void testACyclicCauseChainDoesNotLoopForever()
    {
        Exception first = new RuntimeException("engine unavailable");
        Exception second = new RuntimeException("retry failed", first);
        first.initCause(second);

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(first));
    }

    @Test
    void testTheKillSwitchRestoresThePreviousBehaviour()
    {
        classifier.setEnabled(false);

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IOException("Missing root object specification in trailer.")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new ZipException("invalid stored block lengths")));
    }

    @Test
    void testTheClassNameListCanBeOverridden()
    {
        // Replaces rather than appends, so a false positive can be removed.
        classifier.setClassNames(List.of("IllegalArgumentException"));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IllegalArgumentException("bad glyph table")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new ZipException("invalid stored block lengths")));
    }

    @Test
    void testTheMessageFragmentListCanBeOverriddenAndIsCaseInsensitive()
    {
        classifier.setMessageFragments(List.of("Unreadable Glyph Table"));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IOException("unreadable glyph table at offset 12")));
        // That fragment is no longer configured.
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IOException("Missing root object specification in trailer.")));
    }

    @Test
    void testAFullyQualifiedClassNameAlsoMatches()
    {
        // For engines that shade or relocate a library.
        classifier.setClassNames(List.of("java.util.zip.ZipException"));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("invalid entry size")));
    }

    @Test
    void testTheInfrastructureVetoCanBeOverridden()
    {
        Exception wrapped = new ZipException("corrupt entry");
        wrapped.initCause(new SocketTimeoutException("Read timed out"));

        // SocketTimeoutException is vetoed by default.
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(wrapped));

        // Without the veto, the content rule decides.
        classifier.setInfrastructureClassNames(List.of("SocketException"));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(wrapped));
    }

    /** Words like "corrupt" may appear in an engine's own exceptions, so only parser types are read. */
    @Test
    void testAContentFragmentIsOnlyReadFromAParserFailureType()
    {
        assertEquals(INTERNAL_SERVER_ERROR,
                classifier.classify(new IllegalStateException("corrupt temporary output")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new RuntimeException("the engine is damaged")));
        assertEquals(INTERNAL_SERVER_ERROR,
                classifier.classify(new IllegalArgumentException("truncated command line")));

        // The same words from a parser failure type still decide.
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IOException("corrupt object stream")));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new TikaException("the document is damaged")));
    }

    @Test
    void testTheInfrastructureVetoStillReadsTheMessageOfAnyType()
    {
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IllegalStateException("corrupt entry", new IllegalStateException("No space left on device"))));
    }

    /** A malformed document and a full disk can reach the same {@code catch} block; only the cause tells them apart. */
    @Test
    void testAThrowSiteContentFailureIsVetoedWhenItsCauseIsInfrastructural()
    {
        assertEquals(INTERNAL_SERVER_ERROR, classifier.vetoIfInfrastructural(UNPROCESSABLE_ENTITY,
                new IllegalStateException("could not parse the message", new IOException("No space left on device"))));

        assertEquals(INTERNAL_SERVER_ERROR, classifier.vetoIfInfrastructural(UNPROCESSABLE_ENTITY,
                new IllegalStateException("could not parse the message", new SocketTimeoutException("Read timed out"))));
    }

    @Test
    void testAThrowSiteContentFailureIsLeftAloneWhenNothingSuggestsInfrastructure()
    {
        assertEquals(UNPROCESSABLE_ENTITY, classifier.vetoIfInfrastructural(UNPROCESSABLE_ENTITY,
                new IllegalStateException("could not parse the message", new EOFException())));

        // An unlisted type still stands: the transformer knew more than the lists.
        assertEquals(UNPROCESSABLE_ENTITY, classifier.vetoIfInfrastructural(UNPROCESSABLE_ENTITY,
                new IllegalStateException("nothing the lists recognise")));
    }

    @Test
    void testOnlyAnUnprocessableEntityIsSecondGuessed()
    {
        IOException infrastructural = new IOException("No space left on device");

        assertEquals(BAD_REQUEST, classifier.vetoIfInfrastructural(BAD_REQUEST, infrastructural));
        assertEquals(INSUFFICIENT_STORAGE, classifier.vetoIfInfrastructural(INSUFFICIENT_STORAGE, infrastructural));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.vetoIfInfrastructural(INTERNAL_SERVER_ERROR, infrastructural));
    }

    /** Disabling must also cover transformers that choose their own status and never reach {@code classify}. */
    @Test
    void testTheKillSwitchAlsoWithdrawsAThrowSiteDecision()
    {
        classifier.setEnabled(false);

        assertEquals(INTERNAL_SERVER_ERROR, classifier.vetoIfInfrastructural(UNPROCESSABLE_ENTITY,
                new TransformException(UNPROCESSABLE_ENTITY, "The source could not be parsed",
                        new IOException("Missing root object specification in trailer."))));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.vetoIfInfrastructural(UNPROCESSABLE_ENTITY,
                new IllegalStateException("could not parse", new IOException("No space left on device"))));
    }

    /** A {@code 422} with no cause (e.g. empty output) was determined, not inferred, so it is kept. */
    @Test
    void testTheKillSwitchLeavesADecisionThatWasNotInferredFromACause()
    {
        classifier.setEnabled(false);

        assertEquals(UNPROCESSABLE_ENTITY, classifier.vetoIfInfrastructural(UNPROCESSABLE_ENTITY,
                new TransformException(UNPROCESSABLE_ENTITY, "Transform failed to produce an output file")));
    }

    @Test
    void testTheKillSwitchLeavesTheOtherStatusesAlone()
    {
        classifier.setEnabled(false);
        Throwable e = new IllegalStateException("broke", new IOException("No space left on device"));

        assertEquals(BAD_REQUEST, classifier.vetoIfInfrastructural(BAD_REQUEST, e));
        assertEquals(INSUFFICIENT_STORAGE, classifier.vetoIfInfrastructural(INSUFFICIENT_STORAGE, e));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.vetoIfInfrastructural(INTERNAL_SERVER_ERROR, e));
    }

    @Test
    void testAnOverturnedThrowSiteDecisionIsLoggedAtWarn()
    {
        List<String> logged = captureLogs(Level.WARN);

        classifier.vetoIfInfrastructural(UNPROCESSABLE_ENTITY,
                new IllegalStateException("could not parse the message", new IOException("No space left on device")));

        assertEquals(1, logged.size(), "Overturning a transformer's own decision must be visible");
        assertTrue(logged.get(0).contains("No space left on device"), logged.get(0));
    }

    @Test
    void testTheTypesWhoseMessageIsReadCanBeOverridden()
    {
        classifier.setMessageFragmentClassNames(List.of("IllegalStateException"));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IllegalStateException("the file is corrupt")));
        // IOException is no longer configured.
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IOException("Missing root object specification in trailer.")));
    }

    @Test
    void testAnEngineCanAddAClassNameWithoutLosingTheShippedOnes()
    {
        classifier.setContributions(Map.of("engine", contribution(c -> c.setClassNames(List.of("MessagingException")))));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new MessagingException("Missing start boundary")));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("corrupt entry")),
                "Adding a name must not drop the defaults");
    }

    @Test
    void testAnEngineCanAddAMessageFragmentWithoutLosingTheShippedOnes()
    {
        classifier.setContributions(
                Map.of("engine", contribution(c -> c.setMessageFragments(List.of("invalid chunk length")))));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IOException("Invalid chunk length 5700")));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(
                new IOException("Missing root object specification in trailer.")),
                "Adding a fragment must not drop the defaults");
    }

    @Test
    void testAnEngineCanWidenWhichTypesHaveTheirMessageRead()
    {
        classifier.setContributions(
                Map.of("engine", contribution(c -> c.setMessageFragmentClassNames(List.of("IllegalStateException")))));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IllegalStateException("the file is corrupt")));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(
                new IOException("Missing root object specification in trailer.")),
                "Adding a type must not drop the defaults");
    }

    @Test
    void testAnEngineCanAddToTheInfrastructureVetoWithoutLosingTheShippedEntries()
    {
        classifier.setContributions(Map.of("engine", contribution(c -> {
            c.setInfrastructureClassNames(List.of("MessagingException"));
            c.setInfrastructureMessageFragments(List.of("quota exhausted"));
        })));

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IOException("corrupt", new MessagingException("the mail server went away"))));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new IOException("corrupt quota exhausted")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IOException("corrupt", new SocketTimeoutException("Read timed out"))),
                "Adding to the veto must not drop the defaults");
    }

    @Test
    void testEveryContributorsAdditionsApply()
    {
        classifier.setContributions(Map.of(
                "engine", contribution(c -> c.setClassNames(List.of("MessagingException"))),
                "site", contribution(c -> c.setMessageFragments(List.of("invalid chunk length")))));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new MessagingException("Missing start boundary")));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IOException("Invalid chunk length 5700")));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("corrupt entry")));
    }

    /** Spring does not guarantee setter order for a {@code @ConfigurationProperties} bean. */
    @Test
    void testReplacingAndExtendingTheSameListDoesNotDependOnTheOrderTheyWereBound()
    {
        Map<String, Contribution> contributions = Map.of("engine",
                contribution(c -> c.setClassNames(List.of("MessagingException"))));
        classifier.setContributions(contributions);
        classifier.setClassNames(List.of("ZipException"));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new MessagingException("Missing start boundary")));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("corrupt entry")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new EOFException()),
                "The replacement must still have dropped what it left out");

        TransformExceptionClassifier other = new TransformExceptionClassifier();
        other.setClassNames(List.of("ZipException"));
        other.setContributions(contributions);

        assertEquals(UNPROCESSABLE_ENTITY, other.classify(new MessagingException("Missing start boundary")));
        assertEquals(UNPROCESSABLE_ENTITY, other.classify(new ZipException("corrupt entry")));
        assertEquals(INTERNAL_SERVER_ERROR, other.classify(new EOFException()));
    }

    @Test
    void testAddingSomethingAlreadyPresentChangesNothing()
    {
        List<String> logged = captureLogs(Level.INFO);
        classifier.setContributions(Map.of(
                "engine", contribution(c -> c.setClassNames(List.of("ZipException", "MessagingException"))),
                "site", contribution(c -> c.setClassNames(List.of("MessagingException")))));

        classifier.logEffectiveConfiguration();

        assertEquals(1, logged.size());
        assertEquals(1, countOccurrences(logged.get(0), "ZipException"),
                "ZipException must appear once, not twice: " + logged.get(0));
        assertEquals(1, countOccurrences(logged.get(0), "MessagingException"),
                "MessagingException must appear once, not twice: " + logged.get(0));
        assertTrue(logged.get(0).contains("Contributions included: [engine, site]"), logged.get(0));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("corrupt entry")));
    }

    /** A blank fragment (e.g. a trailing comma in an env var) would match every message. */
    @Test
    void testBlankEntriesAreDroppedWithAWarningRatherThanMatchingEverything()
    {
        List<String> logged = captureLogs(Level.WARN);

        classifier.setMessageFragments(List.of("unexpected end of file", " "));
        classifier.setContributions(Map.of("site", contribution(c -> {
            c.setMessageFragments(Arrays.asList("", null, "  invalid chunk length  "));
            c.setClassNames(List.of(""));
        })));

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new IOException("the stream was reset")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new IllegalStateException("anonymous") {}),
                "A blank class name must not match an anonymous class, whose simple name is empty");
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IOException("Invalid chunk length 5700")),
                "Surrounding whitespace must be trimmed, not kept");
        assertEquals(List.of("unexpected end of file"), classifier.getMessageFragments());
        assertEquals(3, logged.size(), String.join("\n", logged));
        assertTrue(logged.stream().anyMatch(message -> message.contains(
                "Ignored 2 blank entries in transform.failure.classification.contributions.site.message-fragments")),
                String.join("\n", logged));
    }

    @Test
    void testMissingValuesAreTreatedAsEmpty()
    {
        Map<String, Contribution> withNull = new HashMap<>();
        withNull.put("site", null);
        classifier.setContributions(withNull);
        classifier.setContributions(null);
        classifier.setClassNames(null);

        assertEquals(List.of(), classifier.getClassNames());
        assertEquals(Map.of(), classifier.getContributions());
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new ZipException("no longer listed")));
    }

    private static Contribution contribution(Consumer<Contribution> settings)
    {
        Contribution contribution = new Contribution();
        settings.accept(contribution);
        return contribution;
    }

    private static int countOccurrences(String haystack, String needle)
    {
        // Word boundaries, or InvalidZipException would be counted as an occurrence of ZipException.
        return (int) java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(needle) + "\\b")
                .matcher(haystack)
                .results()
                .count();
    }

    /** Unrecognised exceptions must reach production logs (debug is off) so the lists can be corrected. */
    @Test
    void testAnUnrecognisedFailureIsNamedAtInfoSoTheListsCanBeCorrected()
    {
        List<String> logged = captureLogs(Level.INFO);

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IllegalStateException("something nobody has seen before")));

        assertEquals(1, logged.size(), "An unrecognised failure must be logged at info, not debug");
        String message = logged.get(0);
        assertTrue(message.contains("IllegalStateException"),
                "The log must name the exception so it can be added to the lists, but was: " + message);
        assertTrue(message.contains("something nobody has seen before"),
                "The log must carry the message, as that is what the fragment rules match on, but was: " + message);
    }

    @Test
    void testARecognisedFailureIsNotLoggedAtInfo()
    {
        List<String> logged = captureLogs(Level.INFO);

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("corrupt entry")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new SocketTimeoutException("Read timed out")));

        assertEquals(List.of(), logged, "A recognised failure is already described by the lists");
    }

    /** @return a live view of messages the classifier logs at or above {@code level}; undone by {@link #restoreLogger()}. */
    private List<String> captureLogs(Level level)
    {
        List<String> messages = new ArrayList<>();
        capturedLogger = (Logger) LoggerFactory.getLogger(TransformExceptionClassifier.class);
        originalLevel = capturedLogger.getLevel();
        appender = new AppenderBase<>() {
            @Override
            protected void append(ILoggingEvent event)
            {
                messages.add(event.getFormattedMessage());
            }
        };
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        capturedLogger.setLevel(level);
        capturedLogger.addAppender(appender);
        appender.start();
        return messages;
    }

    @AfterEach
    void restoreLogger()
    {
        if (capturedLogger != null)
        {
            capturedLogger.detachAppender(appender);
            capturedLogger.setLevel(originalLevel);
            appender.stop();
            capturedLogger = null;
        }
    }

    /** Stands in for {@code org.apache.tika.exception.TikaException}; matching is by simple name. */
    private static final class TikaException extends Exception
    {
        private static final long serialVersionUID = 1L;

        TikaException(String message)
        {
            super(message);
        }

        TikaException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    /** Stands in for {@code jakarta.mail.MessagingException}, which the defaults do not list. */
    private static final class MessagingException extends Exception
    {
        private static final long serialVersionUID = 1L;

        MessagingException(String message)
        {
            super(message);
        }
    }
}
