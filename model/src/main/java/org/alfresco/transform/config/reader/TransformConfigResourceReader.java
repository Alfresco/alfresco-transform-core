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
package org.alfresco.transform.config.reader;

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transform.config.TransformConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Reads {@link TransformConfig} from json or yaml files. Typically used by {@code TransformEngine.getTransformConfig()}.
 * <pre>
 *     transformConfigResourceReader.read("classpath:pdfrenderer_engine_config.json");
 * </pre>
 */
@Component
public class TransformConfigResourceReader
{
    @Autowired ResourceLoader resourceLoader;

    public TransformConfig read(String resourcePath)
    {
        return read(resourceLoader.getResource(resourcePath));
    }

    public TransformConfig read(Resource resource)
    {
        try
        {
            return TransformConfigReaderFactory.create(resource).load();
        }
        catch (IOException e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR, "Could not read " + resource.getFilename(), e);
        }
    }
}
