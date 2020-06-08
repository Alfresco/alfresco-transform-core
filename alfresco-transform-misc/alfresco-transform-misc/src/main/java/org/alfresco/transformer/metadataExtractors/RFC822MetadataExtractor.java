/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005-2020 Alfresco Software Limited
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

import org.alfresco.transformer.transformers.SelectableTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.mail.Header;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.MimeMessage.RecipientType;

/**
 * Metadata extractor for RFC822 mime emails.
 *
 * Configuration:   (see HtmlMetadataExtractor_metadata_extract.properties and misc_engine_config.json)
 *
 * <pre>
 *   <b>messageFrom:</b>              --      imap:messageFrom, cm:originator
 *   <b>messageTo:</b>                --      imap:messageTo
 *   <b>messageCc:</b>                --      imap:messageCc
 *   <b>messageSubject:</b>           --      imap:messageSubject, cm:title, cm:description, cm:subjectline
 *   <b>messageSent:</b>              --      imap:dateSent, cm:sentdate
 *   <b>messageReceived:</b>          --      imap:dateReceived
 *   <b>All {@link Header#getName() header names}:</b>
 *      <b>Thread-Index:</b>          --      imap:threadIndex
 *      <b>Message-ID:</b>            --      imap:messageId
 * </pre>
 *
 * @author Derek Hulley
 * @author adavis
 */
public class RFC822MetadataExtractor extends AbstractMetadataExtractor implements SelectableTransformer
{
    private static final Logger logger = LoggerFactory.getLogger(HtmlMetadataExtractor.class);

    protected static final String KEY_MESSAGE_FROM = "messageFrom";
    protected static final String KEY_MESSAGE_TO = "messageTo";
    protected static final String KEY_MESSAGE_CC = "messageCc";
    protected static final String KEY_MESSAGE_SUBJECT = "messageSubject";
    protected static final String KEY_MESSAGE_SENT = "messageSent";
    protected static final String KEY_MESSAGE_RECEIVED = "messageReceived";

    public RFC822MetadataExtractor()
    {
        super(logger);
    }

    @Override
    public void extractMetadata(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
                                File sourceFile, File targetFile) throws Exception
    {
        Map<String, Serializable> metadata = extractMetadata(sourceMimetype, transformOptions, sourceFile);
        mapMetadataAndWrite(targetFile, metadata);
    }

    @Override
    public Map<String, Serializable> extractMetadata(String sourceMimetype, Map<String, String> transformOptions,
                                                     File sourceFile) throws Exception
    {
        final Map<String, Serializable> rawProperties = new HashMap<>();

        try (InputStream is = new FileInputStream(sourceFile))
        {
            MimeMessage mimeMessage = new MimeMessage(null, is);

            if (mimeMessage != null)
            {
                /**
                 * Extract RFC822 values that doesn't match to headers and need to be encoded.
                 * Or those special fields that require some code to extract data
                 */
                String tmp = InternetAddress.toString(mimeMessage.getFrom());
                tmp = tmp != null ? MimeUtility.decodeText(tmp) : null;
                putRawValue(KEY_MESSAGE_FROM, tmp, rawProperties);

                tmp = InternetAddress.toString(mimeMessage.getRecipients(RecipientType.TO));
                tmp = tmp != null ? MimeUtility.decodeText(tmp) : null;
                putRawValue(KEY_MESSAGE_TO, tmp, rawProperties);

                tmp = InternetAddress.toString(mimeMessage.getRecipients(RecipientType.CC));
                tmp = tmp != null ? MimeUtility.decodeText(tmp) : null;
                putRawValue(KEY_MESSAGE_CC, tmp, rawProperties);

                putRawValue(KEY_MESSAGE_SENT, mimeMessage.getSentDate(), rawProperties);

                /**
                 * Received field from RFC 822
                 *
                 * "Received"    ":"        ; one per relay
                 *   ["from" domain]        ; sending host
                 *   ["by"   domain]        ; receiving host
                 *   ["via"  atom]          ; physical path
                 *  ("with" atom)           ; link/mail protocol
                 *   ["id"   msg-id]        ; receiver msg id
                 *   ["for"  addr-spec]     ; initial form
                 * ";"    date-time         ; time received
                 */
                Date rxDate = mimeMessage.getReceivedDate();

                if(rxDate != null)
                {
                    // The email implementation extracted the received date for us.
                    putRawValue(KEY_MESSAGE_RECEIVED, rxDate, rawProperties);
                }
                else
                {
                    // the email implementation did not parse the received date for us.
                    String[] rx = mimeMessage.getHeader("received");
                    if(rx != null && rx.length > 0)
                    {
                        String lastReceived = rx[0];
                        lastReceived = MimeUtility.unfold(lastReceived);
                        int x = lastReceived.lastIndexOf(';');
                        if(x > 0)
                        {
                            String dateStr = lastReceived.substring(x + 1).trim();
                            putRawValue(KEY_MESSAGE_RECEIVED, dateStr, rawProperties);
                        }
                    }
                }

                String[] subj = mimeMessage.getHeader("Subject");
                if (subj != null && subj.length > 0)
                {
                    String decodedSubject = subj[0];
                    try
                    {
                        decodedSubject = MimeUtility.decodeText(decodedSubject);
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        logger.warn(e.toString());
                    }
                    putRawValue(KEY_MESSAGE_SUBJECT, decodedSubject, rawProperties);
                }

                /*
                 * Extract values from all header fields, including extension fields "X-"
                 */
                Set<String> keys = getExtractMapping().keySet();
                @SuppressWarnings("unchecked")
                Enumeration<Header> headers = mimeMessage.getAllHeaders();
                while (headers.hasMoreElements())
                {
                    Header header = (Header) headers.nextElement();
                    if (keys.contains(header.getName()))
                    {
                        tmp = header.getValue();
                        tmp = tmp != null ? MimeUtility.decodeText(tmp) : null;

                        putRawValue(header.getName(), tmp, rawProperties);
                    }
                }
            }
        }

        return rawProperties;
    }
}
