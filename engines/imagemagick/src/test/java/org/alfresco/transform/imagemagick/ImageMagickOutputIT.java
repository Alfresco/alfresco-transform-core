/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2026 Alfresco Software Limited
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
package org.alfresco.transform.imagemagick;

import static java.util.Collections.emptyMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import static org.alfresco.transform.base.clients.HttpClient.sendTRequest;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_BMP;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_GIF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PSD;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_TIFF;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public class ImageMagickOutputIT
{
    private static final String ENGINE_URL = "http://localhost:8090";

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF = {'G', 'I', 'F', '8'};
    private static final byte[] BMP = {'B', 'M'};
    private static final byte[] TIFF_LITTLE_ENDIAN = {'I', 'I', '*', 0};
    private static final byte[] TIFF_BIG_ENDIAN = {'M', 'M', 0, '*'};

    public static Stream<Arguments> rasterTargets()
    {
        return Stream.of(
                Arguments.of("png", MIMETYPE_IMAGE_PNG, PNG),
                Arguments.of("jpg", MIMETYPE_IMAGE_JPEG, JPEG),
                Arguments.of("gif", MIMETYPE_IMAGE_GIF, GIF),
                Arguments.of("bmp", MIMETYPE_IMAGE_BMP, BMP));
    }

    @ParameterizedTest(name = "png -> {0}")
    @MethodSource("rasterTargets")
    public void outputIsTheRequestedFormatAtTheOriginalSize(String targetExtension, String targetMimetype, byte[] signature)
    {
        byte[] output = transform("quick.png", MIMETYPE_IMAGE_PNG, targetMimetype, targetExtension, emptyMap());

        assertStartsWith(signature, output, targetExtension);
        if (!"bmp".equals(targetExtension))
        {
            assertSameSize(sourceImage("quick.png"), decode(output, targetExtension));
        }
    }

    @Test
    public void tiffOutputIsTiff()
    {
        byte[] output = transform("quick.png", MIMETYPE_IMAGE_PNG, MIMETYPE_IMAGE_TIFF, "tiff", emptyMap());

        assertTrue(startsWith(TIFF_LITTLE_ENDIAN, output) || startsWith(TIFF_BIG_ENDIAN, output),
                "tiff output does not start with a TIFF signature");
    }

    @Test
    public void resizeKeepsTheAspectRatio()
    {
        byte[] output = transform("quick.png", MIMETYPE_IMAGE_PNG, MIMETYPE_IMAGE_PNG, "png",
                Map.of("resizeWidth", "400"));

        BufferedImage source = sourceImage("quick.png");
        int expectedHeight = Math.round(source.getHeight() * 400f / source.getWidth());
        assertSize(400, expectedHeight, decode(output, "png"));
    }

    @Test
    public void cropProducesTheRequestedRegion()
    {
        byte[] output = transform("quick.png", MIMETYPE_IMAGE_PNG, MIMETYPE_IMAGE_PNG, "png",
                Map.of("cropWidth", "100", "cropHeight", "50", "cropXOffset", "0", "cropYOffset", "0"));

        assertSize(100, 50, decode(output, "png"));
    }

    @Test
    public void alphaRemoveKeepsTheImage()
    {
        byte[] output = transform("quick.png", MIMETYPE_IMAGE_PNG, MIMETYPE_IMAGE_JPEG, "jpg",
                Map.of("alphaRemove", "true"));

        assertStartsWith(JPEG, output, "jpg");
        assertSameSize(sourceImage("quick.png"), decode(output, "jpg"));
    }

    @Test
    public void aWholePsdIsTheCompositeImage()
    {
        byte[] output = transform("quick.psd", MIMETYPE_IMAGE_PSD, MIMETYPE_IMAGE_PNG, "png", emptyMap());

        BufferedImage image = decode(output, "png");
        int[] psdSize = psdSize("quick.psd");
        assertSize(psdSize[0], psdSize[1], image);
        assertTrue(distinctColours(image) > 2, "psd -> png looks like a single layer rather than the composite image");
    }

    private static byte[] transform(String sourceFile, String sourceMimetype, String targetMimetype,
            String targetExtension, Map<String, String> transformOptions)
    {
        ResponseEntity<Resource> response = sendTRequest(ENGINE_URL, sourceFile, sourceMimetype, targetMimetype,
                targetExtension, transformOptions);
        String descriptor = sourceFile + " -> " + targetExtension + " " + transformOptions;
        assertEquals(OK, response.getStatusCode(), descriptor);
        assertNotNull(response.getBody(), descriptor);
        try
        {
            byte[] output = response.getBody().getInputStream().readAllBytes();
            assertTrue(output.length > 0, descriptor + " returned no content");
            return output;
        }
        catch (IOException e)
        {
            throw new AssertionError(descriptor + " could not be read", e);
        }
    }

    private static BufferedImage decode(byte[] output, String format)
    {
        try
        {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(output));
            assertNotNull(image, format + " output could not be decoded");
            return image;
        }
        catch (IOException e)
        {
            throw new AssertionError(format + " output could not be decoded", e);
        }
    }

    private static BufferedImage sourceImage(String sourceFile)
    {
        try (InputStream in = new ClassPathResource(sourceFile).getInputStream())
        {
            return decode(in.readAllBytes(), sourceFile);
        }
        catch (IOException e)
        {
            throw new AssertionError(sourceFile + " could not be read", e);
        }
    }

    private static int[] psdSize(String sourceFile)
    {
        try (InputStream in = new ClassPathResource(sourceFile).getInputStream())
        {
            byte[] header = in.readNBytes(22);
            int height = ((header[14] & 0xFF) << 24) | ((header[15] & 0xFF) << 16) | ((header[16] & 0xFF) << 8) | (header[17] & 0xFF);
            int width = ((header[18] & 0xFF) << 24) | ((header[19] & 0xFF) << 16) | ((header[20] & 0xFF) << 8) | (header[21] & 0xFF);
            return new int[]{width, height};
        }
        catch (IOException e)
        {
            throw new AssertionError(sourceFile + " could not be read", e);
        }
    }

    private static void assertSameSize(BufferedImage expected, BufferedImage image)
    {
        assertSize(expected.getWidth(), expected.getHeight(), image);
    }

    private static void assertSize(int width, int height, BufferedImage image)
    {
        assertEquals(width + "x" + height, image.getWidth() + "x" + image.getHeight(), "image size");
    }

    private static void assertStartsWith(byte[] signature, byte[] output, String format)
    {
        assertTrue(startsWith(signature, output), format + " output does not start with the " + format + " signature");
    }

    private static boolean startsWith(byte[] signature, byte[] output)
    {
        return output.length >= signature.length
                && Arrays.equals(signature, Arrays.copyOf(output, signature.length));
    }

    private static int distinctColours(BufferedImage image)
    {
        Set<Integer> colours = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y++)
        {
            for (int x = 0; x < image.getWidth(); x++)
            {
                colours.add(image.getRGB(x, y));
            }
        }
        return colours.size();
    }
}
