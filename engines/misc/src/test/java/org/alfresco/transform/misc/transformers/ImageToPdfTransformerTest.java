/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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

import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_GIF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_TIFF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.RequestParamMap.END_PAGE;
import static org.alfresco.transform.common.RequestParamMap.PDF_FORMAT;
import static org.alfresco.transform.common.RequestParamMap.PDF_ORIENTATION;
import static org.alfresco.transform.common.RequestParamMap.START_PAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.then;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.misc.util.ArgumentsCartesianProduct;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ImageToPdfTransformerTest
{
    private static final File sourceFile = loadFile("sample.gif");

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
            ImageFile.of("sample.png", MIMETYPE_IMAGE_PNG)
        );
    }

    static Stream<TransformOptions> defaultTransformOptions()
    {
        return Stream.of(
            TransformOptions.none(),
            TransformOptions.of(0, null),
            TransformOptions.of(0, 0)
        );
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
            ArgumentsCartesianProduct.of(tiffImage, tiffTransformOptions())
        ).flatMap(Function.identity());
    }

    @ParameterizedTest
    @MethodSource("transformSourcesAndOptions")
    void testTransformImageToPdf(ImageFile imageFile, TransformOptions transformOptions) throws Exception
    {
        File sourceFile = loadFile(imageFile.fileName);

        // when
        transformer.transform(imageFile.mimetype, MIMETYPE_PDF, transformOptions.toMap(), sourceFile, targetFile, transformManager);

        then(transformManager).shouldHaveNoInteractions();
        try (PDDocument actualPdfDocument = PDDocument.load(targetFile))
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
            TransformOptions.of(0, -1)
        );
    }

    @ParameterizedTest
    @MethodSource("improperTransformOptions")
    void testTransformTiffToPdf_withImproperOptions(TransformOptions transformOptions)
    {
        // when
        assertThrows(IllegalArgumentException.class, () ->
            transformer.transform(MIMETYPE_IMAGE_TIFF, MIMETYPE_PDF, transformOptions.toMap(), sourceFile, targetFile, transformManager));
    }

    @Test
    void testTransformTiffToPdf_withInvalidStartPageOption()
    {
        Map<String, String> transformOptions = TransformOptions.none().toMap();
        transformOptions.put(START_PAGE, "a");

        // when
        assertThrows(IllegalArgumentException.class, () ->
            transformer.transform(MIMETYPE_IMAGE_TIFF, MIMETYPE_PDF, transformOptions, sourceFile, targetFile, transformManager));
    }

    @Test
    void testTransformTiffToPdf_withInvalidEndPageOption()
    {
        Map<String, String> transformOptions = TransformOptions.none().toMap();
        transformOptions.put(END_PAGE, "z");

        // when
        assertThrows(IllegalArgumentException.class, () ->
            transformer.transform(MIMETYPE_IMAGE_TIFF, MIMETYPE_PDF, transformOptions, sourceFile, targetFile, transformManager));
    }

    static Stream<Arguments> validPdfFormatsAndOrientations()
    {
        return ArgumentsCartesianProduct.of(
            Stream.of("default", "DEFAULT", "A0", "a0", "A1", "A2", "A3", "A4", "A5", "A6", "a6", "LETTER", "letter", "LEGAL", "legal"),
            Stream.of("default", "DEFAULT", "portrait", "PORTRAIT", "landscape", "LANDSCAPE")
        );
    }

    @ParameterizedTest
    @MethodSource("validPdfFormatsAndOrientations")
    void testTransformImageToPDF_withVariousPdfFormatsAndOrientations(String pdfFormat, String pdfOrientation) throws Exception
    {
        TransformOptions transformOptions = TransformOptions.of(pdfFormat, pdfOrientation);

        // when
        transformer.transform(MIMETYPE_IMAGE_TIFF, MIMETYPE_PDF, transformOptions.toMap(), sourceFile, targetFile, transformManager);

        try (PDDocument actualPdfDocument = PDDocument.load(targetFile))
        {
            BufferedImage actualImage = ImageIO.read(sourceFile);
            PDRectangle expectedPdfFormat = resolveExpectedPdfFormat(pdfFormat, pdfOrientation, actualImage.getWidth(), actualImage.getHeight());
            assertNotNull(actualPdfDocument);
            assertEquals(expectedPdfFormat.getWidth(), actualPdfDocument.getPage(0).getMediaBox().getWidth());
            assertEquals(expectedPdfFormat.getHeight(), actualPdfDocument.getPage(0).getMediaBox().getHeight());
        }
    }

    @Test
    void testTransformImageToPDF_withInvalidPdfFormatAndUsingDefaultOne() throws Exception
    {
        TransformOptions transformOptions = TransformOptions.of("INVALID");

        // when
        transformer.transform(MIMETYPE_IMAGE_TIFF, MIMETYPE_PDF, transformOptions.toMap(), sourceFile, targetFile, transformManager);

        try (PDDocument actualPdfDocument = PDDocument.load(targetFile))
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

        try (PDDocument actualPdfDocument = PDDocument.load(targetFile))
        {
            BufferedImage actualImage = ImageIO.read(sourceFile);
            assertNotNull(actualPdfDocument);
            assertEquals(actualImage.getWidth(), actualPdfDocument.getPage(0).getMediaBox().getWidth());
            assertEquals(actualImage.getHeight(), actualPdfDocument.getPage(0).getMediaBox().getHeight());
        }
    }

    //----------------------------------------------- Helper methods and classes -----------------------------------------------

    private static PDRectangle resolveExpectedPdfFormat(String pdfFormat, String pdfOrientation, int defaultWidth, int defaultHeight)
    {
        PDRectangle pdRectangle;
        switch (pdfFormat.toUpperCase()) {
        case "LETTER":
            pdRectangle = PDRectangle.LETTER;
            break;
        case "LEGAL":
            pdRectangle = PDRectangle.LEGAL;
            break;
        case "A0":
            pdRectangle = PDRectangle.A0;
            break;
        case "A1":
            pdRectangle = PDRectangle.A1;
            break;
        case "A2":
            pdRectangle = PDRectangle.A2;
            break;
        case "A3":
            pdRectangle = PDRectangle.A3;
            break;
        case "A4":
            pdRectangle = PDRectangle.A4;
            break;
        case "A5":
            pdRectangle = PDRectangle.A5;
            break;
        case "A6":
            pdRectangle = PDRectangle.A6;
            break;
        default:
            pdRectangle = new PDRectangle(defaultWidth, defaultHeight);
        }

        switch (pdfOrientation.toUpperCase()) {
        case "PORTRAIT":
            if (pdRectangle.getWidth() > pdRectangle.getHeight())
            {
                pdRectangle = new PDRectangle(pdRectangle.getHeight(), pdRectangle.getWidth());
            }
            break;
        case "LANDSCAPE":
            if (pdRectangle.getHeight() > pdRectangle.getWidth())
            {
                pdRectangle = new PDRectangle(pdRectangle.getHeight(), pdRectangle.getWidth());
            }
            break;
        }

        return pdRectangle;
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
}