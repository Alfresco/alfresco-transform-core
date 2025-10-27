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
import static org.mockito.BDDMockito.then;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_GIF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_TIFF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.RequestParamMap.END_PAGE;
import static org.alfresco.transform.common.RequestParamMap.PDF_FORMAT;
import static org.alfresco.transform.common.RequestParamMap.PDF_ORIENTATION;
import static org.alfresco.transform.common.RequestParamMap.START_PAGE;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.misc.util.ArgumentsCartesianProduct;

@SuppressWarnings("PMD.AvoidAccessibilityAlteration")
class ImageToPdfTransformerTest
{
    private static final File sourceFile = loadFile("sample.gif");
    private static final File SOURCE_TIFF_FILE = loadFile("sample.tiff");
    private static final int sourceFileWidth;
    private static final int sourceFileHeight;

    @Mock
    private TransformManager transformManager;

    @InjectMocks
    private ImageToPdfTransformer transformer;

    private File targetFile = null;

    @BeforeEach
    void setUp() throws IOException
    {
        MockitoAnnotations.openMocks(this);

        targetFile = File.createTempFile("temp_target", ".pdf");
    }

    static Stream<ImageFile> imageFiles()
    {
        return Stream.of(
                ImageFile.of("sample.jpg", MIMETYPE_IMAGE_JPEG),
                ImageFile.of("sample.gif", MIMETYPE_IMAGE_GIF),
                ImageFile.of("sample.png", MIMETYPE_IMAGE_PNG));
    }

    static Stream<TransformOptions> defaultTransformOptions()
    {
        return Stream.of(
                TransformOptions.none(),
                TransformOptions.of(0, null),
                TransformOptions.of(0, 0));
    }

    static Stream<TransformOptions> tiffTransformOptions()
    {
        return Stream.of(
                TransformOptions.of(0, 0), // (startPage, endPage)
                TransformOptions.of(0, 1),
                TransformOptions.of(1, 1),
                TransformOptions.of(null, 0), // expected 1 page in target file
                TransformOptions.of(null, 1), // expected 2 pages in target file
                TransformOptions.of(0, null), // expected all pages in target file
                TransformOptions.of(1, null), // expected all except first page in target file
                TransformOptions.none() // expected all pages in target file
        );
    }

    static Stream<Arguments> transformSourcesAndOptions()
    {
        ImageFile tiffImage = ImageFile.of("sample.tiff", MIMETYPE_IMAGE_TIFF, 6);
        return Stream.of(
                ArgumentsCartesianProduct.of(imageFiles(), defaultTransformOptions()),
                ArgumentsCartesianProduct.of(tiffImage, tiffTransformOptions())).flatMap(Function.identity());
    }

    @ParameterizedTest
    @MethodSource("transformSourcesAndOptions")
    void testTransformImageToPdf(ImageFile imageFile, TransformOptions transformOptions) throws Exception
    {
        File sourceFile = loadFile(imageFile.fileName);

        // when
        transformer.transform(imageFile.mimetype, MIMETYPE_PDF, transformOptions.toMap(), sourceFile, targetFile, transformManager);

        then(transformManager).shouldHaveNoInteractions();
        try (PDDocument actualPdfDocument = Loader.loadPDF(targetFile))
        {
            int expectedNumberOfPages = calculateExpectedNumberOfPages(transformOptions, imageFile.firstPage(), imageFile.lastPage());
            assertNotNull(actualPdfDocument);
            assertEquals(expectedNumberOfPages, actualPdfDocument.getNumberOfPages());
        }
    }

    private static int calculateExpectedNumberOfPages(TransformOptions transformOptions, int firstPage, int lastPage)
    {
        int startPage = Optional.ofNullable(transformOptions.startPage).orElse(firstPage);
        int endPage = Math.min(Optional.ofNullable(transformOptions.endPage).orElse(lastPage), lastPage);
        return endPage - startPage + 1;
    }

    static Stream<TransformOptions> improperTransformOptions()
    {
        return Stream.of(
                TransformOptions.of(1, 0),
                TransformOptions.of(-1, 0),
                TransformOptions.of(0, -1));
    }

    @ParameterizedTest
    @MethodSource("improperTransformOptions")
    void testTransformTiffToPdf_withImproperOptions(TransformOptions transformOptions)
    {
        // when
        assertThrows(IllegalArgumentException.class, () -> transformer.transform(MIMETYPE_IMAGE_TIFF, MIMETYPE_PDF, transformOptions.toMap(), sourceFile, targetFile, transformManager));
    }

    @Test
    void testTransformTiffToPdf_withInvalidStartPageOption()
    {
        Map<String, String> transformOptions = TransformOptions.none().toMap();
        transformOptions.put(START_PAGE, "a");

        // when
        assertThrows(IllegalArgumentException.class, () -> transformer.transform(MIMETYPE_IMAGE_TIFF, MIMETYPE_PDF, transformOptions, sourceFile, targetFile, transformManager));
    }

    @Test
    void testTransformTiffToPdf_withInvalidEndPageOption()
    {
        Map<String, String> transformOptions = TransformOptions.none().toMap();
        transformOptions.put(END_PAGE, "z");

        // when
        assertThrows(IllegalArgumentException.class, () -> transformer.transform(MIMETYPE_IMAGE_TIFF, MIMETYPE_PDF, transformOptions, sourceFile, targetFile, transformManager));
    }

    /** Option and expected dimensions. */
    static Stream<Arguments> validPdfFormats()
    {
        return Stream.of(
                Arguments.of("DEFAULT", new PDRectangle(sourceFileWidth, sourceFileHeight)),
                Arguments.of("default", new PDRectangle(sourceFileWidth, sourceFileHeight)),
                Arguments.of("A0", PDRectangle.A0),
                Arguments.of("a0", PDRectangle.A0),
                Arguments.of("A1", PDRectangle.A1),
                Arguments.of("A2", PDRectangle.A2),
                Arguments.of("A3", PDRectangle.A3),
                Arguments.of("A4", PDRectangle.A4),
                Arguments.of("A5", PDRectangle.A5),
                Arguments.of("A6", PDRectangle.A6),
                Arguments.of("A6", PDRectangle.A6),
                Arguments.of("LETTER", PDRectangle.LETTER),
                Arguments.of("letter", PDRectangle.LETTER),
                Arguments.of("LEGAL", PDRectangle.LEGAL),
                Arguments.of("legal", PDRectangle.LEGAL));
    }

    /** Option and expected orientation. */
    static Stream<Arguments> validPdfOrientations()
    {
        return Stream.of(
                Arguments.of("DEFAULT", unchangedRectangle()),
                Arguments.of("default", unchangedRectangle()),
                Arguments.of("PORTRAIT", rectangleRotatedIf((width, height) -> width > height)),
                Arguments.of("portrait", rectangleRotatedIf((width, height) -> width > height)),
                Arguments.of("LANDSCAPE", rectangleRotatedIf((width, height) -> height > width)),
                Arguments.of("landscape", rectangleRotatedIf((width, height) -> height > width)));
    }

    static Stream<Arguments> validPdfFormatsAndOrientations()
    {
        return ArgumentsCartesianProduct.ofArguments(
                validPdfFormats(),
                validPdfOrientations());
    }

    @ParameterizedTest
    @MethodSource("validPdfFormatsAndOrientations")
    void testTransformImageToPDF_withVariousPdfFormatsAndOrientations(String pdfFormat, PDRectangle expectedPdfFormat,
            String pdfOrientation, BiFunction<Float, Float, PDRectangle> expectedPdfFormatRotator) throws Exception
    {
        TransformOptions transformOptions = TransformOptions.of(pdfFormat, pdfOrientation);

        // when
        transformer.transform(MIMETYPE_IMAGE_TIFF, MIMETYPE_PDF, transformOptions.toMap(), sourceFile, targetFile, transformManager);

        try (PDDocument actualPdfDocument = Loader.loadPDF(targetFile))
        {
            PDRectangle finalExpectedPdfFormat = expectedPdfFormatRotator.apply(expectedPdfFormat.getWidth(), expectedPdfFormat.getHeight());
            assertNotNull(actualPdfDocument);
            assertEquals(finalExpectedPdfFormat.getWidth(), actualPdfDocument.getPage(0).getMediaBox().getWidth());
            assertEquals(finalExpectedPdfFormat.getHeight(), actualPdfDocument.getPage(0).getMediaBox().getHeight());
        }
    }

    @Test
    void testTransformImageToPDF_withInvalidPdfFormatAndUsingDefaultOne() throws Exception
    {
        TransformOptions transformOptions = TransformOptions.of("INVALID");

        // when
        transformer.transform(MIMETYPE_IMAGE_TIFF, MIMETYPE_PDF, transformOptions.toMap(), sourceFile, targetFile, transformManager);

        try (PDDocument actualPdfDocument = Loader.loadPDF(targetFile))
        {
            BufferedImage actualImage = ImageIO.read(sourceFile);
            assertNotNull(actualPdfDocument);
            assertEquals(actualImage.getWidth(), actualPdfDocument.getPage(0).getMediaBox().getWidth());
            assertEquals(actualImage.getHeight(), actualPdfDocument.getPage(0).getMediaBox().getHeight());
        }
    }

    @Test
    void testTransformImageToPDF_withInvalidPdfOrientationAndUsingDefaultOne() throws Exception
    {
        TransformOptions transformOptions = TransformOptions.of(null, "INVALID");

        // when
        transformer.transform(MIMETYPE_IMAGE_TIFF, MIMETYPE_PDF, transformOptions.toMap(), sourceFile, targetFile, transformManager);

        try (PDDocument actualPdfDocument = Loader.loadPDF(targetFile))
        {
            BufferedImage actualImage = ImageIO.read(sourceFile);
            assertNotNull(actualPdfDocument);
            assertEquals(actualImage.getWidth(), actualPdfDocument.getPage(0).getMediaBox().getWidth());
            assertEquals(actualImage.getHeight(), actualPdfDocument.getPage(0).getMediaBox().getHeight());
        }
    }

    static Stream<Arguments> imageFilesOfVariousSizeAndResolution()
    {
        return Stream.of(
                Arguments.of(ImageFile.of("MNT-24205.tiff", MIMETYPE_IMAGE_TIFF), 612.0f, 792.0f),
                Arguments.of(ImageFile.of("459x594-50.tif", MIMETYPE_IMAGE_TIFF), 660.0f, 855.0f),
                Arguments.of(ImageFile.of("459x594-72.tif", MIMETYPE_IMAGE_TIFF), 459.0f, 594.0f),
                Arguments.of(ImageFile.of("459x594-300.tif", MIMETYPE_IMAGE_TIFF), 110.0f, 142.0f),
                Arguments.of(ImageFile.of("612x792-50.tif", MIMETYPE_IMAGE_TIFF), 881.0f, 1140.0f),
                Arguments.of(ImageFile.of("612x792-72.tif", MIMETYPE_IMAGE_TIFF), 612.0f, 792.0f),
                Arguments.of(ImageFile.of("612x792-300.tif", MIMETYPE_IMAGE_TIFF), 146.0f, 190.0f),
                Arguments.of(ImageFile.of("765x990-50.tif", MIMETYPE_IMAGE_TIFF), 1101.0f, 1425.0f),
                Arguments.of(ImageFile.of("765x990-72.tif", MIMETYPE_IMAGE_TIFF), 765.0f, 990.0f),
                Arguments.of(ImageFile.of("765x990-300.tif", MIMETYPE_IMAGE_TIFF), 183.0f, 237.0f));
    }

    @ParameterizedTest
    @MethodSource("imageFilesOfVariousSizeAndResolution")
    void testTransformTiffToPDF_withVariousImageSizes(ImageFile imageFile, float expectedWidth, float expectedHeight) throws Exception
    {
        TransformOptions transformOptions = TransformOptions.of("DEFAULT");

        File source = loadFile(imageFile.fileName);

        // when
        transformer.transform(imageFile.mimetype, MIMETYPE_PDF, transformOptions.toMap(), source, targetFile, transformManager);

        try (PDDocument actualPdfDocument = Loader.loadPDF(targetFile))
        {
            assertNotNull(actualPdfDocument);
            assertEquals(expectedWidth, actualPdfDocument.getPage(0).getMediaBox().getWidth(), "Pdf width");
            assertEquals(expectedHeight, actualPdfDocument.getPage(0).getMediaBox().getHeight(), "Pdf height");
        }
    }

    @Test
    void testFindImageReaderForTiffFiles()
    {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(SOURCE_TIFF_FILE))
        {
            Method method = ImageToPdfTransformer.class.getDeclaredMethod(
                    "findImageReader", ImageInputStream.class, String.class, String.class);
            method.setAccessible(true);
            // when
            ImageReader imageReader = (ImageReader) method.invoke(
                    transformer, imageInputStream, "sample.tiff", MIMETYPE_IMAGE_TIFF);

            // then
            assertNotNull(imageReader, "Image reader should not be null for TIFF file");
            assertEquals("com.sun.imageio.plugins.tiff.TIFFImageReader", imageReader.getClass().getName(),
                    "ImageReader should be com.sun.imageio.plugins.tiff.TIFFImageReader");
        }
        catch (Exception e)
        {
            Assertions.fail("Exception occurred: " + e.getMessage());
        }
    }
    // ----------------------------------------------- Helper methods and classes -----------------------------------------------

    private static BiFunction<Float, Float, PDRectangle> unchangedRectangle()
    {
        return rectangleRotatedIf(null);
    }

    private static BiFunction<Float, Float, PDRectangle> rectangleRotatedIf(BiPredicate<Float, Float> predicate)
    {
        if (predicate == null)
        {
            return PDRectangle::new;
        }

        return (width, height) -> predicate.test(width, height) ? new PDRectangle(height, width) : new PDRectangle(width, height);
    }

    private static File loadFile(String fileName)
    {
        return new File(Objects.requireNonNull(ImageToPdfTransformerTest.class.getClassLoader().getResource(fileName)).getFile());
    }

    private static class ImageFile
    {
        String fileName;
        String mimetype;
        int numberOfPages;

        private ImageFile(String fileName, String mimetype, int numberOfPages)
        {
            this.fileName = fileName;
            this.mimetype = mimetype;
            this.numberOfPages = numberOfPages;
        }

        public static ImageFile of(String fileName, String mimetype, int numberOfPages)
        {
            return new ImageFile(fileName, mimetype, numberOfPages);
        }

        public static ImageFile of(String fileName, String mimetype)
        {
            return of(fileName, mimetype, 1);
        }

        public int firstPage()
        {
            return 0;
        }

        public int lastPage()
        {
            return numberOfPages - 1;
        }

        @Override
        public String toString()
        {
            return "ImageFile{" + "fileName='" + fileName + '\'' + ", mimetype='" + mimetype + '\'' + '}';
        }
    }

    private static class TransformOptions
    {
        Integer startPage;
        Integer endPage;
        String pdfFormat;
        String pdfOrientation;

        private TransformOptions(Integer startPage, Integer endPage, String pdfFormat, String pdfOrientation)
        {
            this.startPage = startPage;
            this.endPage = endPage;
            this.pdfFormat = pdfFormat;
            this.pdfOrientation = pdfOrientation;
        }

        public Map<String, String> toMap()
        {
            final Map<String, String> transformOptions = new HashMap<>();
            if (startPage != null)
            {
                transformOptions.put(START_PAGE, startPage.toString());
            }
            if (endPage != null)
            {
                transformOptions.put(END_PAGE, endPage.toString());
            }
            if (pdfFormat != null)
            {
                transformOptions.put(PDF_FORMAT, pdfFormat);
            }
            if (pdfOrientation != null)
            {
                transformOptions.put(PDF_ORIENTATION, pdfOrientation);
            }
            return transformOptions;
        }

        public static TransformOptions of(Integer startPage, Integer endPage)
        {
            return new TransformOptions(startPage, endPage, null, null);
        }

        public static TransformOptions of(String pdfFormat)
        {
            return new TransformOptions(null, null, pdfFormat, null);
        }

        public static TransformOptions of(String pdfFormat, String pdfOrientation)
        {
            return new TransformOptions(null, null, pdfFormat, pdfOrientation);
        }

        public static TransformOptions none()
        {
            return TransformOptions.of(null);
        }

        @Override
        public String toString()
        {
            return "TransformOption{" + "startPage=" + startPage + ", endPage=" + endPage + '}';
        }
    }

    static
    {
        try
        {
            BufferedImage image = ImageIO.read(sourceFile);
            sourceFileWidth = image.getWidth();
            sourceFileHeight = image.getHeight();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
