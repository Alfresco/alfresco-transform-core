/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2023 Alfresco Software Limited
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

import static org.alfresco.transform.base.util.Util.stringToInteger;
import static org.alfresco.transform.base.util.Util.stringToLong;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_BMP;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JP2;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_XWD;
import static org.alfresco.transform.common.RequestParamMap.ALLOW_ENLARGEMENT;
import static org.alfresco.transform.common.RequestParamMap.ALPHA_REMOVE;
import static org.alfresco.transform.common.RequestParamMap.AUTO_ORIENT;
import static org.alfresco.transform.common.RequestParamMap.COMMAND_OPTIONS;
import static org.alfresco.transform.common.RequestParamMap.CROP_GRAVITY;
import static org.alfresco.transform.common.RequestParamMap.CROP_HEIGHT;
import static org.alfresco.transform.common.RequestParamMap.CROP_PERCENTAGE;
import static org.alfresco.transform.common.RequestParamMap.CROP_WIDTH;
import static org.alfresco.transform.common.RequestParamMap.CROP_X_OFFSET;
import static org.alfresco.transform.common.RequestParamMap.CROP_Y_OFFSET;
import static org.alfresco.transform.common.RequestParamMap.END_PAGE;
import static org.alfresco.transform.common.RequestParamMap.MAINTAIN_ASPECT_RATIO;
import static org.alfresco.transform.common.RequestParamMap.RESIZE_HEIGHT;
import static org.alfresco.transform.common.RequestParamMap.RESIZE_PERCENTAGE;
import static org.alfresco.transform.common.RequestParamMap.RESIZE_WIDTH;
import static org.alfresco.transform.common.RequestParamMap.START_PAGE;
import static org.alfresco.transform.common.RequestParamMap.THUMBNAIL;
import static org.alfresco.transform.common.RequestParamMap.TIMEOUT;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.executors.AbstractCommandExecutor;
import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transform.imagemagick.ImageMagickOptionsBuilder;

/**
 * Converts image files into different types of images. Transformer supports multi-page images and allows to specify via parameters `startPage` and `endPage` range of pages that should be converted. In case of a one-page target image type (like `jpeg` or `png`) parameters `startPage` and `endPage` will be set to 0 by default - this means that only first page will be converted.
 */
@Component
public class ImageMagickTransformer extends AbstractCommandExecutor implements CustomTransformerFileAdaptor
{
    private final List<String> singlePageFormats = List.of(MIMETYPE_IMAGE_BMP, MIMETYPE_IMAGE_JP2, MIMETYPE_IMAGE_JPEG, MIMETYPE_IMAGE_PNG, MIMETYPE_IMAGE_XWD);

    @Value("${transform.core.imagemagick.exe}")
    private String exe;
    @Value("${transform.core.imagemagick.dyn}")
    private String dyn;
    @Value("${transform.core.imagemagick.root}")
    private String root;

    // Not currently used, but may be again in the future if we need an ImageMagick extension
    @Value("${transform.core.imagemagick.coders}")
    private String coders;
    @Value("${transform.core.imagemagick.config}")
    private String config;

    @PostConstruct
    private void createCommands()
    {
        if (exe == null || exe.isEmpty())
        {
            throw new IllegalArgumentException("ImageMagickTransformer IMAGEMAGICK_EXE variable cannot be null or empty");
        }
        if (dyn == null || dyn.isEmpty())
        {
            throw new IllegalArgumentException("ImageMagickTransformer IMAGEMAGICK_DYN variable cannot be null or empty");
        }
        if (root == null || root.isEmpty())
        {
            throw new IllegalArgumentException("ImageMagickTransformer IMAGEMAGICK_ROOT variable cannot be null or empty");
        }

        super.transformCommand = createTransformCommand();
        super.checkCommand = createCheckCommand();
    }

    @Override
    public String getTransformerName()
    {
        return "imagemagick";
    }

    @Override
    protected RuntimeExec createTransformCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*",
                new String[]{exe, "${source}", "SPLIT:${options}", "-strip", "-quiet", "${target}"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);

        Map<String, String> processProperties = new HashMap<>();
        processProperties.put("MAGICK_HOME", root);
        processProperties.put("DYLD_FALLBACK_LIBRARY_PATH", dyn);
        processProperties.put("LD_LIBRARY_PATH", dyn);

        // Optional properties (see also https://imagemagick.org/script/resources.php#environment)
        if (coders != null && !coders.isBlank())
        {
            processProperties.put("MAGICK_CODER_MODULE_PATH", coders);
        }
        if (config != null && !config.isBlank())
        {
            processProperties.put("MAGICK_CONFIGURE_PATH", config);
        }
        runtimeExec.setProcessProperties(processProperties);

        Map<String, String> defaultProperties = new HashMap<>();
        defaultProperties.put("options", null);
        runtimeExec.setDefaultProperties(defaultProperties);

        runtimeExec.setErrorCodes(
                "1,2,255,400,405,410,415,420,425,430,435,440,450,455,460,465,470,475,480,485,490,495,499,700,705,710,715,720,725,730,735,740,750,755,760,765,770,775,780,785,790,795,799");

        return runtimeExec;
    }

    @Override
    protected RuntimeExec createCheckCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*", new String[]{exe, "-version"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);
        return runtimeExec;
    }

    @Override
    public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
            File sourceFile, File targetFile, TransformManager transformManager) throws TransformException
    {
        String startPageString = transformOptions.get(START_PAGE);
        String endPageString = transformOptions.get(END_PAGE);
        if (!singlePageFormats.contains(sourceMimetype) && singlePageFormats.contains(targetMimetype))
        {
            if (StringUtils.isEmpty(startPageString))
            {
                startPageString = "0";
            }
            if (StringUtils.isEmpty(endPageString))
            {
                endPageString = startPageString;
            }
        }

        final String options = ImageMagickOptionsBuilder
                .builder()
                .withStartPage(startPageString)
                .withEndPage(endPageString)
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
                stringToInteger(startPageString),
                stringToInteger(endPageString));

        Long timeout = stringToLong(transformOptions.get(TIMEOUT));

        run(options, sourceFile, pageRange, targetFile, timeout);
    }

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
