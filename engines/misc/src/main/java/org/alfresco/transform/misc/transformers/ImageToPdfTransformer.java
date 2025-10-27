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

import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.TIFF_TAG_XRESOLUTION;
import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.TIFF_TAG_YRESOLUTION;

import static org.alfresco.transform.common.RequestParamMap.END_PAGE;
import static org.alfresco.transform.common.RequestParamMap.PDF_FORMAT;
import static org.alfresco.transform.common.RequestParamMap.PDF_ORIENTATION;
import static org.alfresco.transform.common.RequestParamMap.START_PAGE;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;

/**
 * Converts image files into PDF files. Transformer uses PDF Box to perform conversions. During conversion image might be scaled down (keeping proportions) to match width or height of the PDF document. If the image is smaller than PDF page size, the image will be placed in the top left-hand side of the PDF document page. Transformer accepts bellow optional transform parameters: - startPage - page number of image (for multi-page images) from which transformer should start conversion. Default: first page of the image. - endPage - page number of image (for multi-page images) up to which transformation should be performed. Default: last page of the image. - pdfFormat - output PDF file format. Available formats: DEFAULT, A0, A1, A2, A3, A4, A5, A6, LETTER, LEGAL. Default: original image size. - pdfOrientation - output PDF file orientation. Available options: DEFAULT, PORTRAIT, LANDSCAPE. Default: original image orientation.
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
    private static final String DEFAULT_PDF_FORMAT_STRING = "DEFAULT"; // pdf format to use when no pdf format specified
    private static final String DEFAULT_PDF_ORIENTATION_STRING = "DEFAULT";
    private static final float PDFBOX_POINTS_PER_INCH = 72.0F;
    private static final List<String> DENY_LIST = List.of("com.github.jaiimageio.impl.plugins.tiff.TIFFImageReader");

    @Override
    public String getTransformerName()
    {
        return "imageToPdf";
    }

    @Override
    public void transform(
            String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
            File imageFile, File pdfFile, TransformManager transformManager) throws Exception
    {
        try (
                ImageInputStream imageInputStream = ImageIO.createImageInputStream(imageFile);
                PDDocument pdfDocument = new PDDocument())
        {
            final Integer startPage = parseOptionIfPresent(transformOptions, START_PAGE, Integer.class).orElse(null);
            final Integer endPage = parseOptionIfPresent(transformOptions, END_PAGE, Integer.class).orElse(null);
            final String pdfFormat = parseOptionIfPresent(transformOptions, PDF_FORMAT, String.class).orElse(DEFAULT_PDF_FORMAT_STRING);
            final String pdfOrientation = parseOptionIfPresent(transformOptions, PDF_ORIENTATION, String.class).orElse(DEFAULT_PDF_ORIENTATION_STRING);
            verifyOptions(startPage, endPage);

            final Map<String, Integer> resolution = determineImageResolution(imageFile);
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

                scaleAndDrawImage(pdfDocument, imageReader.read(i), pdfFormat, pdfOrientation, resolution);
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
        while (imageReaders.hasNext())
        {
            ImageReader reader = imageReaders.next();
            // Only process if the reader class is not in the deny list
            if (!DENY_LIST.contains(reader.getClass().getName()))
            {
                reader.setInput(imageInputStream);
                return reader;
            }
        }
        throw new IOException(String.format(INVALID_IMAGE_ERROR_MESSAGE, imageName, mimetype));
    }

    private void scaleAndDrawImage(final PDDocument pdfDocument, final BufferedImage bufferedImage, final String pdfFormat, final String pdfOrientation, final Map<String, Integer> resolution)
            throws IOException
    {
        final PDImageXObject image = LosslessFactory.createFromImage(pdfDocument, bufferedImage);

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        // if the image has a resolution which differs from pdfbox then adjust size in pixels according to pdfbox ppi
        if (resolution.get("X") > 0 && resolution.get("X") != PDFBOX_POINTS_PER_INCH &&
                resolution.get("Y") > 0 && resolution.get("Y") != PDFBOX_POINTS_PER_INCH)
        {
            imageWidth = (int) (((float) imageWidth / resolution.get("X")) * PDFBOX_POINTS_PER_INCH);
            imageHeight = (int) (((float) imageHeight / resolution.get("Y")) * PDFBOX_POINTS_PER_INCH);
        }

        final PDPage pdfPage = new PDPage(resolvePdfFormat(pdfFormat, pdfOrientation, imageWidth, imageHeight));
        pdfDocument.addPage(pdfPage);
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

    private PDRectangle resolvePdfFormat(final String pdfFormat, final String pdfOrientation, final int defaultWidth, final int defaultHeight)
    {
        PDRectangle pdRectangle;
        switch (pdfFormat.toUpperCase())
        {
        case "DEFAULT":
            pdRectangle = new PDRectangle(defaultWidth, defaultHeight);
            break;
        case "A4":
            pdRectangle = PDRectangle.A4;
            break;
        case "LETTER":
            pdRectangle = PDRectangle.LETTER;
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
        case "A5":
            pdRectangle = PDRectangle.A5;
            break;
        case "A6":
            pdRectangle = PDRectangle.A6;
            break;
        case "LEGAL":
            pdRectangle = PDRectangle.LEGAL;
            break;
        default:
            log.warn("PDF format: '{}' not supported. Maintaining the default one.", pdfFormat);
            pdRectangle = new PDRectangle(defaultWidth, defaultHeight);
            break;
        }

        switch (pdfOrientation.toUpperCase())
        {
        case "DEFAULT":
            break;
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
        default:
            log.warn("PDF orientation: '{}' not supported. Maintaining the default one.", pdfOrientation);
            break;
        }

        return pdRectangle;
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

    private static Map<String, Integer> determineImageResolution(File imageFile)
    {
        int xResolution = 0;
        int yResolution = 0;

        try
        {
            final ImageMetadata metadata = Imaging.getMetadata(imageFile);
            if (metadata instanceof TiffImageMetadata)
            {
                final TiffImageMetadata tiffImageMetadata = (TiffImageMetadata) metadata;
                xResolution = findMetadataField(tiffImageMetadata, TIFF_TAG_XRESOLUTION);
                yResolution = findMetadataField(tiffImageMetadata, TIFF_TAG_YRESOLUTION);
            }
        }
        catch (IOException e)
        {
            // treat as though no resolution exists
        }
        return Map.of("X", xResolution, "Y", yResolution);
    }

    static private int findMetadataField(TiffImageMetadata tiffImageMetadata, TagInfo tagInfo)
    {
        int value = 0;
        try
        {
            TiffField field = tiffImageMetadata.findField(tagInfo);
            if (field != null)
            {
                value = field.getIntValue();
            }
        }
        catch (ImagingException e)
        {
            // treat as though field not found
        }
        return value;
    }
}
