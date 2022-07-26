/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.tika.metadata.extractors;

import org.alfresco.transform.tika.metadata.AbstractTikaMetadataExtractorEmbeddor;
import org.alfresco.transform.tika.parsers.ExifToolParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.alfresco.transform.base.metadata.AbstractMetadataExtractorEmbedder.Type.EXTRACTOR;

@Component
public class IPTCMetadataExtractor extends AbstractTikaMetadataExtractorEmbeddor
{

    private static final Logger logger = LoggerFactory.getLogger(IPTCMetadataExtractor.class);
    
    private static Set<String> IPTC_DATE_KEYS = Set.of("XMP-photoshop:DateCreated", "XMP-iptcExt:ArtworkDateCreated");

    private static final Pattern YEAR_IPTC = Pattern.compile("(\\d{4}[:|-]\\d{2}[:|-]\\d{2})");

    private ExifToolParser parser;

    public IPTCMetadataExtractor() 
    {
        super(EXTRACTOR, logger);
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
                        // Handle property with a single date string
                        putRawValue(key, (Serializable) iptcToIso8601DateString(value), properties);
                    }
                }
            }
        }
        return properties;
    }

    /**
     * Converts a date or date time strings into Iso8601 format <p>
     * 
     * @param dateStrings
     * @return dateStrings in Iso8601 format
     * @see #iptcToIso8601DateString
     */
    public String[] iptcToIso8601DateStrings(String[] dateStrings)
    {
        for (int i = 0; i < dateStrings.length; i++)
        {
            dateStrings[i] = iptcToIso8601DateString(dateStrings[i]);
        }
        return dateStrings;
    }

    /**
     * Converts a date or date time string into Iso8601 format <p>
     * Converts any ':' in the year portion of a date string characters to '-'. <p>
     * Expects the year in the format YYYY:MM:DD or YYYY-MM-DD <p>
     * Will add the correct delimiter, 'T',  to any dateTime strings, where | can be any char other than ,'T':
     * YYYY:MM:DD|HH:mm:ss.... or YYYY-MM-DD|HH:mm:ss....
     * <p>
     * Examples: <p><ul>
     * <li>"1919:10:16" will convert to "1919-10-16"</li>
     * <li>"1901:02:01 00:00:00.000Z" will convert to "1901-02-01T00:00:00.000Z"</li>
     * <li>"2001:02:01 16:15+00:00" will convert to "2001-02-01T16:15+00:00"</li>
     * <li>"2021-06-11 05:36-01:00" will convert to "2021-06-11T05:36-01:00"</li>
     * </ul>
     * @param dateStr
     * @return dateStr in Iso8601 format
     */
    protected String iptcToIso8601DateString(String dateStr) 
    {
        char timeSeparator = 'T';
        Matcher yearMatcher = YEAR_IPTC.matcher(dateStr);
        if (yearMatcher.find())
        {
            String year = yearMatcher.group(1);
            dateStr = yearMatcher.replaceFirst(year.replaceAll(":", "-"));
            if (dateStr.length()>year.length() && dateStr.charAt(year.length())!=timeSeparator) 
            {
                dateStr = dateStr.replace(dateStr.charAt(year.length()), timeSeparator);
            }
        }
        return dateStr;
    }

}
