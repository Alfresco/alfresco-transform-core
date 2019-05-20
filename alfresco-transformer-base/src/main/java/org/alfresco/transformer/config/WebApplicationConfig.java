/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package org.alfresco.transformer.config;

import org.alfresco.transformer.TransformInterceptor;
import org.alfresco.transformer.clients.AlfrescoSharedFileStoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.alfresco.transform.client.model.TransformRequestValidator;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebApplicationConfig implements WebMvcConfigurer
{

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(transformInterceptor()).addPathPatterns("/transform", "/live", "/ready");
    }

    @Bean
    public TransformInterceptor transformInterceptor() {
        return new TransformInterceptor();
    }


    @Bean
    public RestTemplate restTemplate()
    {
        return new RestTemplate();
    }

    @Bean
    public AlfrescoSharedFileStoreClient alfrescoSharedFileStoreClient(){
        return new AlfrescoSharedFileStoreClient();
    }

    @Bean
    public TransformRequestValidator transformRequestValidator()
    {
        return new TransformRequestValidator();
    }
}
