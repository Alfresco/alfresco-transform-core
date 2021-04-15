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

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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

    private static final String EXIFTOOL_CMD = format("env FOO=exiftool {1}", ExternalParser.OUTPUT_FILE_TOKEN, ExternalParser.INPUT_FILE_TOKEN);
    private static final String METADATA_PATTERN = "\\s*([A-Za-z0-9/ \\(\\)]+\\S{1})\\s+:\\s+([A-Za-z0-9\\(\\)\\[\\] \\:\\-\\.]+)\\s*";


    private TikaConfig tikaConfig;

    public IPTCMetadataExtractor() 
    {
        super(logger);
        tikaConfig = readTikaConfig(logger, "external-"+ DEFAULT_CONFIG);
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

        ExternalParser external;
        try {
            external = ExternalParsersFactory.create().get(0);
        } catch (IOException | TikaException e) {
            // Let try to create from scratch if ParserFactory Fails
            external = new ExternalParser();
            external.setCommand(EXIFTOOL_CMD);
            external.setMetadataExtractionPatterns(getExtractionPattern());
        }
        external.setSupportedTypes(addSupportedTypes(external.getSupportedTypes()));
        return external;
    }

    private Set<MediaType> getSupportedTypes() {
        Set<MediaType> supportedTypes = new HashSet<MediaType>();
        supportedTypes.add(new MediaType("image","jpeg"));

        return supportedTypes;
    }

    private Set<MediaType> addSupportedTypes(Set<MediaType>orig) {
        Set<MediaType> combined = new HashSet<>();
        combined.addAll(orig);
        combined.addAll(getSupportedTypes());
        return combined;
    }

    private Map<Pattern, String> getExtractionPattern() {
        var extractionPatterns = new HashMap<Pattern,String>();
        extractionPatterns.put(Pattern.compile(METADATA_PATTERN), "");
        return extractionPatterns;
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
