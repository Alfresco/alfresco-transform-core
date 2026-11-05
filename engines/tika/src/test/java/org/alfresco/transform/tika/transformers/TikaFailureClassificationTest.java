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
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
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

import org.alfresco.transform.base.transform.EngineStatus;

/**
 * Feeds damaged documents to the real Tika, POI and PDFBox and asserts the engine's status, so library changes to exceptions or wording fail here.
 */
// The Tika entry point declares Exception.
@SuppressWarnings("PMD.SignatureDeclareThrowsException")
class TikaFailureClassificationTest
{
    static Stream<Arguments> damagedDocuments()
    {
        return Stream.of(
                // OOXML: zip survives, XML does not (SAX parse error)
                Arguments.of("quick.docx", Damage.HALVED),
                Arguments.of("quick.xlsx", Damage.HALVED),
                Arguments.of("quick.pptx", Damage.HALVED),
                // zip structure gone
                Arguments.of("quick.docx", Damage.MOSTLY_REMOVED),
                Arguments.of("quick.xlsx", Damage.MOSTLY_REMOVED),
                // the part Tika needs is missing
                Arguments.of("quick.pptx", Damage.MOSTLY_REMOVED),
                // zip entries present but deflate streams undecodable
                Arguments.of("quick.docx", Damage.BODY_SCRAMBLED),
                Arguments.of("quick.xlsx", Damage.BODY_SCRAMBLED),
                Arguments.of("quick.pptx", Damage.BODY_SCRAMBLED),
                // OLE2: POI throws IndexOutOfBoundsException, which only the throw site can classify
                Arguments.of("quick.doc", Damage.HALVED),
                Arguments.of("quick.doc", Damage.MOSTLY_REMOVED),
                Arguments.of("quick.doc", Damage.BODY_SCRAMBLED),
                // ODF
                Arguments.of("quick.odt", Damage.HALVED),
                Arguments.of("quick.odt", Damage.BODY_SCRAMBLED),
                // PDF: PDFBox cannot find the trailer
                Arguments.of("quick.pdf", Damage.HALVED),
                Arguments.of("quick.pdf", Damage.MOSTLY_REMOVED),
                Arguments.of("quick.pdf", Damage.BODY_SCRAMBLED));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("damagedDocuments")
    void testADamagedDocumentIsTerminal(String name, Damage damage) throws Exception
    {
        byte[] damaged = damage.apply(read(name));

        Exception e = assertThrows(Exception.class, () -> parse(damaged),
                name + " " + damage + " must fail");

        assertEquals(UNPROCESSABLE_ENTITY, EngineStatus.of(e),
                () -> name + " " + damage + " was reported as " + EngineStatus.of(e) + ": " + describe(e));
    }

    @Test
    void testAnEmptyDocumentIsTerminal()
    {
        Exception e = assertThrows(Exception.class, () -> parse(new byte[0]));

        assertEquals(UNPROCESSABLE_ENTITY, EngineStatus.of(e), describe(e));
    }

    @Test
    void testXmlThatIsNotWellFormedIsTerminal()
    {
        Exception e = assertThrows(Exception.class,
                () -> parse("<?xml version=\"1.0\"?><a><b></a>".getBytes(StandardCharsets.UTF_8)));

        assertEquals(UNPROCESSABLE_ENTITY, EngineStatus.of(e), describe(e));
    }

    /** A source that fails to read mid-parse must stay retryable. */
    @Test
    void testAReadThatFailsUnderTheParserIsRetryable() throws IOException
    {
        byte[] start = read("quick.docx");
        try (InputStream failingRead = new FailsPartWayThrough(start, start.length / 2))
        {
            Exception e = assertThrows(Exception.class, () -> new Tika().transform(new AutoDetectParser(), null,
                    failingRead, OutputStream.nullOutputStream(),
                    new String[]{Tika.TARGET_MIMETYPE + MIMETYPE_TEXT_PLAIN, Tika.TARGET_ENCODING + "UTF-8"}));

            assertEquals(INTERNAL_SERVER_ERROR, EngineStatus.of(e), describe(e));
        }
    }

    /** Serves the first {@code readable} bytes of {@code content}, then fails as a dropped source would. */
    private static final class FailsPartWayThrough extends InputStream
    {
        private final byte[] content;
        private final int readable;
        private int position;

        FailsPartWayThrough(byte[] content, int readable)
        {
            this.content = content;
            this.readable = readable;
        }

        @Override
        public int read() throws IOException
        {
            if (position >= readable)
            {
                throw new IOException("Unexpected read failure");
            }
            return content[position++] & 0xFF;
        }
    }

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
        /** The file stops half way, like a cut-short download. */
        HALVED
        {
            @Override
            byte[] apply(byte[] original)
            {
                return truncate(original, original.length / 2);
            }
        },
        /** Only the opening fragment survives. */
        MOSTLY_REMOVED
        {
            @Override
            byte[] apply(byte[] original)
            {
                return truncate(original, original.length / 20);
            }
        },
        /** Header intact, so a parser is chosen, but the body is nonsense. */
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
