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
package org.alfresco.transformer.config;

import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.alfresco.transform.common.TransformerDebug;
import org.alfresco.transform.messages.TransformRequestValidator;
import org.alfresco.transform.registry.TransformServiceRegistry;
import org.alfresco.transformer.TransformInterceptor;
import org.alfresco.transformer.TransformRegistryImpl;
import org.alfresco.transformer.clients.AlfrescoSharedFileStoreClient;

/**
 * @deprecated will be removed in a future release. Replaced by alfresco-base-t-engine.
 */
@Deprecated
@Configuration
public class WebApplicationConfig implements WebMvcConfigurer
{
    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry
                .addInterceptor(transformInterceptor())
                .addPathPatterns(ENDPOINT_TRANSFORM, "/live", "/ready");
    }

    @Bean
    public TransformInterceptor transformInterceptor()
    {
        return new TransformInterceptor();
    }

    @Bean
    public AlfrescoSharedFileStoreClient alfrescoSharedFileStoreClient()
    {
        return new AlfrescoSharedFileStoreClient();
    }

    @Bean
    public TransformRequestValidator transformRequestValidator()
    {
        return new TransformRequestValidator();
    }

    @Bean
    public TransformServiceRegistry transformRegistry()
    {
        return new TransformRegistryImpl();
    }

    @Bean
    public TransformerDebug transformerDebug()
    {
        return new TransformerDebug().setIsTRouter(false);
    }
}
