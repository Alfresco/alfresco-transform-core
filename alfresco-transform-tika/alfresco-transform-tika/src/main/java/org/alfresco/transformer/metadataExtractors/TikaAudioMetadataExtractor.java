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
package org.alfresco.transformer.metadataExtractors;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.metadata.XMPDM;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.mp4.MP4Parser;
import org.gagravarr.tika.FlacParser;
import org.gagravarr.tika.VorbisParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import static org.alfresco.transformer.executors.Tika.readTikaConfig;

/**
 * A Metadata Extractor which makes use of the Apache Tika Audio Parsers to extract metadata from  media files.
 * For backwards compatibility reasons, this doesn't handle the MP3 format, which has its own dedicated extractor
 * in {@link MP3MetadataExtractor}
 *
 * Configuration:   (see TikaAudioMetadataExtractor_metadata_extract.properties and tika_engine_config.json)
 *
 * <pre>
 *   <b>author:</b>                 --      cm:author
 *   <b>title:</b>                  --      cm:title
 *   <b>created:</b>                --      cm:created
 *   <b>xmpDM:artist</b>            --      audio:artist
 *   <b>xmpDM:composer</b>          --      audio:composer
 *   <b>xmpDM:engineer</b>          --      audio:engineer
 *   <b>xmpDM:genre</b>             --      audio:genre
 *   <b>xmpDM:trackNumber</b>       --      audio:trackNumber
 *   <b>xmpDM:releaseDate</b>       --      audio:releaseDate
 * </pre>
 *
 * @author Nick Burch
 * @author adavis
 */
public class TikaAudioMetadataExtractor extends AbstractTikaMetadataExtractor
{
    private static final Logger logger = LoggerFactory.getLogger(TikaAudioMetadataExtractor.class);

    // The Audio related parsers we use
    private static final Parser[] parsers = new Parser[] {
            new VorbisParser(),
            new FlacParser(),
            new MP4Parser()
    };

    protected final TikaConfig tikaConfig;

    public TikaAudioMetadataExtractor()
    {
        this(logger);
    }

    public TikaAudioMetadataExtractor(Logger logger)
    {
        super(logger);
        tikaConfig = readTikaConfig(logger);
    }

    @Override
    protected Parser getParser()
    {
        return new CompositeParser(tikaConfig.getMediaTypeRegistry(), parsers);
    }

    @Override
    protected Map<String, Serializable> extractSpecific(Metadata metadata,
                                                        Map<String, Serializable> properties, Map<String,String> headers)
    {
        // Most things can go with the default Tika -> Alfresco Mapping
        // Handle the few special cases here

        // The description is special
        putRawValue(KEY_DESCRIPTION, generateDescription(metadata), properties);

        // The release date can be fiddly
        Serializable releaseDate = generateReleaseDate(metadata);
        putRawValue(KEY_CREATED, releaseDate, properties);
        putRawValue(XMPDM.RELEASE_DATE.getName(), releaseDate, properties);

        return properties;
    }

    /**
     * Generates the release date
     */
    private Serializable generateReleaseDate(Metadata metadata)
    {
        String date = metadata.get(XMPDM.RELEASE_DATE);
        if(date == null || date.length() == 0)
        {
            return null;
        }

        // Is it just a year?
        if(date.matches("\\d\\d\\d\\d"))
        {
            // Just a year, we need a full date
            // Go for the 1st of the 1st
            Calendar c = Calendar.getInstance();
            c.set(
                    Integer.parseInt(date), Calendar.JANUARY, 1,
                    0, 0, 0
            );
            c.set(Calendar.MILLISECOND, 0);
            return c.getTime();
        }

        // Treat as a normal date
        return makeDate(date);
    }

    /**
     * Generate the description
     *
     * @param metadata     the metadata extracted from the file
     * @return          the description
     */
    private String generateDescription(Metadata metadata)
    {
        StringBuilder result = new StringBuilder();
        if (metadata.get(TikaCoreProperties.TITLE) != null)
        {
            result.append(metadata.get(TikaCoreProperties.TITLE));
            if (metadata.get(XMPDM.ALBUM) != null)
            {
                result
                        .append(" - ")
                        .append(metadata.get(XMPDM.ALBUM));
            }
            if (metadata.get(XMPDM.ARTIST) != null)
            {
                result
                        .append(" (")
                        .append(metadata.get(XMPDM.ARTIST))
                        .append(")");
            }
        }

        return result.toString();
    }
}
