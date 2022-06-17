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
package org.alfresco.transformer.metadataExtractors;

import org.alfresco.transform.common.Mimetype;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TIFF;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transformer.executors.Tika.readTikaConfig;

/**
 * A Metadata Extractor which makes use of the Apache Tika auto-detection to select the best parser to extract the
 * metadata from a document. This will be used for all files which Tika can handle, but where no other more explicit
 * extractor is defined.
 *
 * Configuration:   (see TikaAutoMetadataExtractor_metadata_extract.properties and tika_engine_config.json)
 *
 * <pre>
 *   <b>author:</b>                 --      cm:author
 *   <b>title:</b>                  --      cm:title
 *   <b>subject:</b>                --      cm:description
 *   <b>created:</b>                --      cm:created
 *   <b>comments:</b>
 *   <b>geo:lat:</b>                --      cm:latitude
 *   <b>geo:long:</b>               --      cm:longitude
 * </pre>
 *
 * @author Nick Burch
 * @author adavis
 */
public class TikaAutoMetadataExtractor extends AbstractTikaMetadataExtractor
{
    private static final Logger logger = LoggerFactory.getLogger(TikaAutoMetadataExtractor.class);

    private static final String EXIF_IMAGE_HEIGHT_TAG = "Exif Image Height";
    private static final String EXIF_IMAGE_WIDTH_TAG = "Exif Image Width";
    private static final String JPEG_IMAGE_HEIGHT_TAG = "Image Height";
    private static final String JPEG_IMAGE_WIDTH_TAG = "Image Width";
    private static final String COMPRESSION_TAG = "Compression";

    protected final TikaConfig tikaConfig;

    public TikaAutoMetadataExtractor()
    {
        super(logger);
        tikaConfig = readTikaConfig(logger);
    }

    /**
     * Does auto-detection to select the best Tika Parser.
     */
    @Override
    protected Parser getParser()
    {
        return new AutoDetectParser(tikaConfig);
    }

    /**
     * Because some editors use JPEG_IMAGE_HEIGHT_TAG when
     * saving JPEG images , a more reliable source for
     * image size are the values provided by Tika
     * and not the exif/tiff metadata read from the file
     * This will override the tiff:Image size
     * which gets embedded into the alfresco node properties
     * for jpeg files that contain such exif information
     */
    @Override
    protected Map<String, Serializable> extractSpecific(Metadata metadata,
                                                        Map<String, Serializable> properties, Map<String,String> headers)
    {
        if (MIMETYPE_IMAGE_JPEG.equals(metadata.get(Metadata.CONTENT_TYPE)))
        {
            //check if the image has exif information
            if (metadata.get(EXIF_IMAGE_WIDTH_TAG) != null
                && metadata.get(EXIF_IMAGE_HEIGHT_TAG) != null
                && metadata.get(COMPRESSION_TAG) != null)
            {
                //replace the exif size properties that will be embedded in the node with
                //the guessed dimensions from Tika
                putRawValue(TIFF.IMAGE_LENGTH.getName(), extractSize(metadata.get(EXIF_IMAGE_HEIGHT_TAG)), properties);
                putRawValue(TIFF.IMAGE_WIDTH.getName(), extractSize(metadata.get(EXIF_IMAGE_WIDTH_TAG)), properties);
                putRawValue(JPEG_IMAGE_HEIGHT_TAG, metadata.get(EXIF_IMAGE_HEIGHT_TAG), properties);
                putRawValue(JPEG_IMAGE_WIDTH_TAG, metadata.get(EXIF_IMAGE_WIDTH_TAG), properties);
            }
        }
        return properties;
    }

    /**
     * Exif metadata for size also returns the string "pixels"
     * after the number value , this function will
     * stop at the first non digit character found in the text
     * @param sizeText string text
     * @return the size value
     */
    private String extractSize(String sizeText)
    {
        StringBuilder sizeValue = new StringBuilder();
        for(char c : sizeText.toCharArray())
        {
            if(Character.isDigit(c))
            {
                sizeValue.append(c);
            }
            else
            {
                break;
            }
        }
        return sizeValue.toString();
    }
}
