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
package org.alfresco.transform.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.transform.config.TransformConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Reads {@link TransformConfig} from a {@json} file. Typically used by {@code TransformEngine.getTransformConfig()}.
 * <pre>
 *     transformConfigResourceReader.read("classpath:pdfrenderer_engine_config.json");
 * </pre>
 */
@Component
public class TransformConfigResourceReader
{
    @Autowired ResourceLoader resourceLoader;

    private ObjectMapper jsonObjectMapper = new ObjectMapper();

    public TransformConfig read(String engineConfigLocation)
    {
        Resource engineConfig = resourceLoader.getResource(engineConfigLocation);
        try (Reader reader = new InputStreamReader(engineConfig.getInputStream(), UTF_8))
        {
            TransformConfig transformConfig = jsonObjectMapper.readValue(reader, TransformConfig.class);
            return transformConfig;
        }
        catch (IOException e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR, "Could not read " + engineConfigLocation, e);
        }
    }
}
