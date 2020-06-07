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
package org.alfresco.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.transform.client.model.config.TransformConfig;
import org.alfresco.transform.client.model.config.TransformOption;
import org.alfresco.transform.client.registry.AbstractTransformRegistry;
import org.alfresco.transform.client.registry.TransformCache;
import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.executors.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * AIOTransformRegistry manages all of the sub transformers registered to it and provides aggregated TransformConfig.
 */
public class AIOTransformRegistry extends AbstractTransformRegistry
{
    private static final Logger log = LoggerFactory.getLogger(AIOTransformRegistry.class);

    private static final String ENGINE_CONFIG_LOCATION_POSTFIX = "_engine_config.json";

    private TransformConfig aggregatedConfig = new TransformConfig();

    // Holds the structures used by AbstractTransformRegistry to look up what is supported.
    // Unlike other sub classes this class does not extend Data or replace it at run time.
    private TransformCache data = new TransformCache();

    private ObjectMapper jsonObjectMapper = new ObjectMapper();

    // Represents the mapping between a transform and a transformer, multiple mappings can point to the same transformer.
    private Map<String, Transformer> transformerTransformMapping = new HashMap();

    /**
     * The registration will go through all supported sub transformers and map them to the transformer implementation.
     * @param transformer The transformer implementation, this could be a single transformer
     *                    or a transformer managing multiple sub transformers. The transformer's configuration file will
     *                    be read based on the {@link Transformer#getTransformerId()} value.
     * @throws Exception Exception is thrown if a mapping for a transformer name already exists.
     */
    public void registerTransformer(final Transformer transformer) throws Exception
    {
        // Load config for the transformer
        String location = getTransformConfigLocation(transformer);
        TransformConfig transformConfig = loadTransformConfig(location);

        // Map all of the transforms defined in the config to this Transformer implementation
        for (org.alfresco.transform.client.model.config.Transformer transformerConfig : transformConfig.getTransformers())
        {
            String transformerName = transformerConfig.getTransformerName();
            if (transformerTransformMapping.containsKey(transformerName))
            {
                throw new Exception("Transformer name " + transformerName + " is already registered.");
            }
            transformerTransformMapping.put(transformerName, transformer);
            log.debug("Registered transformer with name: '{}'.", transformerName);
        }

        // Add the new transformer configuration to the aggregate config
        aggregatedConfig.getTransformers().addAll(transformConfig.getTransformers());
        aggregatedConfig.getTransformOptions().putAll(transformConfig.getTransformOptions());
        registerAll(transformConfig, location, location);
    }

    /**
     *
     * @param transformName The transform name as it appears in TransformConfig.
     * @return The transformer implementation mapped to the transform name.
     */
    public Transformer getByTransformName(final String transformName)
    {
        return getTransformerTransformMapping().get(transformName);
    }

    /**
     *
     * @return The aggregated config of all the registered transformers
     */
    public TransformConfig getTransformConfig()
    {
        return aggregatedConfig;
    }

    protected String getTransformConfigLocation(final Transformer transformer)
    {
        String location = transformer.getTransformerId() + ENGINE_CONFIG_LOCATION_POSTFIX;
        return location;
    }

    protected TransformConfig loadTransformConfig(final String name) throws Exception
    {
        if (getClass().getClassLoader().getResource(name) == null)
        {
            throw new Exception("Configuration '" + name + "' does not exist on the classpath.");
        }

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(name);
             Reader reader = new InputStreamReader(is, UTF_8))
        {
            return jsonObjectMapper.readValue(reader, TransformConfig.class);
        }
        catch (IOException e)
        {
            throw new Exception("Could not read '" + name + "' from the classpath.", e);
        }
    }

    Map<String, Transformer> getTransformerTransformMapping()
    {
        return transformerTransformMapping;
    }

    void setTransformerTransformMapping(Map<String, Transformer> transformerTransformMapping)
    {
        this.transformerTransformMapping = transformerTransformMapping;
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
