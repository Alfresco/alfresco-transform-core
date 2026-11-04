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
 * Pins {@link TransformExceptionClassifier}'s class name and message fragment lists against the libraries they describe.
 * <p/>
 * The lists name types from Tika, POI, PDFBox and Commons Imaging, none of which {@code engines/base} depends on, and match some of them on wording the libraries chose. Neither can be checked by the compiler, so the unit tests next to the classifier can only build exceptions from the same strings the lists were written from - they would stay green if every one of those names were wrong. This test lives in a module that has the libraries on its classpath, damages real documents, and classifies whatever the libraries genuinely raise. A rewording or a renamed exception turns the build red here.
 * <p/>
 * It is in the classifier's own package because the class is package-private; {@code engines/tika} already depends on the {@code engines/base} test jar, so the package is shared.
 */
// Catching Throwable and declaring Exception are the point of this test: what the libraries raise is precisely what it
// must not assume, and POI reaches for unchecked types the classifier is deliberately not told about.
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
                // Tika's own type for an empty source, which is how a failed upload usually arrives
                Arguments.of("an empty source", "quick.docx", truncateTo(Integer.MAX_VALUE)),
                // the zip behind an OOXML file cut short, reported through java.util.zip
                Arguments.of("a docx with its zip structure gone", "quick.docx", truncateTo(20)),
                Arguments.of("an xlsx with its zip structure gone", "quick.xlsx", truncateTo(20)),
                // the XML inside an OOXML file cut short, which arrives as a SAX parse error
                Arguments.of("a docx with unterminated xml", "quick.docx", truncateTo(2)),
                Arguments.of("an xlsx with unterminated xml", "quick.xlsx", truncateTo(2)),
                // an ODF file whose zip end header has gone
                Arguments.of("an odt cut in half", "quick.odt", truncateTo(2)),
                // the shape originally reported: PDFBox cannot find the trailer and says so only in the message
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

    /**
     * PDFBox reports structural damage as a plain {@code IOException} whose message is the only signal, so the fragment matching it is the most fragile entry in the whole configuration. Going through PDFBox directly, rather than through Tika's wrapper, keeps the assertion on PDFBox's own wording.
     */
    @Test
    void testPdfBoxStillWordsAMissingTrailerTheWayTheFragmentsExpect() throws IOException
    {
        byte[] halved = truncateTo(2).damage(read("quick.pdf"));

        Throwable raised = expectFailure(() -> Loader.loadPDF(halved).close());

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(raised),
                () -> "The message fragments no longer match what PDFBox reports." + describe(raised));
    }

    /**
     * POI announces this one by type, so it pins a name in the class list against the name POI actually uses. The list holds the superclass, which only works because POI still extends it.
     */
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
     * The case the central lists are deliberately not asked to decide. POI reports a damaged OLE2 file as an {@code IndexOutOfBoundsException} about a missing block, which is also exactly what an engine bug looks like, so claiming it centrally would risk dropping documents a retry would have saved. The engine that owns the library answers it at the throw site instead - see {@code TikaFailureClassificationTest}. Asserting the fallback here keeps that boundary visible: if a later change did add the type to the lists, this test says so.
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

    /**
     * @return every exception in the chain, so a failure names the library wording the rules have to keep up with rather than just the outermost wrapper.
     */
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
