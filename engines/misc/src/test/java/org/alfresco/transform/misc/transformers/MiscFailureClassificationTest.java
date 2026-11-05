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
package org.alfresco.transform.misc.transformers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_TIFF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IWORK_KEYNOTE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_RFC822;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transform.misc.metadataExtractors.RFC822MetadataExtractor;

/**
 * Feeds damaged content to the real jakarta.mail, Commons Compress and ImageIO and checks the status returned.
 */
class MiscFailureClassificationTest
{
    @TempDir
    Path tempDir;

    /** jakarta.mail accepts almost any bytes, so the damage is headers promising a structure the body lacks. */
    @Test
    void testAnEmlPromisingAStructureItDoesNotContainIsTerminal() throws IOException
    {
        File source = write("broken.eml", ("From: someone@example.com\r\n"
                + "Subject: a message that lies about its own shape\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: multipart/mixed; boundary=\"THIS-BOUNDARY-NEVER-APPEARS\"\r\n"
                + "\r\n"
                + "there is no part here at all\r\n").getBytes(StandardCharsets.UTF_8));

        TransformException e = assertThrows(TransformException.class,
                () -> new EMLTransformer().transform(MIMETYPE_RFC822, MIMETYPE_TEXT_PLAIN, Map.of(),
                        source, target("out.txt"), null));

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(), describe(e));
    }

    @Test
    void testAReadableEmlStillTransforms() throws Exception
    {
        File source = write("quick.eml", read("quick.eml"));
        File target = target("out.txt");

        new EMLTransformer().transform(MIMETYPE_RFC822, MIMETYPE_TEXT_PLAIN, Map.of(), source, target, null);

        assertTrue(Files.size(target.toPath()) > 0, "Expected the text of the message");
    }

    /** {@code MimeMessage} parses lazily, so this fails at an accessor with {@code AddressException}. */
    @Test
    void testAnEmlWithAnUnreadableAddressHeaderIsTerminalOnTheMetadataPath() throws IOException
    {
        try (InputStream source = eml("From: <<<broken\r\nTo: ok@example.com\r\n\r\nbody\r\n"))
        {
            TransformException e = assertThrows(TransformException.class,
                    () -> new MappedRfc822Extractor().extractMetadata(MIMETYPE_RFC822, source,
                            MIMETYPE_TEXT_PLAIN, new ByteArrayOutputStream(), Map.of(), null));

            assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(), describe(e));
        }
    }

    @Test
    void testAReadableEmlStillYieldsMetadataOnTheMetadataPath() throws Exception
    {
        try (InputStream source = eml("From: someone@example.com\r\n"
                + "To: other@example.com\r\n"
                + "Subject: perfectly readable\r\n"
                + "\r\n"
                + "body\r\n"))
        {
            Map<String, Serializable> metadata = new MappedRfc822Extractor().extractMetadata(MIMETYPE_RFC822, source,
                    MIMETYPE_TEXT_PLAIN, new ByteArrayOutputStream(), Map.of(), null);

            assertEquals("someone@example.com", metadata.get("messageFrom"));
        }
    }

    private static InputStream eml(String content)
    {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    /** Supplies the extract mapping that Spring would normally load from the properties file. */
    private static final class MappedRfc822Extractor extends RFC822MetadataExtractor
    {
        @Override
        protected Map<String, Set<String>> getExtractMapping()
        {
            return Map.of("messageFrom", Set.of("messageFrom"), "messageTo", Set.of("messageTo"));
        }
    }

    @Test
    void testAnIWorksFileThatIsNotAZipIsTerminal() throws IOException
    {
        File source = write("broken.key", "this is not a zip file, it is a sentence".getBytes(StandardCharsets.UTF_8));

        TransformException e = assertThrows(TransformException.class,
                () -> new AppleIWorksContentTransformer().transform(MIMETYPE_IWORK_KEYNOTE, MIMETYPE_IMAGE_JPEG,
                        Map.of(), source, target("out.jpg"), null));

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(), describe(e));
    }

    @Test
    void testAnIWorksFileWithNoPreviewInsideIsTerminal() throws IOException
    {
        ByteArrayOutputStream zipped = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(zipped))
        {
            zip.putNextEntry(new ZipEntry("Index/Document.iwa"));
            zip.write("content that is not a preview".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        File source = write("nopreview.key", zipped.toByteArray());

        TransformException e = assertThrows(TransformException.class,
                () -> new AppleIWorksContentTransformer().transform(MIMETYPE_IWORK_KEYNOTE, MIMETYPE_IMAGE_JPEG,
                        Map.of(), source, target("out.jpg"), null));

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(), describe(e));
    }

    @Test
    void testAnImageNoReaderRecognisesIsTerminal() throws IOException
    {
        File source = write("broken.png", "not an image".getBytes(StandardCharsets.UTF_8));

        TransformException e = assertThrows(TransformException.class,
                () -> new ImageToPdfTransformer().transform(MIMETYPE_IMAGE_PNG, MIMETYPE_PDF, Map.of(),
                        source, target("out.pdf"), null));

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(), describe(e));
    }

    /** The header survives, so a reader is chosen and fails with reader-specific {@code IIOException} wording. */
    @Test
    void testATruncatedImageIsTerminal() throws IOException
    {
        byte[] whole = read("sample.png");
        byte[] half = new byte[whole.length / 2];
        System.arraycopy(whole, 0, half, 0, half.length);
        File source = write("truncated.png", half);

        TransformException e = assertThrows(TransformException.class,
                () -> new ImageToPdfTransformer().transform(MIMETYPE_IMAGE_PNG, MIMETYPE_PDF, Map.of(),
                        source, target("out.pdf"), null));

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(), describe(e));
    }

    @Test
    void testAnImageWithAnIntactHeaderButScrambledBodyIsTerminal() throws IOException
    {
        byte[] scrambled = read("sample.png");
        for (int i = 40; i < scrambled.length; i += 7)
        {
            scrambled[i] = (byte) 0xFF;
        }
        File source = write("scrambled.png", scrambled);

        TransformException e = assertThrows(TransformException.class,
                () -> new ImageToPdfTransformer().transform(MIMETYPE_IMAGE_PNG, MIMETYPE_PDF, Map.of(),
                        source, target("out.pdf"), null));

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(), describe(e));
    }

    @Test
    void testAnUndamagedImageStillTransforms() throws Exception
    {
        File source = write("sample.png", read("sample.png"));
        File target = target("out.pdf");

        new ImageToPdfTransformer().transform(MIMETYPE_IMAGE_PNG, MIMETYPE_PDF, Map.of(), source, target, null);

        assertTrue(Files.size(target.toPath()) > 0, "Expected a pdf of the image");
    }

    /**
     * Header and tags intact, compressed strip data corrupt: the JDK's LZW decompressor throws a bare {@code NullPointerException}, not {@code IIOException}.
     */
    @Test
    void testATiffWhoseCompressedDataIsCorruptIsTerminal() throws IOException
    {
        File source = write("corrupt.tiff", corruptCompressedData(writeLzwTiff()));

        TransformException e = assertThrows(TransformException.class,
                () -> new ImageToPdfTransformer().transform(MIMETYPE_IMAGE_TIFF, MIMETYPE_PDF, Map.of(),
                        source, target("out.pdf"), null));

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(), describe(e));
    }

    @Test
    void testAnUndamagedTiffStillTransforms() throws Exception
    {
        File source = write("sample.tiff", writeLzwTiff());
        File target = target("out.pdf");

        new ImageToPdfTransformer().transform(MIMETYPE_IMAGE_TIFF, MIMETYPE_PDF, Map.of(), source, target, null);

        assertTrue(Files.size(target.toPath()) > 0, "Expected a pdf of the image");
    }

    /**
     * @return {@code tiff} with a fifth of the bytes between the tag directory (written first) and the last strip replaced, leaving header and tags intact.
     */
    private static byte[] corruptCompressedData(byte[] tiff)
    {
        byte[] damaged = tiff.clone();
        Random random = new Random(1);
        for (int i = 4096; i < damaged.length - 1024; i++)
        {
            if (random.nextInt(5) == 0)
            {
                damaged[i] = (byte) random.nextInt(256);
            }
        }
        return damaged;
    }

    private static byte[] writeLzwTiff() throws IOException
    {
        BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++)
        {
            for (int y = 0; y < image.getHeight(); y++)
            {
                image.setRGB(x, y, (x * 7 + y * 13) & 0xFFFFFF);
            }
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName("tiff").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionType("LZW");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out))
        {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), param);
        }
        finally
        {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private File write(String name, byte[] content) throws IOException
    {
        Path path = tempDir.resolve(name);
        Files.write(path, content);
        return path.toFile();
    }

    private File target(String name) throws IOException
    {
        Path path = tempDir.resolve(name);
        Files.createFile(path);
        return path.toFile();
    }

    private static byte[] read(String name) throws IOException
    {
        try (InputStream in = MiscFailureClassificationTest.class.getClassLoader().getResourceAsStream(name))
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
