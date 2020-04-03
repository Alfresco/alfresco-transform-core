/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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

import static org.alfresco.transformer.util.Util.stringToInteger;
import static org.alfresco.transformer.util.Util.stringToLong;

import java.io.File;
import java.util.Map;

import org.alfresco.transformer.ImageMagickOptionsBuilder;
import org.alfresco.transformer.executors.ImageMagickCommandExecutor;

public class ImageMagickAdapter extends AbstractTransformer 
{

    private static String CONFIG_PREFIX = "imagemagick";
    private ImageMagickCommandExecutor commandExecutor;

    //TODO move key strings to a central class
    private static final String START_PAGE              = "startPage";
    private static final String END_PAGE                = "endPage";
    private static final String ALPHA_REMOVE            = "alphaRemove";
    private static final String AUTO_ORIENT             = "autoOrient";
    private static final String CROP_GRAVITY            = "cropGravity";
    private static final String CROP_WIDTH              = "cropWidth";
    private static final String CROP_HEIGHT             = "cropHeight";
    private static final String CROP_PERCENTAGE         = "cropPercentage";
    private static final String CROP_X_OFFSET           = "cropXOffset";
    private static final String CROP_Y_OFFSET           = "cropYOffset";
    private static final String THUMBNAIL               = "thumbnail";
    private static final String RESIZE_WIDTH            = "resizeWidth";
    private static final String RESIZE_HEIGHT           = "resizeHeight";
    private static final String RESIZE_PERCENTAGE       = "resizePercentage";
    private static final String ALLOW_ENLARGEMENT       = "allowEnlargement";
    private static final String MAINTAIN_ASPECT_RATIO   = "maintainAspectRatio";
    private static final String COMMAND_OPTIONS         = "commandOptions";
    private static final String TIMEOUT_REQUEST_PARAM   = "timeOut";


    public ImageMagickAdapter() throws Exception 
    {
        super();
        commandExecutor = new ImageMagickCommandExecutor();
    }

    @Override
    String getTransformerConfigPrefix() 
    {
        return CONFIG_PREFIX;
    }

    @Override
    public void transform(File sourceFile, File targetFile, String sourceMimetype, String targetMimetype,
            Map<String, String> transformOptions) throws Exception 
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

        Long timeout = stringToLong(transformOptions.get(TIMEOUT_REQUEST_PARAM));

        commandExecutor.run(options, sourceFile, pageRange, targetFile, timeout);
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