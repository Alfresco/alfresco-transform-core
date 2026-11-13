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
package org.alfresco.transform.tika.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_WORD;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transform.tika.metadata.extractors.OfficeMetadataExtractor;

/**
 * Asserts the status a metadata extract returns for damaged documents. Extraction bypasses {@code Tika.transform}, so it needs its own coverage.
 */
// The extraction entry point declares Exception.
@SuppressWarnings("PMD.SignatureDeclareThrowsException")
class MetadataFailureClassificationTest
{
    /** TIFF is omitted: image metadata uses ExifTool, which is not on the unit test classpath. */
    static Stream<Arguments> damagedDocuments()
    {
        return Stream.of(
                // damaged OLE2 header: IOException named in the central list
                Arguments.of("quick.doc", Damage.MOSTLY_REMOVED),
                // scrambled body: bare NullPointerException or ArrayIndexOutOfBoundsException from POI
                Arguments.of("quick.doc", Damage.BODY_SCRAMBLED),
                Arguments.of("quick.doc", Damage.HALVED));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("damagedDocuments")
    void testMetadataOfADamagedDocumentIsTerminal(String name, Damage damage) throws Exception
    {
        byte[] damaged = damage.apply(read(name));

        TransformException e = assertThrows(TransformException.class,
                () -> extract(new OfficeMetadataExtractor(), damaged, MIMETYPE_WORD),
                name + " " + damage + " must be reported as a content failure");

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(),
                () -> name + " " + damage + " was reported as " + e.getStatus() + ": " + describe(e));
    }

    /** POI throws these with no cause or useful message; only the throw site can classify them. */
    @Test
    void testAnOle2FailurePoiReportsAsABareRuntimeExceptionIsStillTerminal() throws Exception
    {
        byte[] damaged = Damage.BODY_SCRAMBLED.apply(read("quick.doc"));

        TransformException e = assertThrows(TransformException.class,
                () -> extract(new OfficeMetadataExtractor(), damaged, MIMETYPE_WORD));

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(), describe(e));
        assertInstanceOf(RuntimeException.class, e.getCause(),
                () -> "Expected the bare runtime failure this catch exists for, but was: " + describe(e));
    }

    /**
     * The throw site must not catch {@code IOException} (it may be a failed download); the central list classifies {@code NotOLE2FileException}.
     */
    @Test
    void testAnEmptyDocumentIsLeftForTheCentralListsAsAnIoException()
    {
        IOException e = assertThrows(IOException.class,
                () -> extract(new OfficeMetadataExtractor(), new byte[0], MIMETYPE_WORD));

        assertEquals("NotOLE2FileException", e.getClass().getSimpleName(), describe(e));
    }

    @Test
    void testMetadataOfAnUndamagedDocumentIsStillExtracted() throws Exception
    {
        Map<String, Serializable> properties = extract(new OfficeMetadataExtractor(), read("quick.doc"), MIMETYPE_WORD);

        assertFalse(properties.isEmpty(), "Expected the properties of the document");
    }

    private static Map<String, Serializable> extract(AbstractTikaMetadataExtractorEmbeddor extractor, byte[] bytes,
            String sourceMimetype) throws Exception
    {
        try (InputStream in = new ByteArrayInputStream(bytes))
        {
            return extractor.extractMetadata(sourceMimetype, in, null, OutputStream.nullOutputStream(), Map.of(), null);
        }
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
        /** Header intact, so a parser is chosen, but the body is nonsense; produces the bare runtime exceptions. */
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

    private static byte[] read(String name) throws IOException
    {
        try (InputStream in = MetadataFailureClassificationTest.class.getClassLoader().getResourceAsStream(name))
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
