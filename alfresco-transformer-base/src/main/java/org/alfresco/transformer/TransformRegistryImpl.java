/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.transform.client.model.config.TransformConfig;
import org.alfresco.transform.client.registry.AbstractTransformRegistry;
import org.alfresco.transform.client.registry.TransformCache;
import org.alfresco.transform.exceptions.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Used by clients to work out if a transformation is supported based on the engine_config.json.
 */
public class TransformRegistryImpl extends AbstractTransformRegistry
{
    private static final Logger log = LoggerFactory.getLogger(TransformRegistryImpl.class);

    private static final String ENGINE_CONFIG_JSON = "classpath:engine_config.json";

    @Value(ENGINE_CONFIG_JSON)
    private Resource engineConfig;

    // Holds the structures used by AbstractTransformRegistry to look up what is supported.
    // Unlike other sub classes this class does not extend Data or replace it at run time.
    private TransformCache data = new TransformCache();

    private ObjectMapper jsonObjectMapper = new ObjectMapper();

    TransformConfig getTransformConfig()
    {
        try (Reader reader = new InputStreamReader(engineConfig.getInputStream(), UTF_8))
        {
            return jsonObjectMapper.readValue(reader, TransformConfig.class);
        }
        catch (IOException e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                    "Could not read "+ ENGINE_CONFIG_JSON, e);
        }
    }

    @PostConstruct
    public void afterPropertiesSet()
    {
        TransformConfig transformConfig = getTransformConfig();
        register(transformConfig, null, ENGINE_CONFIG_JSON);
    }

    @Override
    public TransformCache getData()
    {
        return data;
    }

    @Override
    protected void logError(String msg)
    {
        log.error(msg);
    }
}
