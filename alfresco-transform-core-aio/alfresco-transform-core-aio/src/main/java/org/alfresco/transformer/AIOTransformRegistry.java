/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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
import org.alfresco.transform.client.registry.AbstractTransformRegistry;
import org.alfresco.transform.client.registry.CombinedTransformConfig;
import org.alfresco.transform.client.registry.TransformCache;
import org.alfresco.transformer.executors.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.alfresco.transform.client.model.config.CoreVersionDecorator.setCoreVersionOnSingleStepTransformers;

/**
 * AIOTransformRegistry manages all of the sub transformers registered to it and provides aggregated TransformConfig.
 */
public class AIOTransformRegistry extends AbstractTransformRegistry
{
    private static final Logger log = LoggerFactory.getLogger(AIOTransformRegistry.class);

    private static final String ENGINE_CONFIG_LOCATION_POSTFIX = "_engine_config.json";

    @Value("${transform.core.version}")
    private String coreVersion;

    private CombinedTransformConfig combinedTransformConfig = new CombinedTransformConfig();

    // Holds the structures used by AbstractTransformRegistry to look up what is supported.
    // Unlike other sub classes this class does not extend Data or replace it at run time.
    private TransformCache data = new TransformCache();

    private ObjectMapper jsonObjectMapper = new ObjectMapper();

    // Represents the mapping between a transform and a transformer, multiple mappings can point to the same transformer.
    private Map<String, Transformer> transformerEngineMapping = new HashMap();

    /**
     * Adds a transformer's (T-Engine) config to the configuration and creates a map of transforms to the T-Engine.
     * The name of this method is now misleading as the registry of transforms takes place in
     * {@link #registerCombinedTransformers()} .
     * @param tEngine The transformer implementation, this could be a single transformer
     *                    or a transformer managing multiple sub transformers. The transformer's configuration file will
     *                    be read based on the {@link Transformer#getTransformerId()} value.
     */
    public void registerTransformer(final Transformer tEngine) throws Exception
    {
        // Load config for the transformer
        String location = getTransformConfigLocation(tEngine);
        TransformConfig transformConfig = loadTransformConfig(location);
        setCoreVersionOnSingleStepTransformers(transformConfig.getTransformers(), coreVersion);
        String transformerId = tEngine.getTransformerId();
        combinedTransformConfig.addTransformConfig(transformConfig, location, transformerId, this);

        // Map all of the transforms defined in the config to this Transformer implementation
        for (org.alfresco.transform.client.model.config.Transformer transformerConfig : transformConfig.getTransformers())
        {
            String transformerName = transformerConfig.getTransformerName();
            // A later tEngine 'might' override one that has already been defined. That is fine.
            Transformer originalTEngine = transformerEngineMapping.get(transformerName);
            if (originalTEngine != null)
            {
                log.debug("Overriding transform with name: '{}' originally defined in '{}'.", transformerName, originalTEngine.getTransformerId());
            }
            transformerEngineMapping.put(transformerName, tEngine);
            log.debug("Registered transform with name: '{}' defined in '{}'.", transformerName, transformerId);
        }
    }

    public void registerCombinedTransformers()
    {
        combinedTransformConfig.combineTransformerConfig(this);
        combinedTransformConfig.registerCombinedTransformers(this);
    }

    /**
     *
     * @param transformName The transform name as it appears in TransformConfig.
     * @return The transformer implementation mapped to the transform name.
     */
    public Transformer getByTransformName(final String transformName)
    {
        return getTransformerEngineMapping().get(transformName);
    }

    /**
     *
     * @return The aggregated config of all the registered transformers
     */
    public TransformConfig getTransformConfig()
    {
        return combinedTransformConfig.buildTransformConfig();
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

    Map<String, Transformer> getTransformerEngineMapping()
    {
        return transformerEngineMapping;
    }

    void setTransformerEngineMapping(Map<String, Transformer> transformerEngineMapping)
    {
        this.transformerEngineMapping = transformerEngineMapping;
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

    @Override
    protected void logWarn(String msg)
    {
        log.warn(msg);
    }
}
