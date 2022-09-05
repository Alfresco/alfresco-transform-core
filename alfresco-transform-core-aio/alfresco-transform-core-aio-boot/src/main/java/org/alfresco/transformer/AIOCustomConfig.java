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
package org.alfresco.transformer;

import org.alfresco.transform.client.registry.TransformServiceRegistry;
import org.alfresco.transformer.executors.ImageMagickCommandExecutor;
import org.alfresco.transformer.executors.LibreOfficeJavaExecutor;
import org.alfresco.transformer.executors.PdfRendererCommandExecutor;
import org.alfresco.transformer.executors.RuntimeExec;
import org.alfresco.transformer.executors.TikaJavaExecutor;
import org.alfresco.transformer.executors.Transformer;
import org.alfresco.transformer.transformers.SelectingTransformer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class AIOCustomConfig
{
    @Value("${transform.core.libreoffice.path}")
    private String libreofficePath;

    @Value("${transform.core.libreoffice.maxTasksPerProcess}")
    private String libreofficeMaxTasksPerProcess;

    @Value("${transform.core.libreoffice.timeout}")
    private String libreofficeTimeout;

    @Value("${transform.core.libreoffice.portNumbers}")
    private String libreofficePortNumbers;

    @Value("${transform.core.libreoffice.templateProfileDir}")
    private String libreofficeTemplateProfileDir;

    @Value("${transform.core.libreoffice.isEnabled}")
    private String libreofficeIsEnabled;

    @Value("${transform.core.pdfrenderer.exe}")
    private String pdfRendererPath;

    @Value("${transform.core.imagemagick.exe}")
    private String imageMagickExePath;

    @Value("${transform.core.imagemagick.dyn}")
    private String imageMagickDynPath;

    @Value("${transform.core.imagemagick.root}")
    private String imageMagickRootPath;

    @Value("${transform.core.imagemagick.coders}")
    private String imageMagickCodersPath;

    @Value("${transform.core.imagemagick.config}")
    private String imageMagickConfigPath;

    @Value("${transform.core.tika.pdfBox.notExtractBookmarksTextDefault:false}")
    private boolean notExtractBookmarksTextDefault;

    @Value("${transform.core.version}")
    private String coreVersion;

    @Value("${transform.core.tika.exifTool.windowsOS}")
    private String exifToolCommandOnWindows;

    @Value("${transform.core.tika.exifTool.unixOS}")
    private String exifToolCommandOnUnix;

    @Bean("exifTool")
    public RuntimeExec exifRuntimeExec() {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String,String[]> commandPerOS = new HashMap<>();
        commandPerOS.put("[wW]in.*", exifToolCommandOnWindows.split(" "));
        commandPerOS.put("*", exifToolCommandOnUnix.split(" "));
        runtimeExec.setCommandsAndArguments(commandPerOS);

        return runtimeExec;
    }


    /**
     *
     * @return Override the TransformRegistryImpl used in {@link AbstractTransformerController}
     */
    @Bean
    @Primary
    public TransformServiceRegistry aioTransformRegistry() throws Exception
    {
        AIOTransformRegistry aioTransformRegistry = new AIOTransformRegistry();
        aioTransformRegistry.setCoreVersion(coreVersion);

        // T-Engines are sorted by name so they are combined in the same order as in the T-Router
        // and Content Repository with individual T-Engines. See TransformersConfigRegistry#retrieveRemoteConfig and
        // LocalTransformServiceRegistry#getTEngineUrlsSortedByName.
        for (Transformer tEngine : getTEnginesSortedByName())
        {
            aioTransformRegistry.registerTransformer(tEngine); // now a poor name - should be combineTransformers
        }
        aioTransformRegistry.registerCombinedTransformers();
        return aioTransformRegistry;
    }

    List<Transformer> getTEnginesSortedByName()
    {
        return Stream.of(new SelectingTransformer(),
                new TikaJavaExecutor(notExtractBookmarksTextDefault, exifRuntimeExec()),
                new ImageMagickCommandExecutor(imageMagickExePath, imageMagickDynPath, imageMagickRootPath, imageMagickCodersPath, imageMagickConfigPath),
                new LibreOfficeJavaExecutor(libreofficePath, libreofficeMaxTasksPerProcess, libreofficeTimeout, libreofficePortNumbers, libreofficeTemplateProfileDir, libreofficeIsEnabled),
                new PdfRendererCommandExecutor(pdfRendererPath))
                .sorted(Comparator.comparing(Transformer::getTransformerId))
                .collect(Collectors.toList());
    }
}
