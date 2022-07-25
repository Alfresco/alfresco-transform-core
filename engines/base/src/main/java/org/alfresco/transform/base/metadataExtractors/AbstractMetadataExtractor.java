/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005-2022 Alfresco Software Limited
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
package org.alfresco.transform.base.metadataExtractors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.transform.base.CustomTransformer;
import org.alfresco.transform.base.TransformManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import static org.alfresco.transform.base.metadataExtractors.AbstractMetadataExtractor.Type.EMBEDDER;

/**
 * Helper methods for metadata extract and embed.
 * <p>
 * <i>Much of the code is based on AbstractMappingMetadataExtracter from the
 * content repository. The code has been simplified to only set up mapping one way.</i>
 * <p>
 * If a transform specifies that it can convert from {@code "<MIMETYPE>"} to {@code "alfresco-metadata-extract"}
 * (specified in the {@code engine_config.json}), it is indicating that it can extract metadata from {@code <MIMETYPE>}.
 *
 * The transform results in a Map of extracted properties encoded as json being returned to the content repository.
 * <ul>
 *   <li>The method extracts ALL available metadata from the document with
 *   {@link #extractMetadata(String, InputStream, String, OutputStream, Map, TransformManager)} and then calls
 *   {@link #mapMetadataAndWrite(OutputStream, Map, Map)}.</li>
 *   <li>Selected values from the available metadata are mapped into content repository property names and values,
 *   depending on what is defined in a {@code "<classname>_metadata_extract.properties"} file.</li>
 *   <li>The selected values are set back to the content repository as a JSON representation of a Map, where the values
 *   are applied to the source node.</li>
 * </ul>
 * To support the same functionality as metadata extractors configured inside the content repository,
 * extra key value pairs may be returned from {@link #extractMetadata(String, InputStream, String, OutputStream, Map, TransformManager)}.
 * These are:
 * <ul>
 *     <li>{@code "sys:overwritePolicy"} which can specify the
 *     {@code org.alfresco.repo.content.metadata.MetadataExtracter.OverwritePolicy} name. Defaults to "PRAGMATIC".</li>
 *     <li>{@code "sys:enableStringTagging"} if {@code "true"} finds or creates tags for each string mapped to
 *     {@code cm:taggable}. Defaults to {@code "false"} to ignore mapping strings to tags.</li>
 *     <li>{@code "sys:carryAspectProperties"} </li>
 *     <li>{@code "sys:stringTaggingSeparators"} </li>
 * </ul>
 *
 * If a transform specifies that it can convert from {@code "<MIMETYPE>"} to {@code "alfresco-metadata-embed"}, it is
 * indicating that it can embed metadata in {@code <MIMETYPE>}.
 *
 * The transform calls {@link #embedMetadata(String, InputStream, String, OutputStream, Map, TransformManager)}
 * which should results in a new version of supplied source file that contains the metadata supplied in the transform
 * options.
 * 
 * @author Jesper Steen Møller
 * @author Derek Hulley
 * @author adavis
 */
public abstract class AbstractMetadataExtractor implements CustomTransformer
{
    private static final String EXTRACT = "extract";
    private static final String EMBED = "embed";
    private static final String METADATA = "metadata";
    private static final String EXTRACT_MAPPING = "extractMapping";

    private static final String NAMESPACE_PROPERTY_PREFIX = "namespace.prefix.";
    private static final char NAMESPACE_PREFIX = ':';
    private static final char NAMESPACE_BEGIN = '{';
    private static final char NAMESPACE_END = '}';

    private static final List<String> SYS_PROPERTIES = Arrays.asList(
            "sys:overwritePolicy",
            "sys:enableStringTagging",
            "sys:carryAspectProperties",
            "sys:stringTaggingSeparators");

    private static final ObjectMapper jsonObjectMapper = new ObjectMapper();

    protected final Logger logger;
    private Map<String, Set<String>> defaultExtractMapping;
    private ThreadLocal<Map<String, Set<String>>> extractMapping = new ThreadLocal<>();
    private Map<String, Set<String>> embedMapping;

    public enum Type
    {
        EXTRACTOR, EMBEDDER
    }

    private final Type type;

    public AbstractMetadataExtractor(Type type, Logger logger)
    {
        this.type = type;
        this.logger = logger;
        defaultExtractMapping = Collections.emptyMap();
        embedMapping = Collections.emptyMap();
        try
        {
            defaultExtractMapping = buildExtractMapping();
            embedMapping = buildEmbedMapping();
        }
        catch (Exception e)
        {
            logger.error("Failed to read config", e);
        }
    }

    @Override
    public String getTransformerName()
    {
        return getClass().getSimpleName();
    }

    @Override
    public void transform(String sourceMimetype, InputStream inputStream,
            String targetMimetype, OutputStream outputStream,
            Map<String, String> transformOptions, TransformManager transformManager) throws Exception
    {
        if (type == EMBEDDER)
        {
            embedMetadata(sourceMimetype, inputStream, targetMimetype, outputStream, transformOptions, transformManager);
        }
        else
        {
            extractMapAndWriteMetadata(sourceMimetype, inputStream, targetMimetype, outputStream, transformOptions, transformManager);
        }
    }

    public abstract void embedMetadata(String sourceMimetype, InputStream inputStream, String targetMimetype,
            OutputStream outputStream, Map<String, String> transformOptions, TransformManager transformManager)
            throws Exception;

    protected Map<String, Serializable> getMetadata(Map<String, String> transformOptions)
    {
        String metadataAsJson = transformOptions.get(METADATA);
        if (metadataAsJson == null)
        {
            throw new IllegalArgumentException("No metadata in embed request");
        }

        try
        {
            TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<>() {};
            HashMap<String, Serializable> systemProperties = jsonObjectMapper.readValue(metadataAsJson, typeRef);
            Map<String, Serializable> rawProperties = mapSystemToRaw(systemProperties);
            return rawProperties;
        }
        catch (JsonProcessingException e)
        {
            throw new IllegalArgumentException("Failed to read metadata from request", e);
        }
    }

    private Map<String, Serializable> mapSystemToRaw(Map<String, Serializable> systemMetadata)
    {
        Map<String, Serializable> metadataProperties = new HashMap<>(systemMetadata.size() * 2 + 1);
        for (Map.Entry<String, Serializable> entry : systemMetadata.entrySet())
        {
            String modelProperty = entry.getKey();
            // Check if there is a mapping for this
            if (!embedMapping.containsKey(modelProperty))
            {
                // No mapping - ignore
                continue;
            }
            Serializable documentValue = entry.getValue();
            Set<String> metadataKeys = embedMapping.get(modelProperty);
            for (String metadataKey : metadataKeys)
            {
                metadataProperties.put(metadataKey, documentValue);
            }
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Converted system model values to metadata values: \n" +
                            "   System Properties:    " + systemMetadata + "\n" +
                            "   Metadata Properties: " + metadataProperties);
        }
        return metadataProperties;
    }

    protected Map<String, Set<String>> getExtractMapping()
    {
        return Collections.unmodifiableMap(extractMapping.get());
    }

    public Map<String, Set<String>> getEmbedMapping()
    {
        return Collections.unmodifiableMap(embedMapping);
    }

    /**
     * Based on AbstractMappingMetadataExtracter#getDefaultMapping.
     *
     * This method provides a <i>mapping</i> of where to store the values extracted from the documents. The list of
     * properties need <b>not</b> include all metadata values extracted from the document. This mapping should be
     * defined in a file based on the class name: {@code "<classname>_metadata_extract.properties"}
     * @return Returns a static mapping. It may not be null.
     */
    private Map<String, Set<String>> buildExtractMapping()
    {
        String filename = getPropertiesFilename(EXTRACT);
        Properties properties = readProperties(filename);
        if (properties == null)
        {
            logger.error("Failed to read "+filename);
        }

        Map<String, String> namespacesByPrefix = getNamespaces(properties);
        return buildExtractMapping(properties, namespacesByPrefix);
    }

    private Map<String, Set<String>> buildExtractMapping(Properties properties, Map<String, String> namespacesByPrefix)
    {
        // Create the mapping
        Map<String, Set<String>> convertedMapping = new HashMap<>(17);
        for (Map.Entry<Object, Object> entry : properties.entrySet())
        {
            String documentProperty = (String) entry.getKey();
            String qnamesStr = (String) entry.getValue();
            if (documentProperty.startsWith(NAMESPACE_PROPERTY_PREFIX))
            {
                continue;
            }
            // Create the entry
            Set<String> qnames = new HashSet<>(3);
            convertedMapping.put(documentProperty, qnames);
            // The to value can be a list of QNames
            StringTokenizer tokenizer = new StringTokenizer(qnamesStr, ",");
            while (tokenizer.hasMoreTokens())
            {
                String qnameStr = tokenizer.nextToken().trim();
                qnameStr = getQNameString(namespacesByPrefix, entry, qnameStr, EXTRACT);
                qnames.add(qnameStr);
            }
            if (logger.isTraceEnabled())
            {
                logger.trace("Added mapping from " + documentProperty + " to " + qnames);
            }
        }
        return convertedMapping;
    }

    /**
     * Based on AbstractMappingMetadataExtracter#getDefaultEmbedMapping.
     *
     * This method provides a <i>mapping</i> of model properties that should be embedded in the content.  The list of
     * properties need <b>not</b> include all properties. This mapping should be defined in a file based on the class
     * name: {@code "<classname>_metadata_embed.properties"}
     * <p>
     * If no {@code "<classname>_metadata_embed.properties"} file is found, a reverse of the
     * {@code "<classname>_metadata_extract.properties"} will be assumed. A last win approach will be used for handling
     * duplicates.
     * @return Returns a static mapping. It may not be null.
     */
    private Map<String, Set<String>> buildEmbedMapping()
    {
        String filename = getPropertiesFilename(EMBED);
        Properties properties = readProperties(filename);

        Map<String, Set<String>> embedMapping;
        if (properties != null)
        {
            Map<String, String> namespacesByPrefix = getNamespaces(properties);
            embedMapping = buildEmbedMapping(properties, namespacesByPrefix);
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("No " + filename + ", assuming reverse of extract mapping");
            }
            embedMapping = buildEmbedMappingByReversingExtract();
        }
        return embedMapping;
    }

    private Map<String, Set<String>> buildEmbedMapping(Properties properties, Map<String, String> namespacesByPrefix)
    {
        Map<String, Set<String>> convertedMapping = new HashMap<>(17);
        for (Map.Entry<Object, Object> entry : properties.entrySet())
        {
            String modelProperty = (String) entry.getKey();
            String metadataKeysString = (String) entry.getValue();
            if (modelProperty.startsWith(NAMESPACE_PROPERTY_PREFIX))
            {
                continue;
            }

            modelProperty = getQNameString(namespacesByPrefix, entry, modelProperty, EMBED);
            String[] metadataKeysArray = metadataKeysString.split(",");
            Set<String> metadataKeys = new HashSet<String>(metadataKeysArray.length);
            for (String metadataKey : metadataKeysArray) {
                metadataKeys.add(metadataKey.trim());
            }
            // Create the entry
            convertedMapping.put(modelProperty, metadataKeys);
            if (logger.isTraceEnabled())
            {
                logger.trace("Added mapping from " + modelProperty + " to " + metadataKeysString);
            }
        }
        return convertedMapping;
    }

    private Map<String, Set<String>> buildEmbedMappingByReversingExtract()
    {
        Map<String, Set<String>> extractMapping = buildExtractMapping();
        Map<String, Set<String>> embedMapping;
        embedMapping = new HashMap<>(extractMapping.size());
        for (String metadataKey : extractMapping.keySet())
        {
            if (extractMapping.get(metadataKey) != null && extractMapping.get(metadataKey).size() > 0)
            {
                String modelProperty = extractMapping.get(metadataKey).iterator().next();
                Set<String> metadataKeys = embedMapping.get(modelProperty);
                if (metadataKeys == null)
                {
                    metadataKeys = new HashSet<String>(1);
                    embedMapping.put(modelProperty, metadataKeys);
                }
                metadataKeys.add(metadataKey);
                if (logger.isTraceEnabled())
                {
                    logger.trace("Added mapping from " + modelProperty + " to " + metadataKeys.toString());
                }
            }
        }
        return embedMapping;
    }

    private String getPropertiesFilename(String suffix)
    {
        String className = this.getClass().getName();
        String shortClassName = className.split("\\.")[className.split("\\.").length - 1];
        shortClassName = shortClassName.replace('$', '-');
        // The embedder uses the reverse of the extractor's data.
        shortClassName = shortClassName.replace("Embedder", "Extractor");

        return shortClassName + "_metadata_" + suffix + ".properties";
    }

    private Properties readProperties(String filename)
    {
        Properties properties = null;
        try
        {
            InputStream inputStream = AbstractMetadataExtractor.class.getClassLoader().getResourceAsStream(filename);
            if (inputStream != null)
            {
                properties = new Properties();
                properties.load(inputStream);
            }
        }
        catch (IOException ignore)
        {
        }
        return properties;
    }

    private Map<String, String> getNamespaces(Properties properties)
    {
        Map<String, String> namespacesByPrefix = new HashMap<String, String>(5);
        for (Map.Entry<Object, Object> entry : properties.entrySet())
        {
            String propertyName = (String) entry.getKey();
            if (propertyName.startsWith(NAMESPACE_PROPERTY_PREFIX))
            {
                String prefix = propertyName.substring(17);
                String namespace = (String) entry.getValue();
                namespacesByPrefix.put(prefix, namespace);
            }
        }
        return namespacesByPrefix;
    }

    private String getQNameString(Map<String, String> namespacesByPrefix, Map.Entry<Object, Object> entry, String qnameStr, String type)
    {
        // Check if we need to resolve a namespace reference
        int index = qnameStr.indexOf(NAMESPACE_PREFIX);
        if (index > -1 && qnameStr.charAt(0) != NAMESPACE_BEGIN)
        {
            String prefix = qnameStr.substring(0, index);
            String suffix = qnameStr.substring(index + 1);
            // It is prefixed
            String uri = namespacesByPrefix.get(prefix);
            if (uri == null)
            {
                throw new IllegalArgumentException("No prefix mapping for " + type + " property mapping: \n" +
                        "   Extractor: " + this + "\n" +
                        "   Mapping: " + entry);
            }
            qnameStr = NAMESPACE_BEGIN + uri + NAMESPACE_END + suffix;
        }
        return qnameStr;
    }

    /**
     * Adds a value to the map, conserving null values.  Values are converted to null if:
     * <ul>
     *   <li>it is an empty string value after trimming</li>
     *   <li>it is an empty collection</li>
     *   <li>it is an empty array</li>
     * </ul>
     * String values are trimmed before being put into the map.
     * Otherwise, it is up to the extracter to ensure that the value is a <tt>Serializable</tt>.
     * It is not appropriate to implicitly convert values in order to make them <tt>Serializable</tt>
     * - the best conversion method will depend on the value's specific meaning.
     *
     * @param key           the destination key
     * @param value         the serializable value
     * @param destination   the map to put values into
     * @return              Returns <tt>true</tt> if set, otherwise <tt>false</tt>
     */
    // Copied from the content repository's AbstractMappingMetadataExtracter.
    protected boolean putRawValue(String key, Serializable value, Map<String, Serializable> destination)
    {
        if (value == null)
        {
            // Just keep this
        }
        else if (value instanceof String)
        {
            String valueStr = ((String) value).trim();
            if (valueStr.length() == 0)
            {
                value = null;
            }
            else
            {
                if (valueStr.indexOf("\u0000") != -1)
                {
                    valueStr = valueStr.replaceAll("\u0000", "");
                }
                // Keep the trimmed value
                value = valueStr;
            }
        }
        else if (value instanceof Collection)
        {
            Collection<?> valueCollection = (Collection<?>) value;
            if (valueCollection.isEmpty())
            {
                value = null;
            }
        }
        else if (value.getClass().isArray())
        {
            if (Array.getLength(value) == 0)
            {
                value = null;
            }
        }
        // It passed all the tests
        destination.put(key, value);
        return true;
    }

    private void extractMapAndWriteMetadata(String sourceMimetype, InputStream inputStream, String targetMimetype,
            OutputStream outputStream, Map<String, String> transformOptions, TransformManager transformManager)
            throws Exception
    {
        // Use a ThreadLocal to avoid changing method signatures of methods that currently call getExtractMapping.
        Map<String, Set<String>> mapping = getExtractMappingFromOptions(transformOptions, defaultExtractMapping);
        try
        {
            extractMapping.set(mapping);
            Map<String, Serializable> metadata = extractMetadata(sourceMimetype, inputStream, targetMimetype,
                    outputStream, transformOptions, transformManager);
            mapMetadataAndWrite(outputStream, metadata, mapping);
        }
        finally
        {
            extractMapping.set(null);
        }
    }

    public abstract Map<String, Serializable> extractMetadata(String sourceMimetype, InputStream inputStream,
            String targetMimetype, OutputStream outputStream, Map<String, String> transformOptions,
            TransformManager transformManager) throws Exception;

    private Map<String, Set<String>> getExtractMappingFromOptions(Map<String, String> transformOptions, Map<String,
            Set<String>> defaultExtractMapping)
    {
        String extractMappingOption = transformOptions.get(EXTRACT_MAPPING);
        if (extractMappingOption != null)
        {
            try
            {
                TypeReference<HashMap<String, Set<String>>> typeRef = new TypeReference<>() {};
                return jsonObjectMapper.readValue(extractMappingOption, typeRef);
            }
            catch (JsonProcessingException e)
            {
                throw new IllegalArgumentException("Failed to read "+ EXTRACT_MAPPING +" from request", e);
            }
        }
        return defaultExtractMapping;
    }

    public void mapMetadataAndWrite(OutputStream outputStream, Map<String, Serializable> metadata,
                                    Map<String, Set<String>> extractMapping) throws IOException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Raw metadata:");
            metadata.forEach((k,v) -> logger.debug("  "+k+"="+v));
        }

        metadata = mapRawToSystem(metadata, extractMapping);
        writeMetadata(outputStream, metadata);
    }

    /**
     * Based on AbstractMappingMetadataExtracter#mapRawToSystem.
     *
     * @param rawMetadata    Metadata keyed by document properties
     * @param extractMapping Mapping between document ans system properties
     * @return               Returns the metadata keyed by the system properties
     */
    private Map<String, Serializable> mapRawToSystem(Map<String, Serializable> rawMetadata,
                                                     Map<String, Set<String>> extractMapping)
    {
        boolean debugEnabled = logger.isDebugEnabled();
        if (debugEnabled)
        {
            logger.debug("Returned metadata:");
        }
        Map<String, Serializable> systemProperties = new HashMap<String, Serializable>(rawMetadata.size() * 2 + 1);
        for (Map.Entry<String, Serializable> entry : rawMetadata.entrySet())
        {
            String documentKey = entry.getKey();
            Serializable documentValue = entry.getValue();
            if (SYS_PROPERTIES.contains(documentKey))
            {
                systemProperties.put(documentKey, documentValue);
                if (debugEnabled)
                {
                    logger.debug("  " + documentKey + "=" + documentValue);
                }
                continue;
            }
            // Check if there is a mapping for this
            if (!extractMapping.containsKey(documentKey))
            {
                // No mapping - ignore
                continue;
            }

           Set<String> systemQNames = extractMapping.get(documentKey);
            for (String systemQName : systemQNames)
            {
                if (debugEnabled)
                {
                    logger.debug("  "+systemQName+"="+documentValue+" ("+documentKey+")");
                }
                systemProperties.put(systemQName, documentValue);
            }
        }
        return new TreeMap<String, Serializable>(systemProperties);
    }

    private void writeMetadata(OutputStream outputStream, Map<String, Serializable> results)
            throws IOException
    {
        jsonObjectMapper.writeValue(outputStream, results);
    }
}
