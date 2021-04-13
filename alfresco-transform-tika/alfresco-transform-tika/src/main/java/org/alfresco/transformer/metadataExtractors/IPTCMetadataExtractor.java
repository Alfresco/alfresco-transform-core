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
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import java.io.IOException;

import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.external.CompositeExternalParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.alfresco.transform.exceptions.TransformException;

public class IPTCMetadataExtractor extends AbstractTikaMetadataExtractor
{

    private static final Logger logger = LoggerFactory.getLogger(IPTCMetadataExtractor.class);

    public IPTCMetadataExtractor() 
    {
        super(IPTCMetadataExtractor.logger);
    }

    /**
     * TODO Remove notes
     * Notes: In media management a patched version of tika is used with a parser not in the core tika
     * product: ExiftoolImageParser
     * https://github.com/apache/tika/pull/92/files
     * 
     */


    @Override
    protected Parser getParser() {
        try {
            return new CompositeExternalParser();
        } catch (IOException | TikaException e) {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
        }
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
     * Required components:
     *  ** ??tika config file?? Might be required to instantiate the external parser if using the TikaAutoMetadataExtractor
     *      ** Tika.readTikaConfig() may need to modified to provide a different config file for external parsers.
     *  ** *_metadata_extract.properties file mapped to the IPTC content model (will need to be created manually)
     *  ** Create the model for IPTC properties.
     */

}
