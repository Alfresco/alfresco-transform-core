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
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.alfresco.transform.base.metadata.AbstractMetadataExtractorEmbedder.Type.EXTRACTOR;

/**
 * POI-based metadata extractor for Office 07 documents. See http://poi.apache.org/ for information on POI.
 *
 * Configuration:   (see PoiMetadataExtractor_metadata_extract.properties and tika_engine_config.json)
 *
 * <pre>
 *   <b>author:</b>                 --      cm:author
 *   <b>title:</b>                  --      cm:title
 *   <b>subject:</b>                --      cm:description
 *   <b>created:</b>                --      cm:created
 *   <b>Any custom property:</b>    --      [not mapped]
 * </pre>

 * @author Nick Burch
 * @author Neil McErlean
 * @author Dmitry Velichkevich
 * @author adavis
 */
@Component
public class PoiMetadataExtractor extends AbstractTikaMetadataExtractorEmbeddor
{
    private static final Logger logger = LoggerFactory.getLogger(PoiMetadataExtractor.class);

    public PoiMetadataExtractor()
    {
        super(EXTRACTOR, logger);
    }

    @Override
    protected Parser getParser()
    {
        return new OOXMLParser();
    }
}
