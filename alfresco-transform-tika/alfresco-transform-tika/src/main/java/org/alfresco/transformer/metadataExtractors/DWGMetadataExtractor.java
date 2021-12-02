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
import org.apache.tika.parser.dwg.DWGParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;

/**
 * {@code "application/dwg"} and {@code "image/vnd.dwg"} metadata extractor.
 *
 * Configuration:   (see DWGMetadataExtractor_metadata_extract.properties and tika_engine_config.json)
 *
 * <pre>
 *   <b>title:</b>           --      cm:title
 *   <b>description:</b>     --      cm:description
 *   <b>author:</b>          --      cm:author
 *   <b>keywords:</b>
 *   <b>comments:</b>
 *   <b>lastauthor:</b>
 * </pre>
 *
 * @author Nick Burch
 * @author adavis
 */
public class DWGMetadataExtractor extends AbstractTikaMetadataExtractor
{
    private static final Logger logger = LoggerFactory.getLogger(DWGMetadataExtractor.class);

    private static final String KEY_KEYWORD = "keyword";
    private static final String KEY_LAST_AUTHOR = "lastAuthor";

    public DWGMetadataExtractor()
    {
        super(logger);
    }

    @Override
    protected Map<String, Serializable> extractSpecific(Metadata metadata,
                                                        Map<String, Serializable> properties, Map<String,String> headers)
    {
        putRawValue(KEY_KEYWORD, metadata.get(TikaCoreProperties.SUBJECT), properties);
        putRawValue(KEY_LAST_AUTHOR, metadata.get(TikaCoreProperties.MODIFIED), properties);
        return properties;
    }

    @Override
    protected Parser getParser()
    {
        return new DWGParser();
    }
}
