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
package org.alfresco.transformer;

import org.alfresco.transform.client.registry.TransformServiceRegistry;
import org.alfresco.transformer.executors.ImageMagickCommandExecutor;
import org.alfresco.transformer.executors.LibreOfficeJavaExecutor;
import org.alfresco.transformer.executors.PdfRendererCommandExecutor;
import org.alfresco.transformer.executors.TikaJavaExecutor;
import org.alfresco.transformer.transformers.SelectingTransformer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AIOCustomConfig
{
    @Value("${transform.core.libreoffice.path}")
    private String libreofficePath;

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

    /**
     *
     * @return Override the TransformRegistryImpl used in {@link AbstractTransformerController}
     */
    @Bean
    @Primary
    public TransformServiceRegistry aioTransformRegistry() throws Exception
    {
        AIOTransformRegistry aioTransformRegistry = new AIOTransformRegistry();
        aioTransformRegistry.registerTransformer(new SelectingTransformer());
        aioTransformRegistry.registerTransformer(new TikaJavaExecutor());
        aioTransformRegistry.registerTransformer(new ImageMagickCommandExecutor(imageMagickExePath, imageMagickDynPath, imageMagickRootPath, imageMagickCodersPath, imageMagickConfigPath));
        aioTransformRegistry.registerTransformer(new LibreOfficeJavaExecutor(libreofficePath));
        aioTransformRegistry.registerTransformer(new PdfRendererCommandExecutor(pdfRendererPath));
        return aioTransformRegistry;
    }
}
