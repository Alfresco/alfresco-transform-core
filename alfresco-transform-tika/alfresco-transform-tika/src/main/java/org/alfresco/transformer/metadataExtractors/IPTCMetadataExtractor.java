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
package org.alfresco.transformer.metadataExtractors;

import static java.text.MessageFormat.format;
import static org.alfresco.transformer.executors.Tika.DEFAULT_CONFIG;
import static org.alfresco.transformer.executors.Tika.readTikaConfig;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.alfresco.transform.exceptions.TransformException;
import org.apache.tika.config.ServiceLoader;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.external.ExternalParser;
import org.apache.tika.parser.external.ExternalParsersFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPTCMetadataExtractor extends AbstractTikaMetadataExtractor
{

    private static final Logger logger = LoggerFactory.getLogger(IPTCMetadataExtractor.class);

    private static final String EXIFTOOL_PARSER_CONFIG = "parsers/external/config/exiftool-parser.xml";

    public IPTCMetadataExtractor() 
    {
        super(logger);
    }

    /**
     * TODO Remove notes
     * Notes: In media management a patched/forked version of tika is used with a parser not in the core tika
     * product: ExiftoolImageParser
     * https://github.com/apache/tika/pull/92/files
     * 
     */

    @Override
    protected Parser getParser() {
        return createExifToolParser();
    }

    private Parser createExifToolParser() {
        try {
           return ExternalParsersFactory.create(getExternalParserConfigURL()).get(0);
        } catch (IOException | TikaException e) {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(), "Error creating Exiftool Parser", e);
        }
    }

    private URL getExternalParserConfigURL(){
        ClassLoader classLoader = IPTCMetadataExtractor.class.getClassLoader();
        return classLoader.getResource(EXIFTOOL_PARSER_CONFIG);
    }

    // TODO REMOVE NOTES
    /** Notes
     * Looks like we can use the autoTikaParser to create an external parser, and provide it with the correct config:
     * see https://cwiki.apache.org/confluence/display/TIKA/EXIFToolParser
     * In order to add additional properties to the super method the extractSpecific() method needs to be overridden.
     * This will then follow this loose flow:
     *  Perform default extraction -> do additional "Specific" extractions
     * by default extractSpecific() does nothing.
     * 
     * If the correct parser is instantiated then we will be able to find the desired IPTC metadata when the
     * parse() method is called by the super class. We will then add the desired additional results via extractSpecific()
     * 
     * Required components:
     *  ** ??tika config file?? Might be required to instantiate the external parser if using the TikaAutoMetadataExtractor
     *      ** Tika.readTikaConfig() may need to modified to provide a different config file for external parsers.
     *  ** *_metadata_extract.properties file mapped to the IPTC content model (will need to be created manually)
     *  ** Create the model for IPTC properties.
     */

    @Override
    protected Map<String, Serializable> extractSpecific(Metadata metadata,
                                                        Map<String, Serializable> properties, Map<String,String> headers)
    {
        
        return properties;
    }
}
