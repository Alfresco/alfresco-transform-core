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
import static org.alfresco.transform.common.RequestParamMap.START_PAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.then;

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
    private static final File sourceFile = loadFile("quick.gif");

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
            ImageFile.of("quick.jpg", MIMETYPE_IMAGE_JPEG),
            ImageFile.of("quick.gif", MIMETYPE_IMAGE_GIF),
            ImageFile.of("quick.png", MIMETYPE_IMAGE_PNG)
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
            TransformOptions.of(1, null), // expected 1 page in target file
            TransformOptions.none() // expected all pages in target file
        );
    }

    static Stream<Arguments> transformSourcesAndOptions()
    {
        return Stream.of(
            ArgumentsCartesianProduct.of(imageFiles(), defaultTransformOptions()),
            ArgumentsCartesianProduct.of(ImageFile.of("quick.tiff", MIMETYPE_IMAGE_TIFF, 6), tiffTransformOptions())
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

    static Stream<String> validPdfFormats()
    {
        return Stream.of("A0", "a0", "A1", "A2", "A3", "A4", "A5", "A6", "a6", "LETTER", "letter", "LEGAL", "legal");
    }

    @ParameterizedTest
    @MethodSource("validPdfFormats")
    void testTransformImageToPDF_withVariousPdfFormats(String pdfFormat) throws Exception
    {
        TransformOptions transformOptions = TransformOptions.of(pdfFormat);

        // when
        transformer.transform(MIMETYPE_IMAGE_TIFF, MIMETYPE_PDF, transformOptions.toMap(), sourceFile, targetFile, transformManager);

        try (PDDocument actualPdfDocument = PDDocument.load(targetFile))
        {
            PDRectangle expectedPdfFormat = resolveExpectedPdfFormat(pdfFormat);
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
            assertNotNull(actualPdfDocument);
            assertEquals(PDRectangle.A4.getWidth(), actualPdfDocument.getPage(0).getMediaBox().getWidth());
            assertEquals(PDRectangle.A4.getHeight(), actualPdfDocument.getPage(0).getMediaBox().getHeight());
        }
    }

    //----------------------------------------------- Helper methods and classes -----------------------------------------------

    private static PDRectangle resolveExpectedPdfFormat(String pdfFormat)
    {
        switch (pdfFormat.toUpperCase()) {
        case "LETTER":
            return PDRectangle.LETTER;
        case "LEGAL":
            return PDRectangle.LEGAL;
        case "A0":
            return PDRectangle.A0;
        case "A1":
            return PDRectangle.A1;
        case "A2":
            return PDRectangle.A2;
        case "A3":
            return PDRectangle.A3;
        case "A5":
            return PDRectangle.A5;
        case "A6":
            return PDRectangle.A6;
        case "A4":
        default:
            return PDRectangle.A4;
        }
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

        private TransformOptions(Integer startPage, Integer endPage, String pdfFormat)
        {
            this.startPage = startPage;
            this.endPage = endPage;
            this.pdfFormat = pdfFormat;
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
            return transformOptions;
        }

        public static TransformOptions of(Integer startPage, Integer endPage)
        {
            return new TransformOptions(startPage, endPage, null);
        }

        public static TransformOptions of(String pdfFormat)
        {
            return new TransformOptions(null, null, pdfFormat);
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