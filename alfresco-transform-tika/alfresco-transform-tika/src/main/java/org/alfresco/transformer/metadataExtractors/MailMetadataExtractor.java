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

import org.apache.tika.metadata.Message;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;

/**
 * Outlook MAPI format email metadata extractor.
 *
 * Configuration:   (see MailMetadataExtractor_metadata_extract.properties and tika_engine_config.json)
 *
 * <pre>
 *   <b>sentDate:</b>               --      cm:sentdate
 *   <b>originator:</b>             --      cm:originator,    cm:author
 *   <b>addressee:</b>              --      cm:addressee
 *   <b>addressees:</b>             --      cm:addressees
 *   <b>subjectLine:</b>            --      cm:subjectline,   cm:description
 *   <b>toNames:</b>                --
 *   <b>ccNames:</b>                --
 *   <b>bccNames:</b>               --
 * </pre>
 *
 * TIKA note - to/cc/bcc go into the html part, not the metadata.
 *  Also, email addresses not included as yet.
 *
 * @author Kevin Roast
 * @author adavis
 */
public class MailMetadataExtractor extends AbstractTikaMetadataExtractor
{
    private static final Logger logger = LoggerFactory.getLogger(MailMetadataExtractor.class);

    private static final String KEY_SENT_DATE = "sentDate";
    private static final String KEY_ORIGINATOR = "originator";
    private static final String KEY_ADDRESSEE = "addressee";
    private static final String KEY_ADDRESSEES = "addressees";
    private static final String KEY_SUBJECT = "subjectLine";
    private static final String KEY_TO_NAMES = "toNames";
    private static final String KEY_CC_NAMES = "ccNames";
    private static final String KEY_BCC_NAMES = "bccNames";

    public MailMetadataExtractor()
    {
        super(logger);
    }

    @Override
    protected Parser getParser()
    {
        // The office parser does Outlook as well as Word, Excel etc
        return new OfficeParser();
    }

    @Override
    protected Map<String, Serializable> extractSpecific(Metadata metadata,
                                                        Map<String, Serializable> properties, Map<String,String> headers)
    {
        putRawValue(KEY_ORIGINATOR, metadata.get(TikaCoreProperties.CREATED), properties);
        putRawValue(KEY_SUBJECT, metadata.get(TikaCoreProperties.TITLE), properties);
        putRawValue(KEY_DESCRIPTION, metadata.get(TikaCoreProperties.SUBJECT), properties);
        putRawValue(KEY_SENT_DATE, metadata.get(TikaCoreProperties.MODIFIED), properties);

        // Store the TO, but not cc/bcc in the addressee field
        putRawValue(KEY_ADDRESSEE, metadata.get(Message.MESSAGE_TO), properties);

        // Store each of To, CC and BCC in their own fields
        putRawValue(KEY_TO_NAMES, metadata.getValues(Message.MESSAGE_TO), properties);
        putRawValue(KEY_CC_NAMES, metadata.getValues(Message.MESSAGE_CC), properties);
        putRawValue(KEY_BCC_NAMES, metadata.getValues(Message.MESSAGE_BCC), properties);

        // But store all email addresses (to/cc/bcc) in the addresses field
        putRawValue(KEY_ADDRESSEES, metadata.getValues(Message.MESSAGE_RECIPIENT_ADDRESS), properties);

        return properties;
    }
}
