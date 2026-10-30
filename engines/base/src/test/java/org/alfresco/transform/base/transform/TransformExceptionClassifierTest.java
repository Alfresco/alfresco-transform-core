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
import java.util.List;
import java.util.zip.ZipException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Checks that a document the engine cannot parse is reported as {@code 422} so the T-Router stops retrying it, while anything that might be a genuine engine or infrastructure problem stays {@code 500} so it is still retried.
 */
// One assertion per rule is what makes a misclassification traceable to the rule that caused it, so the method count
// follows the number of rules rather than any structure worth splitting. Same reasoning as StreamHandlerTest.
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
        // The exact failure reported in MNT-25570: a .docx renamed to .pdf.
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
        // Tika's CompositeParser rethrows every parser IOException as a TikaException, so the wrapper alone must
        // never decide the outcome - otherwise a full disk permanently drops a transform a retry would have fixed.
        Exception diskFull = new IllegalStateException("TIKA-198: Illegal IOException from parser",
                new TikaException("TIKA-198: Illegal IOException from parser",
                        new IOException("No space left on device")));

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(diskFull));
    }

    @Test
    void testTikaExceptionAloneIsNotTreatedAsAContentFailure()
    {
        // It is Tika's universal wrapper, not a content failure type.
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new TikaException("Unexpected RuntimeException from parser")));
    }

    /**
     * POI declares a general content failure and then throws a more specific subclass of it. The shipped 5.4.1 throws {@code OldExcelFormatException} where the listed type is its parent {@code OldFileFormatException}, and {@code OLE2NotOfficeXmlFileException} where the listed type is {@code NotOfficeXmlFileException}. Matching only the runtime name missed every one of those.
     */
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
        // The walk must not turn every exception into a content failure: nothing in this hierarchy is listed.
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new UnrecognisedParserException()));
    }

    @Test
    void testTheInfrastructureVetoAlsoMatchesSubclasses()
    {
        // FileSystemException is listed; its subclasses must veto too, as 500-and-retry is the safe direction.
        Exception wrapped = new ZipException("corrupt entry");
        wrapped.initCause(new FileSystemLoopException("/tmp/work"));

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(wrapped));
    }

    /** Mirrors POI's {@code OldExcelFormatException extends OldFileFormatException}, where only the parent is listed. */
    private static class OldFileFormatException extends IllegalArgumentException
    {
        private static final long serialVersionUID = 1L;
    }

    private static class OldExcelFormatException extends OldFileFormatException
    {
        private static final long serialVersionUID = 1L;
    }

    /** Nothing in this hierarchy is listed, so the walk must leave it at 500. */
    private static class UnrecognisedParserException extends IllegalStateException
    {
        private static final long serialVersionUID = 1L;
    }

    @Test
    void testInfrastructureVetoBeatsAContentFailureEarlierInTheChain()
    {
        // The content rule matches the outermost exception, the veto matches deeper. The veto must win.
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
        // This is the shape Tika produces for `--targetEncoding=rubbish`. The charset name comes from the caller's
        // transform options, not from the file, so it fails every document identically. Classifying it as 422 would
        // let the router drop each of those requests silently instead of surfacing the caller's mistake as a 500.
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
        // The safe default: retrying a doomed document only wastes time, but not retrying a
        // recoverable one loses a transform that would have succeeded.
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
        // transform.failure.classification.enabled=false must make every unhandled exception a
        // 500 again, so a deployment can back the feature out without downgrading.
        classifier.setEnabled(false);

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IOException("Missing root object specification in trailer.")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new ZipException("invalid stored block lengths")));
    }

    @Test
    void testTheClassNameListCanBeOverridden()
    {
        // Replacing rather than appending is deliberate: an entry causing a false positive has
        // to be removable, not just addable.
        classifier.setClassNames(List.of("IllegalArgumentException"));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IllegalArgumentException("bad glyph table")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new ZipException("invalid stored block lengths")));
    }

    @Test
    void testTheMessageFragmentListCanBeOverriddenAndIsCaseInsensitive()
    {
        classifier.setMessageFragments(List.of("Unreadable Glyph Table"));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IOException("unreadable glyph table at offset 12")));
        // The MNT-25570 fragment is no longer configured, so it must fall back to 500.
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IOException("Missing root object specification in trailer.")));
    }

    @Test
    void testAFullyQualifiedClassNameAlsoMatches()
    {
        // Lets an engine that shades or relocates a library still be configured precisely.
        classifier.setClassNames(List.of("java.util.zip.ZipException"));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("invalid entry size")));
    }

    @Test
    void testTheInfrastructureVetoCanBeOverridden()
    {
        Exception wrapped = new ZipException("corrupt entry");
        wrapped.initCause(new SocketTimeoutException("Read timed out"));

        // SocketTimeoutException is vetoed by default, so the content rule on the wrapper loses.
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(wrapped));

        // Drop it from the veto list and the content rule is allowed to decide instead.
        classifier.setInfrastructureClassNames(List.of("SocketException"));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(wrapped));
    }

    /**
     * Several content fragments - "corrupt", "damaged", "truncated" - are words an engine might use about itself. Reading them out of any exception would turn an engine bug into a terminal 422 and drop a transform that a retry would have fixed.
     */
    @Test
    void testAContentFragmentIsOnlyReadFromAParserFailureType()
    {
        assertEquals(INTERNAL_SERVER_ERROR,
                classifier.classify(new IllegalStateException("corrupt temporary output")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new RuntimeException("the engine is damaged")));
        assertEquals(INTERNAL_SERVER_ERROR,
                classifier.classify(new IllegalArgumentException("truncated command line")));

        // The same words from the types the parsers actually report content damage with still decide.
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IOException("corrupt object stream")));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new TikaException("the document is damaged")));
    }

    /**
     * The restriction applies only to the content list. The veto has to stay broad, or an infrastructure failure wrapped in something unexpected would no longer be recognised.
     */
    @Test
    void testTheInfrastructureVetoStillReadsTheMessageOfAnyType()
    {
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IllegalStateException("corrupt entry", new IllegalStateException("No space left on device"))));
    }

    @Test
    void testTheTypesWhoseMessageIsReadCanBeOverridden()
    {
        classifier.setMessageFragmentClassNames(List.of("IllegalStateException"));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IllegalStateException("the file is corrupt")));
        // IOException is no longer configured, so MNT-25570's own shape must fall back to 500.
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IOException("Missing root object specification in trailer.")));
    }

    /**
     * The defaults are inferred rather than harvested from real failures, so the one signal that can correct them - an exception the lists do not recognise - has to survive into a production log, where debug is off.
     */
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

    /**
     * Only unhandled transform failures reach the classifier at all, but a recognised one teaches nothing, so it must not add to the production log.
     */
    @Test
    void testARecognisedFailureIsNotLoggedAtInfo()
    {
        List<String> logged = captureLogs(Level.INFO);

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("corrupt entry")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new SocketTimeoutException("Read timed out")));

        assertEquals(List.of(), logged, "A recognised failure is already described by the lists");
    }

    /**
     * @return a live view of the messages logged by the classifier at or above {@code level}. Undone by {@link #restoreLogger()}, so the level change cannot leak into the other tests sharing this JVM.
     */
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

    /**
     * Stands in for {@code org.apache.tika.exception.TikaException}. Matching is by class <i>name</i>, so a local class of the same simple name exercises the real rule without {@code engines/base} depending on Tika.
     */
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
}
