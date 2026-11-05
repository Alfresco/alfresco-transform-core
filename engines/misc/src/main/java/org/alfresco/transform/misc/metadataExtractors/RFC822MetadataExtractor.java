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
import jakarta.mail.Address;
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
            // Only raised for unreadable bytes, so a retry cannot help; the infrastructure veto still applies.
            throw new TransformException(UNPROCESSABLE_ENTITY,
                    "The source is not a readable RFC822 message: " + e.getMessage(), e);
        }
    }

    /**
     * Separate so the caller's catch covers the whole read: {@code MimeMessage} parses lazily, so a bad header can fail at any accessor.
     */
    private Map<String, Serializable> extractFromMessage(InputStream inputStream)
            throws MessagingException, UnsupportedEncodingException
    {
        final Map<String, Serializable> rawProperties = new HashMap<>();
        MimeMessage mimeMessage = new MimeMessage(null, inputStream);

        putAddresses(KEY_MESSAGE_FROM, mimeMessage.getFrom(), rawProperties);
        putAddresses(KEY_MESSAGE_TO, mimeMessage.getRecipients(RecipientType.TO), rawProperties);
        putAddresses(KEY_MESSAGE_CC, mimeMessage.getRecipients(RecipientType.CC), rawProperties);
        putRawValue(KEY_MESSAGE_SENT, mimeMessage.getSentDate(), rawProperties);
        putReceivedDate(mimeMessage, rawProperties);
        putSubject(mimeMessage, rawProperties);
        putMappedHeaders(mimeMessage, rawProperties);

        return rawProperties;
    }

    private void putAddresses(String key, Address[] addresses, Map<String, Serializable> rawProperties)
            throws UnsupportedEncodingException
    {
        String value = InternetAddress.toString(addresses);
        putRawValue(key, value != null ? MimeUtility.decodeText(value) : null, rawProperties);
    }

    /**
     * Reads the received date, falling back to the last {@code Received} header, which ends with {@code ";" date-time}.
     */
    private void putReceivedDate(MimeMessage mimeMessage, Map<String, Serializable> rawProperties)
            throws MessagingException
    {
        Date rxDate = mimeMessage.getReceivedDate();
        if (rxDate != null)
        {
            putRawValue(KEY_MESSAGE_RECEIVED, rxDate, rawProperties);
            return;
        }

        String[] rx = mimeMessage.getHeader("received");
        if (rx == null || rx.length == 0)
        {
            return;
        }

        String lastReceived = MimeUtility.unfold(rx[0]);
        int x = lastReceived.lastIndexOf(';');
        if (x > 0)
        {
            putRawValue(KEY_MESSAGE_RECEIVED, lastReceived.substring(x + 1).trim(), rawProperties);
        }
    }

    private void putSubject(MimeMessage mimeMessage, Map<String, Serializable> rawProperties)
            throws MessagingException
    {
        String[] subj = mimeMessage.getHeader("Subject");
        if (subj == null || subj.length == 0)
        {
            return;
        }

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

    /**
     * Extracts values from all mapped header fields, including extension fields "X-".
     */
    private void putMappedHeaders(MimeMessage mimeMessage, Map<String, Serializable> rawProperties)
            throws MessagingException, UnsupportedEncodingException
    {
        Set<String> keys = getExtractMapping().keySet();
        Enumeration<Header> headers = mimeMessage.getAllHeaders();
        while (headers.hasMoreElements())
        {
            Header header = headers.nextElement();
            if (keys.contains(header.getName()))
            {
                String value = header.getValue();
                putRawValue(header.getName(), value != null ? MimeUtility.decodeText(value) : null, rawProperties);
            }
        }
    }
}
