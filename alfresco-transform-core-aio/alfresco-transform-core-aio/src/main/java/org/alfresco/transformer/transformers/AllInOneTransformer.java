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

import org.alfresco.transform.client.model.config.TransformConfig;
import org.alfresco.transform.client.model.config.TransformOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Top level transformer managing multiple sub transformers.
 *
 * @author eknizat
 */
public class AllInOneTransformer implements Transformer
{

    private static final Logger logger = LoggerFactory.getLogger(AllInOneTransformer.class);

    /**
     * Represents the mapping between a transform and a transformer, multiple mappings can point to the same transformer.
     */
    private Map<String, Transformer> transformerTransformMapping = new HashMap();

    public AllInOneTransformer()
    {
        // TODO - use observer style registration?
        try
        {
            this.registerTransformer(new MiscAdapter());
            this.registerTransformer(new TikaAdapter());
            this.registerTransformer(new ImageMagickAdapter());
            this.registerTransformer(new LibreOfficeAdapter());
            this.registerTransformer(new PdfRendererAdapter());
        }
        catch (Exception e)
        {
            // Rethrow as runtime exception, nothing else can be done
            throw new RuntimeException("Failed to register all transformers.", e);
        }
    }

    /**
     * The registration will go through all supported sub transformers and map them to the transformer implementation.
     *
     * @param transformer The transformer implementation,
     *                    this could be a transformer managing multiple sub transformers.
     * @throws Exception Exception is thrown if a mapping for a transformer name already exists.
     */
    public void registerTransformer(Transformer transformer) throws Exception
    {
        for (org.alfresco.transform.client.model.config.Transformer transformerConfig
                : transformer.getTransformConfig().getTransformers())
        {
            String transformerName = transformerConfig.getTransformerName();
            if (transformerTransformMapping.containsKey(transformerName))
            {
                throw new Exception("Transformer name " + transformerName + " is already registered.");
            }

            transformerTransformMapping.put(transformerName, transformer);
            logger.debug("Registered transformer with name: '{}'.", transformerName);
        }
    }

    @Override
    public void transform(File sourceFile, File targetFile, String sourceMimetype, String targetMimetype,
                          Map<String, String> transformOptions) throws Exception
    {
        String transformName = transformOptions.get(TRANSFORM_NAME_PARAMETER);
        Transformer transformer = transformerTransformMapping.get(transformName);

        if (transformer == null)
        {
            throw new Exception("No transformer mapping for : transform:" + transformName + " sourceMimetype:"
                    + sourceMimetype + " targetMimetype:" + targetMimetype);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Performing transform '{}' using {}", transformName, transformer.getClass().getSimpleName());
        }
        transformer.transform(sourceFile, targetFile, sourceMimetype, targetMimetype, transformOptions);
    }

    @Override
    public TransformConfig getTransformConfig()
    {

        // Merge the config for all sub transformers
        List<org.alfresco.transform.client.model.config.Transformer> transformerConfigs = new LinkedList<>();
        Map<String, Set<TransformOption>> transformOptions = new HashMap<>();
        Set<Transformer> distinctTransformers = new HashSet<>(transformerTransformMapping.values());
        {
            for (Transformer transformer: distinctTransformers)
            {
                TransformConfig transformConfig = transformer.getTransformConfig();
                transformerConfigs.addAll(transformConfig.getTransformers());
                transformOptions.putAll(transformConfig.getTransformOptions());
            }
        }

        TransformConfig allInOneConfig = new TransformConfig();
        allInOneConfig.setTransformers(transformerConfigs);
        allInOneConfig.setTransformOptions(transformOptions);

        return allInOneConfig;
    }
}
