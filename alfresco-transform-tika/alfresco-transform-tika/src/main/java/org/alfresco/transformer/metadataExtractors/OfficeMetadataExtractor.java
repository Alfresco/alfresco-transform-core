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

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;

/**
 * Office file format metadata extractor.
 *
 * Configuration:   (see OfficeMetadataExtractor_metadata_extract.properties and tika_engine_config.json)
 *
 * This extracter uses the POI library to extract the following:
 * <pre>
 *   <b>author:</b>             --      cm:author
 *   <b>title:</b>              --      cm:title
 *   <b>subject:</b>            --      cm:description
 *   <b>createDateTime:</b>     --      cm:created
 *   <b>lastSaveDateTime:</b>   --      cm:modified
 *   <b>comments:</b>
 *   <b>editTime:</b>
 *   <b>format:</b>
 *   <b>keywords:</b>
 *   <b>lastAuthor:</b>
 *   <b>lastPrinted:</b>
 *   <b>osVersion:</b>
 *   <b>thumbnail:</b>
 *   <b>pageCount:</b>
 *   <b>wordCount:</b>
 * </pre>
 *
 * Uses Apache Tika
 *
 * @author Derek Hulley
 * @author Nick Burch
 * @author adavis
 */
public class OfficeMetadataExtractor extends AbstractTikaMetadataExtractor
{
    private static final Logger logger = LoggerFactory.getLogger(OfficeMetadataExtractor.class);

    public static final String KEY_CREATE_DATETIME = "createDateTime";
    public static final String KEY_LAST_SAVE_DATETIME = "lastSaveDateTime";
    public static final String KEY_EDIT_TIME = "editTime";
    public static final String KEY_FORMAT = "format";
    public static final String KEY_KEYWORDS = "keywords";
    public static final String KEY_LAST_AUTHOR = "lastAuthor";
    public static final String KEY_LAST_PRINTED = "lastPrinted";
    public static final String KEY_PAGE_COUNT = "pageCount";
    public static final String KEY_PARAGRAPH_COUNT = "paragraphCount";
    public static final String KEY_WORD_COUNT = "wordCount";

    public OfficeMetadataExtractor()
    {
        super(logger);
    }

    @Override
    protected Parser getParser()
    {
        return new OfficeParser();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Map<String, Serializable> extractSpecific(Metadata metadata,
                                                        Map<String, Serializable> properties, Map<String,String> headers)
    {
        putRawValue(KEY_CREATE_DATETIME, metadata.get(Metadata.CREATION_DATE), properties);
        putRawValue(KEY_LAST_SAVE_DATETIME, metadata.get(Metadata.LAST_SAVED), properties);
        putRawValue(KEY_EDIT_TIME, metadata.get(Metadata.EDIT_TIME), properties);
        putRawValue(KEY_FORMAT, metadata.get(Metadata.FORMAT), properties);
        putRawValue(KEY_KEYWORDS, metadata.get(Metadata.KEYWORDS), properties);
        putRawValue(KEY_LAST_AUTHOR, metadata.get(Metadata.LAST_AUTHOR), properties);
        putRawValue(KEY_LAST_PRINTED, metadata.get(Metadata.LAST_PRINTED), properties);
//       putRawValue(KEY_OS_VERSION, metadata.get(Metadata.OS_VERSION), properties);
//       putRawValue(KEY_THUMBNAIL, metadata.get(Metadata.THUMBNAIL), properties);
        putRawValue(KEY_PAGE_COUNT, metadata.get(Metadata.PAGE_COUNT), properties);
        putRawValue(KEY_PARAGRAPH_COUNT, metadata.get(Metadata.PARAGRAPH_COUNT), properties);
        putRawValue(KEY_WORD_COUNT, metadata.get(Metadata.WORD_COUNT), properties);
        return properties;
    }
}
