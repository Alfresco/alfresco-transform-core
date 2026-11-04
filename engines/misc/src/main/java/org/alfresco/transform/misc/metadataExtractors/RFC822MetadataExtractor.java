/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2023 Alfresco Software Limited
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
package org.alfresco.transform.misc.metadataExtractors;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import static org.alfresco.transform.base.metadata.AbstractMetadataExtractorEmbedder.Type.EXTRACTOR;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jakarta.mail.Header;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMessage.RecipientType;
import jakarta.mail.internet.MimeUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.metadata.AbstractMetadataExtractorEmbedder;
import org.alfresco.transform.exceptions.TransformException;

/**
 * Metadata extractor for RFC822 mime emails.
 *
 * Configuration: (see HtmlMetadataExtractor_metadata_extract.properties and misc_engine_config.json)
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
@Component
public class RFC822MetadataExtractor extends AbstractMetadataExtractorEmbedder
{
    private static final Logger logger = LoggerFactory.getLogger(RFC822MetadataExtractor.class);

    protected static final String KEY_MESSAGE_FROM = "messageFrom";
    protected static final String KEY_MESSAGE_TO = "messageTo";
    protected static final String KEY_MESSAGE_CC = "messageCc";
    protected static final String KEY_MESSAGE_SUBJECT = "messageSubject";
    protected static final String KEY_MESSAGE_SENT = "messageSent";
    protected static final String KEY_MESSAGE_RECEIVED = "messageReceived";

    public RFC822MetadataExtractor()
    {
        super(EXTRACTOR, logger);
    }

    @Override
    public void embedMetadata(String sourceMimetype, InputStream inputStream, String targetMimetype,
            OutputStream outputStream, Map<String, String> transformOptions, TransformManager transformManager)
            throws Exception
    {
        // Only used for extract, so may be empty.
    }

    @Override
    public Map<String, Serializable> extractMetadata(String sourceMimetype, InputStream inputStream,
            String targetMimetype, OutputStream outputStream, Map<String, String> transformOptions,
            TransformManager transformManager) throws Exception
    {
        try
        {
            return extractFromMessage(inputStream);
        }
        catch (MessagingException e)
        {
            // The same statement EMLTransformer makes about the same library, and it has to be made again here
            // because this is a different entry point: the repository asks for metadata on almost every upload, so an
            // unreadable .eml reaches this method long before it reaches a transform. jakarta.mail raises this only
            // when the bytes are not a message it can read, and a retry hands the same bytes to the same parser.
            //
            // The infrastructure veto still applies on the way out, so a failure that was really the disk or the
            // download is still reported as retryable.
            throw new TransformException(UNPROCESSABLE_ENTITY,
                    "The source is not a readable RFC822 message: " + e.getMessage(), e);
        }
    }

    /**
     * @return the raw properties read from the message. Separated from {@link #extractMetadata} so that the catch there covers the whole read: {@code MimeMessage} parses lazily, so a malformed header can surface at any of the accessors below rather than at construction.
     */
    private Map<String, Serializable> extractFromMessage(InputStream inputStream)
            throws MessagingException, UnsupportedEncodingException
    {
        final Map<String, Serializable> rawProperties = new HashMap<>();

        MimeMessage mimeMessage = new MimeMessage(null, inputStream);

        if (mimeMessage != null)
        {
            /**
             * Extract RFC822 values that doesn't match to headers and need to be encoded. Or those special fields that require some code to extract data
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
             * "Received" ":" ; one per relay ["from" domain] ; sending host ["by" domain] ; receiving host ["via" atom] ; physical path ("with" atom) ; link/mail protocol ["id" msg-id] ; receiver msg id ["for" addr-spec] ; initial form ";" date-time ; time received
             */
            Date rxDate = mimeMessage.getReceivedDate();

            if (rxDate != null)
            {
                // The email implementation extracted the received date for us.
                putRawValue(KEY_MESSAGE_RECEIVED, rxDate, rawProperties);
            }
            else
            {
                // the email implementation did not parse the received date for us.
                String[] rx = mimeMessage.getHeader("received");
                if (rx != null && rx.length > 0)
                {
                    String lastReceived = rx[0];
                    lastReceived = MimeUtility.unfold(lastReceived);
                    int x = lastReceived.lastIndexOf(';');
                    if (x > 0)
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

            /* Extract values from all header fields, including extension fields "X-" */
            Set<String> keys = getExtractMapping().keySet();
            Enumeration<Header> headers = mimeMessage.getAllHeaders();
            while (headers.hasMoreElements())
            {
                Header header = headers.nextElement();
                if (keys.contains(header.getName()))
                {
                    tmp = header.getValue();
                    tmp = tmp != null ? MimeUtility.decodeText(tmp) : null;

                    putRawValue(header.getName(), tmp, rawProperties);
                }
            }
        }

        return rawProperties;
    }
}
