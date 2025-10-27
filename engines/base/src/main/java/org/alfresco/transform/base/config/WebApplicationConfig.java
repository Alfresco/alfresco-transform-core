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
package org.alfresco.transform.base.config;

import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.alfresco.transform.config.CoreFunction.standardizeCoreVersion;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.alfresco.transform.base.html.TransformInterceptor;
import org.alfresco.transform.base.registry.TransformConfigSource;
import org.alfresco.transform.common.TransformerDebug;
import org.alfresco.transform.messages.TransformRequestValidator;

@Configuration
@ComponentScan(
        basePackages = {"org.alfresco.transform"},
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Test.*"))
public class WebApplicationConfig implements WebMvcConfigurer
{
    @Value("${transform.core.version}")
    private String coreVersionString;

    @Value("${container.isTRouter}")
    private boolean isTRouter;

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(new TransformInterceptor())
                .addPathPatterns(ENDPOINT_TRANSFORM, "/live", "/ready");
    }

    @Bean
    public TransformRequestValidator transformRequestValidator()
    {
        return new TransformRequestValidator();
    }

    @Bean
    public TransformerDebug transformerDebug()
    {
        return new TransformerDebug().setIsTRouter(isTRouter);
    }

    @Bean
    public String coreVersion()
    {
        return standardizeCoreVersion(coreVersionString);
    }

    @Bean
    public List<TransformConfigSource> transformConfigSources()
    {
        return new ArrayList<>();
    }
}
