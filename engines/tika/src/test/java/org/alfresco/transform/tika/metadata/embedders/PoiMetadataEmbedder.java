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
package org.alfresco.transform.tika.metadata.embedders;

import static org.alfresco.transform.base.metadata.AbstractMetadataExtractorEmbedder.Type.EMBEDDER;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tika.embedder.Embedder;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.alfresco.transform.tika.metadata.AbstractTikaMetadataExtractorEmbeddor;

/**
 * Sample POI metadata embedder to demonstrate it is possible to add custom T-Engines that will add metadata. This is not production code, so no supported mimetypes exist in the {@code tika_engine_config.json}.
 */
@Component
public class PoiMetadataEmbedder extends AbstractTikaMetadataExtractorEmbeddor
{
    private static final Logger logger = LoggerFactory.getLogger(PoiMetadataEmbedder.class);

    public PoiMetadataEmbedder()
    {
        super(EMBEDDER, logger);
    }

    @Override
    protected Parser getParser()
    {
        return new OOXMLParser();
    }

    @Override
    protected Embedder getEmbedder()
    {
        return new SamplePoiEmbedder();
    }

    private static class SamplePoiEmbedder implements Embedder
    {
        private static final Set<MediaType> SUPPORTED_EMBED_TYPES = Collections.singleton(MediaType.application("vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        @Override
        public Set<MediaType> getSupportedEmbedTypes(ParseContext parseContext)
        {
            return SUPPORTED_EMBED_TYPES;
        }

        @Override
        public void embed(Metadata metadata, InputStream inputStream, OutputStream outputStream, ParseContext parseContext)
                throws IOException
        {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            POIXMLProperties props = workbook.getProperties();

            POIXMLProperties.CoreProperties coreProp = props.getCoreProperties();
            POIXMLProperties.CustomProperties custProp = props.getCustomProperties();

            for (String name : metadata.names())
            {
                metadata.isMultiValued("description");
                String value;
                if (metadata.isMultiValued(name))
                {
                    String[] values = metadata.getValues(name);
                    StringJoiner sj = new StringJoiner(", ");
                    for (String s : values)
                    {
                        sj.add(s);
                    }
                    value = sj.toString();
                }
                else
                {
                    value = metadata.get(name);
                }
                switch (name)
                {
                case "author":
                    coreProp.setCreator(value);
                    break;
                case "title":
                    coreProp.setTitle(value);
                    break;
                case "description":
                    coreProp.setDescription(value);
                    break;
                // There are other core values but this is sample code, so we will assume it is a custom value.
                default:
                    custProp.addProperty(name, value);
                    break;
                }
            }
            workbook.write(outputStream);
        }
    }
}
