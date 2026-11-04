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
 * Feeds genuinely unreadable content to the real jakarta.mail, Commons Compress and ImageIO on this module's classpath, and asserts the status the transformer answers with.
 * <p/>
 * These three libraries report damage in ways no central list can safely claim: jakarta.mail by a type {@code engines/base} has never heard of, ImageIO by an {@code IIOException} whose text is whichever reader happened to be chosen. The transformers here decide it themselves, because this is the only code that knows what was being read. Building the damaged input in the test rather than checking in fixtures keeps the damage visible to a reader.
 */
class MiscFailureClassificationTest
{
    @TempDir
    Path tempDir;

    /**
     * jakarta.mail is extremely forgiving - it will accept almost any bytes as a message with a single text part - so the case that matters is a message whose own headers promise a structure the body does not deliver. That is what a truncated or rewritten {@code .eml} looks like.
     */
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

    /**
     * The same library on a second entry point. Metadata extraction has its own parse and never goes through {@link EMLTransformer}, so the decision made there does not carry; and because the repository asks for metadata on nearly every upload, this is the path an unreadable {@code .eml} reaches first.
     * <p/>
     * {@code MimeMessage} reads lazily, so the failure surfaces at an accessor rather than at construction - here {@code AddressException}, a {@code MessagingException}, raised while parsing the address headers.
     */
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

    /**
     * The real extractor, with the one thing a unit test cannot supply. The mapping is normally put in place by {@code AbstractMetadataExtractorEmbedder} as it reads the properties file, which only happens inside a Spring context; everything being asserted here happens in the extractor's own code.
     */
    private static final class MappedRfc822Extractor extends RFC822MetadataExtractor
    {
        @Override
        protected Map<String, Set<String>> getExtractMapping()
        {
            return Map.of("messageFrom", Set.of("messageFrom"), "messageTo", Set.of("messageTo"));
        }
    }

    /**
     * iWorks files are zips. Commons Compress reports anything that is not one as an {@code IOException}, and the transformer cannot tell that apart from a read that failed, so it hands the decision on with the cause attached.
     */
    @Test
    void testAnIWorksFileThatIsNotAZipIsTerminal() throws IOException
    {
        File source = write("broken.key", "this is not a zip file, it is a sentence".getBytes(StandardCharsets.UTF_8));

        TransformException e = assertThrows(TransformException.class,
                () -> new AppleIWorksContentTransformer().transform(MIMETYPE_IWORK_KEYNOTE, MIMETYPE_IMAGE_JPEG,
                        Map.of(), source, target("out.jpg"), null));

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(), describe(e));
    }

    /**
     * A valid zip with no preview inside it is the case most easily mistaken for an engine fault: nothing failed to read, there is simply nothing to return. Retrying reads the same entries and finds the same nothing.
     */
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

    /**
     * ImageIO chooses a reader from the bytes themselves, so having none means the header is not the format the request claimed.
     */
    @Test
    void testAnImageNoReaderRecognisesIsTerminal() throws IOException
    {
        File source = write("broken.png", "not an image".getBytes(StandardCharsets.UTF_8));

        TransformException e = assertThrows(TransformException.class,
                () -> new ImageToPdfTransformer().transform(MIMETYPE_IMAGE_PNG, MIMETYPE_PDF, Map.of(),
                        source, target("out.pdf"), null));

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus(), describe(e));
    }

    /**
     * The case a central list cannot reach. The header survives, so a reader is still chosen and the failure happens deep inside it, reported as an {@code IIOException} whose wording - "Error reading PNG metadata", "Invalid chunk length" - belongs to that one reader. Nothing in it says "corrupt", so before this was decided at the throw site a truncated image was reported as an engine fault and retried.
     */
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

    /**
     * The counterweight. Every assertion above says a file is beyond saving, so one has to prove an intact image still converts - otherwise a change that reported everything as damaged would pass.
     */
    @Test
    void testAnUndamagedImageStillTransforms() throws Exception
    {
        File source = write("sample.png", read("sample.png"));
        File target = target("out.pdf");

        new ImageToPdfTransformer().transform(MIMETYPE_IMAGE_PNG, MIMETYPE_PDF, Map.of(), source, target, null);

        assertTrue(Files.size(target.toPath()) > 0, "Expected a pdf of the image");
    }

    /**
     * The case that showed catching {@code IIOException} alone was not enough.
     * <p/>
     * Damaging a TIFF anywhere near its tags makes the reader fail while still reading them, which it reports properly as an {@code IIOException}. The interesting damage is further in: leave the header and the tag directory intact, so every strip is still described exactly as before, and corrupt only the compressed data those tags point at. The reader then believes what the tags told it and hands the bytes to the decompressor, which is where it falls over - for LZW, with {@code NullPointerException: Cannot read the array length because "oldString" is null} from inside the JDK's own decompressor. No {@code IIOException} is ever constructed, so before this the document was reported as an engine fault and retried three times before being lost.
     * <p/>
     * It is fair to call that a bug in the decompressor. It is still terminal, because a retry hands the same decompressor the same bytes.
     * <p/>
     * The image is written here rather than checked in so that the damage is visible, and so the layout is known: this writer puts the tag directory at the front, which is what makes "corrupt the data but not the tags" expressible as a byte range.
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
     * @return {@code tiff} with a fifth of the bytes between its tag directory and its last strip replaced. The untouched margins keep the header, the tags and the final strip's framing intact, so the file still describes itself correctly and only what it describes is wrong.
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

    /**
     * @return every exception in the chain, so a failure names the library wording rather than just the outermost wrapper.
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
