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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfresco.transformer.tika.parsers.ExifToolParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jdk.internal.jshell.tool.resources.l10n;

public class IPTCMetadataExtractor extends AbstractTikaMetadataExtractor
{

    private static final Logger logger = LoggerFactory.getLogger(IPTCMetadataExtractor.class);
    
    private static Set<String> IPTC_DATE_KEYS = Set.of("XMP-photoshop:DateCreated", "XMP-iptcExt:ArtworkDateCreated");

    private ExifToolParser parser;

    public IPTCMetadataExtractor() 
    {
        super(logger);
    }

    @Override
    protected Parser getParser() 
    {
        if (this.parser == null) {
            this.parser = new ExifToolParser();
        }
        return this.parser;  
    }

    /**
     * Because some of the mimetypes that IPTCMetadataExtractor now parse, were previously handled 
     * by TikaAutoMetadataExtractor we call the TikaAutoMetadataExtractor.extractSpecific method to 
     * ensure that the returned properties contains the expected entries.
     */
    @Override
    protected Map<String, Serializable> extractSpecific(Metadata metadata, Map<String, Serializable> properties,
            Map<String, String> headers) 
    {
        properties = new TikaAutoMetadataExtractor().extractSpecific(metadata, properties, headers);
        ExifToolParser etParser = (ExifToolParser)this.getParser();
        if (etParser.getSeparator()!=null)
        {
            for (String key : properties.keySet())
            {
                if (properties.get(key) instanceof String)
                {
                    String value = (String) properties.get(key);
                    String separator = etParser.getSeparator();
                    if (value.contains(separator))
                    {
                        if (value.contains(String.format("\"%s\"",separator)))
                        {
                            separator = String.format("\"%s\"",separator);
                        }
                        String [] values = StringUtils.splitByWholeSeparator(value, separator);
                        // Change dateTime format. MM converted ':' to '-'
                        if (IPTC_DATE_KEYS.contains(key)){
                            values =  iptcToIso8601DateStrings(values);
                        }
                        putRawValue(key, (Serializable) Arrays.asList(values), properties);
                    }
                    else if (IPTC_DATE_KEYS.contains(key)) {
                        // Handle key with single date string
                        putRawValue(key, (Serializable) iptcToIso8601DateString(value), properties);
                    }
                }
            }
        }
        return properties;
    }

    protected String[] iptcToIso8601DateStrings(String[] values)
    {
        for (int i = 0; i < values.length; i++)
        {
            values[i] = iptcToIso8601DateString(values[i]);
        }
        return values;
    }

    protected String iptcToIso8601DateString(String value) {
        return value.replaceAll(":", "-");
    }

}
