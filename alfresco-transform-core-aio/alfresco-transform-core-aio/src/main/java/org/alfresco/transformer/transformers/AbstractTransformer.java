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
package org.alfresco.transformer.transformers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.alfresco.transform.client.model.config.TransformConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class AbstractTransformer implements Transformer
{
    private static final String TRANSFORMER_CONFIG_SUFFIX = "_engine_config.json";
    private ObjectMapper jsonObjectMapper;
    TransformConfig transformConfig;

    public AbstractTransformer() throws Exception
    {
        jsonObjectMapper = new JsonMapper();
        transformConfig = loadTransformConfig();
    }

    public void setObjectMapper(ObjectMapper objectMapper)
    {
        this.jsonObjectMapper = objectMapper;
    }

    public void setTransformConfig(TransformConfig transformConfig)
    {
        this.transformConfig = transformConfig;
    }

    /**
     * Used to search for an engine configuration file.
     *
     * @return A unique prefix which is used to load a &lt;prefix&gt; _engine_config.json file
     */
    abstract String getTransformerConfigPrefix();

    @Override
    public TransformConfig getTransformConfig()
    {
        return transformConfig;
    }

    /*
     * TODO - Override default config name by a configurable location defined by a property
     */
    private TransformConfig loadTransformConfig() throws Exception
    {
        String configFileName = getTransformerConfigPrefix() + TRANSFORMER_CONFIG_SUFFIX;

        if (getClass().getClassLoader().getResource(configFileName) == null)
        {
            throw new Exception("Configuration '" + configFileName + "' does not exist on the classpath.");
        }

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(configFileName);
             Reader reader = new InputStreamReader(is, UTF_8))
        {
            return jsonObjectMapper.readValue(reader, TransformConfig.class);
        }
        catch (IOException e)
        {
            throw new Exception("Could not read '" + configFileName + "' from the classpath.", e);
        }
    }
}
