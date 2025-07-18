/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2015 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.config.reader;

import static java.util.Objects.isNull;

import org.springframework.core.io.Resource;

public class TransformConfigReaderFactory
{
    private TransformConfigReaderFactory()
    {}

    public static TransformConfigReader create(final Resource resource)
    {
        final String fileName = resource.getFilename();

        if (isNull(fileName) || !fileName.contains("."))
        {
            throw new RuntimeException("Invalid configuration file: " + fileName);
        }
        final String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        switch (extension)
        {
        case "properties":
            throw new UnsupportedOperationException(".properties configuration files are no longer " +
                    "supported: " + fileName);
        case "yaml":
        case "yml":
            return new TransformConfigReaderYaml(resource);
        case "json":
            return new TransformConfigReaderJson(resource);
        default:
            throw new RuntimeException("Unknown configuration file type: " + fileName);
        }
    }
}
