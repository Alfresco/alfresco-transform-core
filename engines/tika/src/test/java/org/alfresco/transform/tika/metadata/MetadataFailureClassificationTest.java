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
 * Asserts the status a metadata extract answers with when the document cannot be parsed.
 * <p/>
 * Metadata extraction is a second route through the engine, and the one an unreadable document meets first: the repository asks for an extract on essentially every upload, long before anything asks for a rendition. It does not go through {@code Tika.transform}, so classifying failures there left this path untouched - and because the extractors are driven by the same libraries, the same damaged document that was answered terminally as a rendition was still answered as an engine fault here, and still retried.
 * <p/>
 * The extractors are driven directly rather than over HTTP so the status is read from the exception itself. As with the sibling tests, real documents are damaged in code: the point is to pin the behaviour to what the shipped libraries actually throw, which for this path is mostly not what a central list would predict.
 */
// The metadata extraction entry point declares Exception, so a helper that calls it has to as well.
@SuppressWarnings("PMD.SignatureDeclareThrowsException")
class MetadataFailureClassificationTest
{
    /**
     * Each case names a document and how to break it. The legacy OLE2 formats are the ones that matter here: POI reports damage in them quite differently depending on how far it gets, and the deepest of those failures is the one nothing else catches.
     * <p/>
     * TIFF is absent deliberately. The engine extracts image metadata with {@code IPTCMetadataExtractor}, which shells out to ExifTool - a native binary present in the image and not on a unit test's classpath - and plain Tika is tolerant enough of a damaged TIFF to return partial metadata rather than fail. That path is covered by the end-to-end run instead.
     */
    static Stream<Arguments> damagedDocuments()
    {
        return Stream.of(
                // POI reports a damaged OLE2 header as an IOException, which the central class name list already names
                Arguments.of("quick.doc", Damage.MOSTLY_REMOVED),
                // but a scrambled body leaves POI walking structures it has already believed, and it emerges as a bare
                // NullPointerException("Cannot read the array length because \"oldString\" is null") or an
                // ArrayIndexOutOfBoundsException - no message to match on, and types no central list can claim without
                // also claiming every engine bug of the same shape
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

    /**
     * The case that justifies catching {@code RuntimeException} around the parse. POI leaves these with no cause, no useful message and a type shared with ordinary bugs, so the central lists cannot reach them - only the code that knows a document was being parsed can. Asserting the shape as well as the status keeps the reason visible if POI ever starts reporting it properly.
     */
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
     * An empty file is the commonest way a failed upload reaches a transform, and the extract is what meets it.
     * <p/>
     * This one is left for the central lists on purpose. POI names it itself, and it travels as an {@code IOException} - which the throw site deliberately does not catch, because at that point an {@code IOException} is just as likely to be the source download dying, and that has to stay retryable. The engine still answers {@code 422}, by the other route: {@code NotOLE2FileException} is on the classifier's class name list, where {@code ClassifierLibraryPinningTest} holds it against this same library. Pinned here because the two halves dividing the work is the design, and a later change that "helpfully" widened this catch to {@code IOException} would break it.
     */
    @Test
    void testAnEmptyDocumentIsLeftForTheCentralListsAsAnIoException()
    {
        IOException e = assertThrows(IOException.class,
                () -> extract(new OfficeMetadataExtractor(), new byte[0], MIMETYPE_WORD));

        assertEquals("NotOLE2FileException", e.getClass().getSimpleName(), describe(e));
    }

    /**
     * The counterweight. Every assertion above says a document is beyond reading, so one has to prove an intact one still yields its metadata - otherwise an extractor that failed on everything would pass.
     */
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
        /** The header is intact, so the format is still detected and a parser is still chosen, but what it then reads is nonsense. This is the case that reaches furthest into a library before failing, and the one that produced the bare runtime exceptions. */
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

    /**
     * @return every exception in the chain, so a failure report names the library wording rather than just the outermost wrapper.
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
