/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.base.registry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.alfresco.transform.base.registry.TransformConfigFromFiles.retrieveResource;

/**
 * Similar to {@link TransformConfigFiles} but uses the names historically used by the t-router.
 */
@Configuration
@ConfigurationProperties(prefix = "transformer.routes")
public class TransformConfigFilesHistoric
{
    // Populated with file paths from Spring Boot properties such as transformer.routes.additional.<engineName> or
    // environment variables like TRANSFORMER_ROUTES_ADDITIONAL_<engineName>.
    private final Map<String, String> additional = new HashMap<>();

    private String TRANSFORMER_ROUTES_FROM_CLASSPATH = "transformer-pipelines.json";

    @Value("${transformer-routes-path}")
    private String transformerRoutesExternalFile;

    public List<Resource> retrieveResources()
    {
        ArrayList<Resource> resources = new ArrayList<>();
        addStandardConfigIfItExists(resources);
        resources.addAll(TransformConfigFromFiles.retrieveResources(additional));
        return resources;
    }

    private void addStandardConfigIfItExists(ArrayList<Resource> resources)
    {
        Resource resource = null;
        if (transformerRoutesExternalFile != null && !transformerRoutesExternalFile.isBlank())
        {
            resource = retrieveResource(transformerRoutesExternalFile);
        }

        if (resource == null || !resource.exists())
        {
            resource = new ClassPathResource(TRANSFORMER_ROUTES_FROM_CLASSPATH);
        }

        if (resource.exists())
        {
            resources.add(resource);
        }
    }
}
