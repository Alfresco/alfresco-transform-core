/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2025 Alfresco Software Limited
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.pdfbox.Loader;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Pins the classifier's lists to what the real Tika, POI and PDFBox throw, so a renamed exception or reworded message fails the build.
 */
// Catching Throwable is the point: the test must not assume what the libraries raise.
@SuppressWarnings({"PMD.AvoidCatchingThrowable", "PMD.SignatureDeclareThrowsException"})
class ClassifierLibraryPinningTest
{
    private TransformExceptionClassifier classifier;

    @BeforeEach
    void setUp()
    {
        classifier = new TransformExceptionClassifier();
    }

    static Stream<Arguments> damageTheListsMustStillRecognise()
    {
        return Stream.of(
                // empty source: Tika's own exception type
                Arguments.of("an empty source", "quick.docx", truncateTo(Integer.MAX_VALUE)),
                // OOXML zip cut short: java.util.zip
                Arguments.of("a docx with its zip structure gone", "quick.docx", truncateTo(20)),
                Arguments.of("an xlsx with its zip structure gone", "quick.xlsx", truncateTo(20)),
                // OOXML XML cut short: SAX parse error
                Arguments.of("a docx with unterminated xml", "quick.docx", truncateTo(2)),
                Arguments.of("an xlsx with unterminated xml", "quick.xlsx", truncateTo(2)),
                // ODF zip end header gone
                Arguments.of("an odt cut in half", "quick.odt", truncateTo(2)),
                // PDFBox missing trailer, matched only by message
                Arguments.of("a pdf cut in half", "quick.pdf", truncateTo(2)),
                Arguments.of("a pdf with almost nothing left", "quick.pdf", truncateTo(20)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("damageTheListsMustStillRecognise")
    void testWhatTheLibrariesRaiseIsStillRecognised(String description, String file, Damager damager) throws IOException
    {
        Throwable raised = parseExpectingFailure(damager.damage(read(file)));

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(raised),
                () -> description + " is no longer recognised by the classification lists." + describe(raised));
    }

    /** Calls PDFBox directly so the assertion is on PDFBox's own wording, the most fragile fragment. */
    @Test
    void testPdfBoxStillWordsAMissingTrailerTheWayTheFragmentsExpect() throws IOException
    {
        byte[] halved = truncateTo(2).damage(read("quick.pdf"));

        Throwable raised = expectFailure(() -> Loader.loadPDF(halved).close());

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(raised),
                () -> "The message fragments no longer match what PDFBox reports." + describe(raised));
    }

    /** The list holds the superclass, so this also checks POI still extends it. */
    @Test
    void testPoiStillNamesAnOle2FileOfferedAsOoxml() throws IOException
    {
        Throwable raised = expectFailure(() -> {
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(read("quick.doc"))))
            {
                document.getParagraphs();
            }
        });

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(raised),
                () -> "OLE2NotOfficeXmlFileException is no longer matched." + describe(raised));
    }

    @Test
    void testPoiStillReportsATruncatedOoxmlZipInWordsTheFragmentsMatch() throws IOException
    {
        byte[] damaged = truncateTo(3).damage(read("quick.docx"));

        Throwable raised = expectFailure(() -> {
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(damaged)))
            {
                document.getParagraphs();
            }
        });

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(raised),
                () -> "A truncated OOXML zip is no longer matched." + describe(raised));
    }

    /**
     * The central lists leave POI's {@code IndexOutOfBoundsException} at 500; the Tika engine handles it where it is thrown.
     */
    @Test
    void testDamageOnlyPoiCanRecogniseIsLeftToTheEngineThatOwnsPoi() throws IOException
    {
        byte[] damaged = truncateTo(3).damage(read("quick.doc"));

        Throwable raised = expectFailure(() -> {
            try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(damaged)))
            {
                document.getText();
            }
        });

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(raised),
                () -> "The central lists now claim POI's OLE2 damage, which they were not meant to." + describe(raised));
    }

    @FunctionalInterface
    interface Damager
    {
        byte[] damage(byte[] original);
    }

    @FunctionalInterface
    interface FailingCall
    {
        void run() throws Exception;
    }

    private static Damager truncateTo(int denominator)
    {
        return original -> {
            byte[] shorter = new byte[denominator == Integer.MAX_VALUE ? 0 : original.length / denominator];
            System.arraycopy(original, 0, shorter, 0, shorter.length);
            return shorter;
        };
    }

    private static Throwable parseExpectingFailure(byte[] bytes)
    {
        return expectFailure(() -> new AutoDetectParser().parse(new ByteArrayInputStream(bytes),
                new BodyContentHandler(-1), new Metadata(), new ParseContext()));
    }

    private static Throwable expectFailure(FailingCall call)
    {
        try
        {
            call.run();
        }
        catch (Throwable t)
        {
            return t;
        }
        throw new AssertionError("The library accepted bytes this test damaged, so the test no longer proves anything");
    }

    private static byte[] read(String name) throws IOException
    {
        try (InputStream in = ClassifierLibraryPinningTest.class.getClassLoader().getResourceAsStream(name))
        {
            assertNotNull(in, "Test resource " + name + " is missing");
            return in.readAllBytes();
        }
    }

    /** @return every exception in the chain, so a failure shows the library's wording. */
    private static String describe(Throwable e)
    {
        StringBuilder sb = new StringBuilder();
        Set<Throwable> seen = new HashSet<>();
        for (Throwable cause = e; cause != null && seen.add(cause); cause = cause.getCause())
        {
            sb.append("\n  caused by ").append(cause.getClass().getName()).append(": ").append(cause.getMessage());
        }
        return sb.toString();
    }
}
