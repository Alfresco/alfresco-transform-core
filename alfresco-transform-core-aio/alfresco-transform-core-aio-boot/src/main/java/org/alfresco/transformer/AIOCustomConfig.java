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
import org.alfresco.transformer.transformers.ImageMagickAdapter;
import org.alfresco.transformer.transformers.MiscAdapter;
import org.alfresco.transformer.transformers.PdfRendererAdapter;
import org.alfresco.transformer.transformers.TikaAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AIOCustomConfig
{
    /**
     *
     * @return Override the TransformRegistryImpl used in {@link AbstractTransformerController}
     */
    @Bean
    @Primary
    public TransformServiceRegistry aioTransformRegistry() throws Exception
    {
        System.out.println("****** Instanciating AIOTransformRegistry");
        AIOTransformRegistry aioTransformRegistry = new AIOTransformRegistry();
        aioTransformRegistry.registerTransformer(new MiscAdapter());
        aioTransformRegistry.registerTransformer(new TikaAdapter());
        aioTransformRegistry.registerTransformer(new ImageMagickAdapter());
        //aioTransformRegistry.registerTransformer(new LibreOfficeAdapter()); // TODO - why did this start throwing NPE - should we catch them?
        aioTransformRegistry.registerTransformer(new PdfRendererAdapter());
        return aioTransformRegistry;
    }

}
