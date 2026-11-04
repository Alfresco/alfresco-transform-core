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
package org.alfresco.transform.tika.transformers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.tika.parser.AutoDetectParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.alfresco.transform.exceptions.TransformException;

/**
 * Feeds genuinely damaged documents to the real Tika, POI and PDFBox on the classpath and asserts the status the engine answers with.
 * <p/>
 * The classification rules match on exception class names and on fragments of message text, neither of which the compiler can check. A test that builds its own {@code new IOException("corrupt")} proves only that the list contains the word it was written from. These damage real files instead, so the assertions are pinned to what the shipped libraries actually throw: if a library upgrade reworded a message or changed which exception it raises, this test fails rather than the behaviour silently regressing in production.
 * <p/>
 * The corruptions are applied in code rather than checked in as fixtures, so the damage is visible to a reader and no binary needs reviewing.
 */
// The Tika entry point this exercises declares Exception, so a helper that calls it has to as well.
@SuppressWarnings("PMD.SignatureDeclareThrowsException")
class TikaFailureClassificationTest
{
    /**
     * Each case names a source file and a way of damaging it. The formats chosen are the ones a repository is most likely to hold: OOXML, the legacy OLE2 formats POI reports failures from quite differently, ODF and PDF.
     */
    static Stream<Arguments> damagedDocuments()
    {
        return Stream.of(
                // OOXML: the zip survives but its XML does not, so the failure surfaces as a SAX parse error
                Arguments.of("quick.docx", Damage.HALVED),
                Arguments.of("quick.xlsx", Damage.HALVED),
                Arguments.of("quick.pptx", Damage.HALVED),
                // cut short enough that the zip's own structure is gone
                Arguments.of("quick.docx", Damage.MOSTLY_REMOVED),
                Arguments.of("quick.xlsx", Damage.MOSTLY_REMOVED),
                // the part Tika needs is simply not there
                Arguments.of("quick.pptx", Damage.MOSTLY_REMOVED),
                // the zip entries are present but their deflate streams are not decodable
                Arguments.of("quick.docx", Damage.BODY_SCRAMBLED),
                Arguments.of("quick.xlsx", Damage.BODY_SCRAMBLED),
                Arguments.of("quick.pptx", Damage.BODY_SCRAMBLED),
                // OLE2: POI reports damage as an IndexOutOfBoundsException about a missing block, which is the case a
                // central list of exception names cannot claim without also claiming every engine bug of that shape
                Arguments.of("quick.doc", Damage.HALVED),
                Arguments.of("quick.doc", Damage.MOSTLY_REMOVED),
                Arguments.of("quick.doc", Damage.BODY_SCRAMBLED),
                // ODF
                Arguments.of("quick.odt", Damage.HALVED),
                Arguments.of("quick.odt", Damage.BODY_SCRAMBLED),
                // PDF: the shape originally reported, where PDFBox cannot find the trailer
                Arguments.of("quick.pdf", Damage.HALVED),
                Arguments.of("quick.pdf", Damage.MOSTLY_REMOVED),
                Arguments.of("quick.pdf", Damage.BODY_SCRAMBLED));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("damagedDocuments")
    void testADamagedDocumentIsTerminal(String name, Damage damage) throws Exception
    {
        byte[] damaged = damage.apply(read(name));

        TransformException e = assertThrows(TransformException.class, () -> parse(damaged),
                name + " " + damage + " must be reported as a content failure");

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(),
                () -> name + " " + damage + " was reported as " + e.getStatus() + ": " + describe(e));
    }

    /**
     * Tika has a type of its own for this, and it has to keep working: an empty file is the commonest way for a failed upload to reach a transform.
     */
    @Test
    void testAnEmptyDocumentIsTerminal()
    {
        TransformException e = assertThrows(TransformException.class, () -> parse(new byte[0]));

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(), describe(e));
    }

    @Test
    void testXmlThatIsNotWellFormedIsTerminal()
    {
        TransformException e = assertThrows(TransformException.class,
                () -> parse("<?xml version=\"1.0\"?><a><b></a>".getBytes(StandardCharsets.UTF_8)));

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(), describe(e));
    }

    /**
     * The counterweight to the rest of the class. Every assertion above says a document is beyond saving, so one case has to prove the engine can still read a document it was given intact - otherwise a change that reported everything as damaged would pass.
     */
    @Test
    void testAnUndamagedDocumentStillTransforms() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        parse(read("quick.docx"), out);

        assertEquals(true, out.toString(StandardCharsets.UTF_8).contains("quick brown fox"),
                "Expected the text of the document, but got: " + out.toString(StandardCharsets.UTF_8));
    }

    enum Damage
    {
        /** The file stops half way, as it would if a download were cut short or a disk filled during an upload. */
        HALVED
        {
            @Override
            byte[] apply(byte[] original)
            {
                return truncate(original, original.length / 2);
            }
        },
        /** Only the opening fragment survives, so even the container's own structure is missing. */
        MOSTLY_REMOVED
        {
            @Override
            byte[] apply(byte[] original)
            {
                return truncate(original, original.length / 20);
            }
        },
        /** The header is intact, so the format is still detected and a parser is still chosen, but the content behind it is nonsense. This is the case that reaches furthest into a library before failing. */
        BODY_SCRAMBLED
        {
            @Override
            byte[] apply(byte[] original)
            {
                byte[] damaged = original.clone();
                for (int i = 600; i < damaged.length; i += 3)
                {
                    damaged[i] = (byte) 0xA5;
                }
                return damaged;
            }
        };

        abstract byte[] apply(byte[] original);

        static byte[] truncate(byte[] original, int length)
        {
            byte[] shorter = new byte[length];
            System.arraycopy(original, 0, shorter, 0, length);
            return shorter;
        }
    }

    private static void parse(byte[] bytes) throws Exception
    {
        parse(bytes, OutputStream.nullOutputStream());
    }

    private static void parse(byte[] bytes, OutputStream out) throws Exception
    {
        Tika tika = new Tika();
        try (InputStream in = new ByteArrayInputStream(bytes))
        {
            tika.transform(new AutoDetectParser(), null, in, out,
                    new String[]{Tika.TARGET_MIMETYPE + MIMETYPE_TEXT_PLAIN, Tika.TARGET_ENCODING + "UTF-8"});
        }
    }

    private static byte[] read(String name) throws IOException
    {
        try (InputStream in = TikaFailureClassificationTest.class.getClassLoader().getResourceAsStream(name))
        {
            if (in == null)
            {
                throw new IOException("Test resource " + name + " is missing");
            }
            return in.readAllBytes();
        }
    }

    /**
     * @return every exception in the chain, so a failure report names the library wording the rules have to keep up with rather than just the outermost wrapper.
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
