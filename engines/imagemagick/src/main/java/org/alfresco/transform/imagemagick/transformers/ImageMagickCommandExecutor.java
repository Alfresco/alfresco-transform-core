/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2025 Alfresco Software Limited
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

import java.util.HashMap;
import java.util.Map;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.alfresco.transform.base.executors.AbstractCommandExecutor;
import org.alfresco.transform.base.executors.RuntimeExec;

@Component
public class ImageMagickCommandExecutor extends AbstractCommandExecutor
{
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
}
