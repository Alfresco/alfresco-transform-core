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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@ConfigurationProperties(prefix = "transform.config")
public class TransformConfigFiles
{
    // Populated from Spring Boot properties or such as transform.config.file.<filename> or environment variables like
    // TRANSFORM_CONFIG_FILE_<filename>.
    private final Map<String, String> files = new HashMap<>();

    public Map<String, String> getFile()
    {
        return files;
    }

    public List<Resource> retrieveResources()
    {
        return TransformConfigFromFiles.retrieveResources(files);
    }
}
