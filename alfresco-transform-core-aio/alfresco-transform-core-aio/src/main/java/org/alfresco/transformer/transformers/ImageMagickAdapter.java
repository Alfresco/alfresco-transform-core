/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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
package org.alfresco.transformer.transformers;

import static org.alfresco.transformer.util.RequestParamMap.ALLOW_ENLARGEMENT;
import static org.alfresco.transformer.util.RequestParamMap.ALPHA_REMOVE;
import static org.alfresco.transformer.util.RequestParamMap.AUTO_ORIENT;
import static org.alfresco.transformer.util.RequestParamMap.COMMAND_OPTIONS;
import static org.alfresco.transformer.util.RequestParamMap.CROP_GRAVITY;
import static org.alfresco.transformer.util.RequestParamMap.CROP_HEIGHT;
import static org.alfresco.transformer.util.RequestParamMap.CROP_PERCENTAGE;
import static org.alfresco.transformer.util.RequestParamMap.CROP_WIDTH;
import static org.alfresco.transformer.util.RequestParamMap.CROP_X_OFFSET;
import static org.alfresco.transformer.util.RequestParamMap.CROP_Y_OFFSET;
import static org.alfresco.transformer.util.RequestParamMap.END_PAGE;
import static org.alfresco.transformer.util.RequestParamMap.MAINTAIN_ASPECT_RATIO;
import static org.alfresco.transformer.util.RequestParamMap.RESIZE_HEIGHT;
import static org.alfresco.transformer.util.RequestParamMap.RESIZE_PERCENTAGE;
import static org.alfresco.transformer.util.RequestParamMap.RESIZE_WIDTH;
import static org.alfresco.transformer.util.RequestParamMap.START_PAGE;
import static org.alfresco.transformer.util.RequestParamMap.THUMBNAIL;
import static org.alfresco.transformer.util.RequestParamMap.TIMEOUT;
import static org.alfresco.transformer.util.Util.stringToInteger;
import static org.alfresco.transformer.util.Util.stringToLong;

import java.io.File;
import java.util.Map;

import org.alfresco.transformer.ImageMagickOptionsBuilder;
import org.alfresco.transformer.executors.ImageMagickCommandExecutor;

public class ImageMagickAdapter implements Transformer
{

    private static String ID = "imagemagick";
    private ImageMagickCommandExecutor commandExecutor;

    public ImageMagickAdapter(String exe, String dyn, String root, String coder, String config) throws Exception
    {
        commandExecutor = new ImageMagickCommandExecutor(exe, dyn, root, coder, config);
    }

    @Override
    public void transform(File sourceFile, File targetFile, String sourceMimetype, String targetMimetype,
            Map<String, String> transformOptions)
    {
        
        final String options = ImageMagickOptionsBuilder
            .builder()
            .withStartPage(transformOptions.get(START_PAGE))
            .withEndPage(transformOptions.get(END_PAGE))
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
            .withCommandOptions(transformOptions.get(COMMAND_OPTIONS))
            .build();

        String pageRange = calculatePageRange(
            stringToInteger(transformOptions.get(START_PAGE)),
            stringToInteger(transformOptions.get(END_PAGE))
        );

        Long timeout = stringToLong(transformOptions.get(TIMEOUT));

        commandExecutor.run(options, sourceFile, pageRange, targetFile, timeout);
    }

    @Override
    public String getTransformerId()
    {
        return ID;
    }

    // COPIED From ImageMagickController
    private static String calculatePageRange(Integer startPage, Integer endPage)
    {
        return startPage == null
               ? endPage == null
                 ? ""
                 : "[" + endPage + ']'
               : endPage == null || startPage.equals(endPage)
                 ? "[" + startPage + ']'
                 : "[" + startPage + '-' + endPage + ']';
    }
}
