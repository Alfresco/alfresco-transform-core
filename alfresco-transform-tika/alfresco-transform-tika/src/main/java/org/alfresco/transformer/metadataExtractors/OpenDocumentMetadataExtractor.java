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
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.odf.OpenDocumentParser;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * {@code "application/vnd.oasis.opendocument..."} and {@code "applicationvnd.oasis.opendocument..."} metadata extractor.
 *
 * Configuration:   (see OpenDocumentMetadataExtractor_metadata_extract.properties and tika_engine_config.json)
 *
 * <pre>
 *   <b>creationDate:</b>           --      cm:created
 *   <b>creator:</b>                --      cm:author
 *   <b>date:</b>
 *   <b>description:</b>            --      cm:description
 *   <b>generator:</b>
 *   <b>initialCreator:</b>
 *   <b>keyword:</b>
 *   <b>language:</b>
 *   <b>printDate:</b>
 *   <b>printedBy:</b>
 *   <b>subject:</b>
 *   <b>title:</b>                  --      cm:title
 *   <b>All user properties</b>
 * </pre>
 *
 * Uses Apache Tika
 *
 * @author Antti Jokipii
 * @author Derek Hulley
 * @author adavis
 */
public class OpenDocumentMetadataExtractor extends AbstractTikaMetadataExtractor
{
    private static final Logger logger = LoggerFactory.getLogger(OpenDocumentMetadataExtractor.class);

    private static final String KEY_CREATION_DATE = "creationDate";
    private static final String KEY_CREATOR = "creator";
    private static final String KEY_DATE = "date";
    private static final String KEY_GENERATOR = "generator";
    private static final String KEY_INITIAL_CREATOR = "initialCreator";
    private static final String KEY_KEYWORD = "keyword";
    private static final String KEY_LANGUAGE = "language";

    private static final String CUSTOM_PREFIX = "custom:";

    private static final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");

    public OpenDocumentMetadataExtractor()
    {
        super(logger);
    }

    @Override
    protected Parser getParser()
    {
        return new OpenDocumentParser();
    }

    @Override
    protected Map<String, Serializable> extractSpecific(Metadata metadata,
                                                        Map<String, Serializable> properties, Map<String, String> headers)
    {
        putRawValue(KEY_CREATION_DATE, getDateOrNull(metadata.get(TikaCoreProperties.CREATED)), properties);
        putRawValue(KEY_CREATOR, metadata.get(TikaCoreProperties.CREATOR), properties);
        putRawValue(KEY_DATE, getDateOrNull(metadata.get(TikaCoreProperties.MODIFIED)), properties);
        putRawValue(KEY_DESCRIPTION, metadata.get(TikaCoreProperties.DESCRIPTION), properties);
        putRawValue(KEY_GENERATOR, metadata.get("generator"), properties);
        putRawValue(KEY_INITIAL_CREATOR, metadata.get("initial-creator"), properties);
        putRawValue(KEY_KEYWORD, metadata.get(TikaCoreProperties.SUBJECT), properties);
        putRawValue(KEY_LANGUAGE, metadata.get(TikaCoreProperties.LANGUAGE), properties);

        // Handle user-defined properties dynamically
        Map<String, Set<String>> mapping = super.getExtractMapping();
        for (String key : mapping.keySet())
        {
            if (metadata.get(CUSTOM_PREFIX + key) != null)
            {
                putRawValue(key, metadata.get(CUSTOM_PREFIX + key), properties);
            }
        }

        return properties;
    }

    private Date getDateOrNull(String dateString)
    {
        if (dateString != null && dateString.length() != 0)
        {
            try
            {
                return dateFormatter.parseDateTime(dateString).toDate();
            }
            catch (IllegalArgumentException ignore)
            {
            }
        }
        return null;
    }
}
