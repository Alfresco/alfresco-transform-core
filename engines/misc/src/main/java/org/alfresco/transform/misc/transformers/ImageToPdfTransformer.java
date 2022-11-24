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

import static org.alfresco.transform.common.RequestParamMap.END_PAGE;
import static org.alfresco.transform.common.RequestParamMap.PDF_FORMAT;
import static org.alfresco.transform.common.RequestParamMap.START_PAGE;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Converts image files into PDF files. Transformer uses PDF Box to perform conversions.
 * During conversion image might be scaled down (keeping proportions) to match width or height of the PDF document.
 * If the image is smaller than PDF page size, the image will be placed in the top left-hand side of the PDF document page.
 * Transformer takes 3 optional transform options:
 * - startPage - page number of image (for multipage images) from which transformer should start conversion. Default: first page of the image.
 * - endPage - page number of image (for multipage images) up to which transformation should be performed. Default: last page of the image.
 * - pdfFormat - output PDF file format. Available formats: A0, A1, A2, A3, A4, A5, A6, LETTER, LEGAL. Default: A4.
 */
@Component
public class ImageToPdfTransformer implements CustomTransformerFileAdaptor
{
    private static final Logger log = LoggerFactory.getLogger(ImageToPdfTransformer.class);

    private static final String NEGATIVE_START_PAGE_ERROR_MESSAGE = "Start page number cannot be a negative number.";
    private static final String NEGATIVE_END_PAGE_ERROR_MESSAGE = "End page number cannot be a negative number.";
    private static final String START_PAGE_GREATER_THAN_END_PAGE_ERROR_MESSAGE = "Start page number cannot be greater than end page.";
    private static final String INVALID_OPTION_ERROR_MESSAGE = "Parameter '%s' is invalid: \"%s\" - it must be an integer.";
    private static final String INVALID_IMAGE_ERROR_MESSAGE = "Image file (%s) format (%s) not supported by ImageIO.";
    private static final String DEFAULT_PDF_FORMAT_STRING = "A4";
    private static final PDRectangle DEFAULT_PDF_FORMAT = PDRectangle.A4;

    @Override
    public String getTransformerName()
    {
        return "imageToPdf";
    }

    @Override
    public void transform(
        String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
        File imageFile, File pdfFile, TransformManager transformManager
    ) throws Exception {
        try (
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(imageFile);
            PDDocument pdfDocument = new PDDocument()
        ) {
            final Integer startPage = parseOptionIfPresent(transformOptions, START_PAGE, Integer.class).orElse(null);
            final Integer endPage = parseOptionIfPresent(transformOptions, END_PAGE, Integer.class).orElse(null);
            final String pdfFormat = parseOptionIfPresent(transformOptions, PDF_FORMAT, String.class).orElse(DEFAULT_PDF_FORMAT_STRING);
            verifyOptions(startPage, endPage);

            final ImageReader imageReader = findImageReader(imageInputStream, imageFile.getName(), sourceMimetype);
            for (int i = 0; i < imageReader.getNumImages(true); i++)
            {
                if (startPage != null && i < startPage)
                {
                    continue;
                }
                if (endPage != null && i > endPage)
                {
                    break;
                }

                scaleAndDrawImage(pdfDocument, imageReader.read(i), pdfFormat);
            }

            pdfDocument.save(pdfFile);
        }
    }

    private ImageReader findImageReader(final ImageInputStream imageInputStream, final String imageName, final String mimetype) throws IOException
    {
        final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
        if (imageReaders == null || !imageReaders.hasNext())
        {
            throw new IOException(String.format(INVALID_IMAGE_ERROR_MESSAGE, imageName, mimetype));
        }
        final ImageReader imageReader = imageReaders.next();
        imageReader.setInput(imageInputStream);

        return imageReader;
    }

    private void scaleAndDrawImage(final PDDocument pdfDocument, final BufferedImage bufferedImage, final String pdfFormat) throws IOException
    {
        final PDPage pdfPage = new PDPage(resolvePdfFormat(pdfFormat));
        pdfDocument.addPage(pdfPage);
        final PDImageXObject image = LosslessFactory.createFromImage(pdfDocument, bufferedImage);
        try (PDPageContentStream pdfPageContent = new PDPageContentStream(pdfDocument, pdfPage))
        {
            final PDRectangle pageSize = pdfPage.getMediaBox();
            final float widthRatio = image.getWidth() > 0 ? pageSize.getWidth() / image.getWidth() : 0;
            final float heightRatio = image.getHeight() > 0 ? pageSize.getHeight() / image.getHeight() : 0;
            final float ratio = Stream.of(widthRatio, heightRatio, 1f).min(Comparator.naturalOrder()).get();
            // find image bottom
            final float y = pageSize.getHeight() - image.getHeight() * ratio;
            // drawing starts from bottom left corner
            pdfPageContent.drawImage(image, 0, y, image.getWidth() * ratio, image.getHeight() * ratio);
        }
    }

    private PDRectangle resolvePdfFormat(final String pdfFormat)
    {
        switch (pdfFormat.toUpperCase()) {
        case "A4":
            return DEFAULT_PDF_FORMAT;
        case "LETTER":
            return PDRectangle.LETTER;
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
        case "LEGAL":
            return PDRectangle.LEGAL;
        default:
            log.info("PDF format: '{}' not supported. Using default: '{}'", pdfFormat, DEFAULT_PDF_FORMAT_STRING);
            return DEFAULT_PDF_FORMAT;
        }
    }

    private static <T> Optional<T> parseOptionIfPresent(final Map<String, String> transformOptions, final String parameter, final Class<T> targetType)
    {
        if (transformOptions.containsKey(parameter))
        {
            final String option = transformOptions.get(parameter);
            if (targetType == Integer.class)
            {
                try
                {
                    return Optional.of(targetType.cast(Integer.parseInt(option)));
                }
                catch (NumberFormatException e)
                {
                    throw new IllegalArgumentException(String.format(INVALID_OPTION_ERROR_MESSAGE, parameter, option));
                }
            }
            else
            {
                return Optional.of(targetType.cast(option));
            }
        }

        return Optional.empty();
    }

    private static void verifyOptions(final Integer startPage, final Integer endPage)
    {
        if (startPage != null && startPage < 0)
        {
            throw new IllegalArgumentException(NEGATIVE_START_PAGE_ERROR_MESSAGE);
        }

        if (endPage != null && endPage < 0)
        {
            throw new IllegalArgumentException(NEGATIVE_END_PAGE_ERROR_MESSAGE);
        }

        if (startPage != null && endPage != null && startPage > endPage)
        {
            throw new IllegalArgumentException(START_PAGE_GREATER_THAN_END_PAGE_ERROR_MESSAGE);
        }
    }
}
