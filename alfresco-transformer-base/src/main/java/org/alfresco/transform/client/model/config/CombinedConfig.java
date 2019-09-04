/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2019 Alfresco Software Limited
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
package org.alfresco.transform.client.model.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class recreates the json format used in ACS 6.1 where we just had an array of transformers and each
 * transformer has a list of node options. The idea of this code is that it replaces the references with the
 * actual node options that have been separated out into their own section.<p>
 *
 * The T-Router and T-Engines return the format with the node option separated into their own section. Pipeline
 * definitions used by the LocalTransformServiceRegistry may use node reference options defined in the json
 * returned by T-Engines.  with the actual definitions from the node options
 * reference section. It also combines multiple json sources into a single jsonNode structure that can be parsed as
 * before.
 */
public class CombinedConfig
{
    private static final String TRANSFORMER_NAME = "transformerName";
    private static final String TRANSFORM_CONFIG = "/transform/config";
    private static final String TRANSFORM_OPTIONS = "transformOptions";
    private static final String GROUP = "group";
    private static final String TRANSFORMERS = "transformers";

    private final Logger log;
    private Map<String, ArrayNode> allTransformOptions = new HashMap<>();
    private List<TransformNodeAndItsOrigin> allTransforms = new ArrayList<>();
    private ObjectMapper jsonObjectMapper = new ObjectMapper();
    private ConfigFileFinder configFileFinder;
    private int tEngineCount;

    static class TransformNodeAndItsOrigin
    {
        final ObjectNode node;
        final String baseUrl;
        final String readFrom;

        TransformNodeAndItsOrigin(ObjectNode node, String baseUrl, String readFrom)
        {
            this.node = node;
            this.baseUrl = baseUrl;
            this.readFrom = readFrom;
        }
    }

    static class TransformAndItsOrigin
    {
        final InlineTransformer transform;
        final String baseUrl;
        final String readFrom;

        TransformAndItsOrigin(InlineTransformer transform, String baseUrl, String readFrom)
        {
            this.transform = transform;
            this.baseUrl = baseUrl;
            this.readFrom = readFrom;
        }
    }

    public CombinedConfig(Logger log)
    {
        this.log = log;

        configFileFinder = new ConfigFileFinder(jsonObjectMapper)
        {
            @Override
            protected void readJson(JsonNode jsonNode, String readFromMessage, String baseUrl) throws IOException
            {
                JsonNode transformOptions = jsonNode.get(TRANSFORM_OPTIONS);
                if (transformOptions != null && transformOptions.isObject())
                {
                    Iterator<Map.Entry<String, JsonNode>> iterator = transformOptions.fields();
                    while (iterator.hasNext())
                    {
                        Map.Entry<String, JsonNode> entry = iterator.next();

                        JsonNode options = entry.getValue();
                        if (options.isArray())
                        {
                            String optionsName = entry.getKey();
                            allTransformOptions.put(optionsName, (ArrayNode)options);
                        }
                    }
                }

                JsonNode transformers = jsonNode.get(TRANSFORMERS);
                if (transformers != null && transformers.isArray())
                {
                    for (JsonNode transformer : transformers)
                    {
                        if (transformer.isObject())
                        {
                            allTransforms.add(new TransformNodeAndItsOrigin((ObjectNode)transformer, baseUrl, readFromMessage));
                        }
                    }
                }
            }
        };
    }

    public boolean addLocalConfig(String path) throws IOException
    {
        return configFileFinder.readFiles(path, log);
    }

    public void register(TransformRegistry registry) throws IOException
    {
        List<TransformAndItsOrigin> transformers = getTransforms();
        transformers.forEach(t->registry.register(t.transform));
    }

    public List<TransformAndItsOrigin> getTransforms() throws IOException
    {
        List<TransformAndItsOrigin> transforms = new ArrayList<>();

        // After all json input has been loaded build the output with the options in place.
        ArrayNode transformersNode = jsonObjectMapper.createArrayNode();
        for (TransformNodeAndItsOrigin entity : allTransforms)
        {
            transformersNode.add(entity.node);

            try
            {
                ArrayNode transformOptions = (ArrayNode) entity.node.get(TRANSFORM_OPTIONS);
                if (transformOptions != null)
                {

                    ArrayNode options;
                    int size = transformOptions.size();
                    if (size == 1)
                    {
                        // If there is a single node option reference, we can just use it.
                        int i = 0;
                        options = getTransformOptions(transformOptions, i, entity.node);
                    }
                    else
                    {
                        // If there are many node option references (typically in a pipeline), then each element
                        // has a group for each set of node options.
                        options = jsonObjectMapper.createArrayNode();
                        for (int i = size - 1; i >= 0; i--)
                        {
                            JsonNode referencedTransformOptions = getTransformOptions(transformOptions, i, entity.node);
                            if (referencedTransformOptions != null)
                            {
                                ObjectNode element = jsonObjectMapper.createObjectNode();
                                options.add(element);

                                ObjectNode group = jsonObjectMapper.createObjectNode();
                                group.set(TRANSFORM_OPTIONS, referencedTransformOptions);
                                element.set(GROUP, group);
                            }
                        }
                    }
                    if (options == null || options.size() == 0)
                    {
                        entity.node.remove(TRANSFORM_OPTIONS);
                    }
                    else
                    {
                        entity.node.set(TRANSFORM_OPTIONS, options);
                    }
                }

                try
                {
                    InlineTransformer transform = jsonObjectMapper.convertValue(entity.node, InlineTransformer.class);
                    transforms.add(new TransformAndItsOrigin(transform, entity.baseUrl, entity.readFrom));
                }
                catch (IllegalArgumentException e)
                {
                    log.error("Invalid transformer "+getTransformName(entity.node)+" "+e.getMessage()+" baseUrl="+entity.baseUrl);
                }
            }
            catch (IllegalArgumentException e)
            {
                String transformString = jsonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entity.node);
                log.error(e.getMessage());
                log.debug(transformString);
            }
        }
        if (log.isTraceEnabled())
        {
            log.trace("Combined config:\n"+jsonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(transformersNode));
        }

        transforms = sortTransformers(transforms);
        return transforms;
    }

    // Sort transformers so there are no forward references, if that is possible.
    private List<TransformAndItsOrigin> sortTransformers(List<TransformAndItsOrigin> original)
    {
        List<TransformAndItsOrigin> transformers = new ArrayList<>(original.size());
        List<TransformAndItsOrigin> todo = new ArrayList<>(original.size());
        Set<String> transformerNames = new HashSet<>();
        boolean added;
        do
        {
            added = false;
            for (TransformAndItsOrigin entry : original)
            {
                String name = entry.transform.getTransformerName();
                List<TransformStep> pipeline = entry.transform.getTransformerPipeline();
                boolean addEntry = true;
                if (pipeline != null && !pipeline.isEmpty())
                {
                    for (TransformStep step : pipeline)
                    {
                        String stepName = step.getTransformerName();
                        if (!transformerNames.contains(stepName))
                        {
                            todo.add(entry);
                            addEntry = false;
                            break;
                        }
                    }
                }
                if (addEntry)
                {
                    transformers.add(entry);
                    added = true;
                    if (name != null)
                    {
                        transformerNames.add(name);
                    }
                }
            }

            original.clear();
            original.addAll(todo);
            todo.clear();
        }
        while (added && !original.isEmpty());

        transformers.addAll(todo);

        return transformers;
    }

    private ArrayNode getTransformOptions(ArrayNode transformOptions, int i, ObjectNode transform)
    {
        ArrayNode options = null;
        JsonNode optionName = transformOptions.get(i);
        if (optionName.isTextual())
        {
            String name = optionName.asText();
            options = allTransformOptions.get(name);
            if (options == null)
            {
                String message = "Reference to \"transformOptions\": \"" + name + "\" not found. Transformer " +
                        getTransformName(transform) + " ignored.";
                throw new IllegalArgumentException(message);
            }
        }
        return options;
    }

    private String getTransformName(ObjectNode transform)
    {
        String name = "Unknown";
        JsonNode nameNode = transform.get(TRANSFORMER_NAME);
        if (nameNode != null && nameNode.isTextual())
        {
            name = '"'+nameNode.asText()+'"';
        }
        return name;
    }
}
