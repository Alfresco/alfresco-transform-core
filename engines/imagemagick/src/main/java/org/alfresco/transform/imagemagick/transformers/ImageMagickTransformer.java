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
package org.alfresco.transform.imagemagick.transformers;

import static org.alfresco.transform.base.util.Util.stringToLong;
import static org.alfresco.transform.common.RequestParamMap.ALLOW_ENLARGEMENT;
import static org.alfresco.transform.common.RequestParamMap.ALPHA_REMOVE;
import static org.alfresco.transform.common.RequestParamMap.AUTO_ORIENT;
import static org.alfresco.transform.common.RequestParamMap.CROP_GRAVITY;
import static org.alfresco.transform.common.RequestParamMap.CROP_HEIGHT;
import static org.alfresco.transform.common.RequestParamMap.CROP_PERCENTAGE;
import static org.alfresco.transform.common.RequestParamMap.CROP_WIDTH;
import static org.alfresco.transform.common.RequestParamMap.CROP_X_OFFSET;
import static org.alfresco.transform.common.RequestParamMap.CROP_Y_OFFSET;
import static org.alfresco.transform.common.RequestParamMap.MAINTAIN_ASPECT_RATIO;
import static org.alfresco.transform.common.RequestParamMap.RESIZE_HEIGHT;
import static org.alfresco.transform.common.RequestParamMap.RESIZE_PERCENTAGE;
import static org.alfresco.transform.common.RequestParamMap.RESIZE_WIDTH;
import static org.alfresco.transform.common.RequestParamMap.THUMBNAIL;
import static org.alfresco.transform.common.RequestParamMap.TIMEOUT;

import java.io.File;
import java.util.Map;

import org.springframework.stereotype.Component;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transform.imagemagick.ImageMagickOptionsBuilder;
import org.alfresco.transform.imagemagick.transformers.page.PageRangeFactory;

/**
 * Converts image files into different types of images. Transformer supports multi-page images and allows to specify via parameters `startPage` and `endPage` range of pages that should be converted. In case of a one-page target image type (like `jpeg` or `png`) parameters `startPage` and `endPage` will be set to 0 by default - this means that only first page will be converted.
 */
@Component
public class ImageMagickTransformer implements CustomTransformerFileAdaptor
{
    private final ImageMagickCommandExecutor imageMagickCommandExecutor;
    private final PageRangeFactory pageRangeFactory;

    public ImageMagickTransformer(ImageMagickCommandExecutor imageMagickCommandExecutor, PageRangeFactory pageRangeFactory)
    {
        this.imageMagickCommandExecutor = imageMagickCommandExecutor;
        this.pageRangeFactory = pageRangeFactory;
    }

    @Override
    public String getTransformerName()
    {
        return "imagemagick";
    }

    @Override
    public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
            File sourceFile, File targetFile, TransformManager transformManager) throws TransformException
    {
        String options = ImageMagickOptionsBuilder
                .builder()
                .withAlphaRemove(transformOptions.get(ALPHA_REMOVE))
                .withAutoOrient(transformOptions.get(AUTO_ORIENT))
                .withCropGravity(transformOptions.get(CROP_GRAVITY))
                .withCropWidth(transformOptions.get(CROP_WIDTH))
                .withCropHeight(transformOptions.get(CROP_HEIGHT))
                .withCropPercentage(transformOptions.get(CROP_PERCENTAGE))
                .withCropXOffset(transformOptions.get(CROP_X_OFFSET))
                .withCropYOffset(transformOptions.get(CROP_Y_OFFSET))
                .withThumbnail(transformOptions.get(THUMBNAIL))
                .withResizeWidth(transformOptions.get(RESIZE_WIDTH))
                .withResizeHeight(transformOptions.get(RESIZE_HEIGHT))
                .withResizePercentage(transformOptions.get(RESIZE_PERCENTAGE))
                .withAllowEnlargement(transformOptions.get(ALLOW_ENLARGEMENT))
                .withMaintainAspectRatio(transformOptions.get(MAINTAIN_ASPECT_RATIO))
                .build();
        String pageRange = pageRangeFactory.create(sourceMimetype, targetMimetype, transformOptions);
        Long timeout = stringToLong(transformOptions.get(TIMEOUT));

        imageMagickCommandExecutor.run(options, sourceFile, pageRange, targetFile, timeout);
    }
}
