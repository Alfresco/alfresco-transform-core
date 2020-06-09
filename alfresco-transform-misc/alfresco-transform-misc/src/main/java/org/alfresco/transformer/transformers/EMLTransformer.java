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
package org.alfresco.transformer.transformers;

import org.alfresco.transformer.fs.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_MULTIPART_ALTERNATIVE;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_TEXT_PLAIN;

/**
 * Uses javax.mail.MimeMessage to generate plain text versions of RFC822 email
 * messages. Searches for all text content parts, and returns them. Any
 * attachments are ignored. TIKA Note - could be replaced with the Tika email
 * parser. Would require a recursing parser to be specified, but not the full
 * Auto one (we don't want attachments), just one containing text and html
 * related parsers.
 *
 * <p>
 * This code is based on a class of the same name originally implemented in alfresco-repository.
 * </p>
 */
public class EMLTransformer implements SelectableTransformer

{
    private static final Logger logger = LoggerFactory.getLogger(EMLTransformer.class);

    private static final String CHARSET = "charset";
    private static final String DEFAULT_ENCODING = "UTF-8";

    @Override
    public void transform(final String sourceMimetype, final String targetMimetype, final Map<String, String> parameters,
                          final File sourceFile, final File targetFile) throws Exception
    {
        logger.debug("Performing RFC822 to text transform.");
        // Use try with resource
        try (InputStream contentInputStream = new BufferedInputStream(
            new FileInputStream(sourceFile));
             Writer bufferedFileWriter = new BufferedWriter(new FileWriter(targetFile)))
        {
            MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
                contentInputStream);

            final StringBuilder sb = new StringBuilder();
            Object content = mimeMessage.getContent();
            if (content instanceof Multipart)
            {
                processMultiPart((Multipart) content, sb);
            }
            else
            {
                sb.append(content.toString());
            }
            bufferedFileWriter.write(sb.toString());
        }
    }

    /**
     * Find "text" parts of message recursively and appends it to sb StringBuilder
     *
     * @param multipart Multipart to process
     * @param sb        StringBuilder
     * @throws MessagingException
     * @throws IOException
     */
    private void processMultiPart(Multipart multipart, StringBuilder sb) throws MessagingException,
        IOException
    {
        boolean isAlternativeMultipart = multipart.getContentType().contains(
            MIMETYPE_MULTIPART_ALTERNATIVE);
        if (isAlternativeMultipart)
        {
            processAlternativeMultipart(multipart, sb);
        }
        else
        {
            for (int i = 0, n = multipart.getCount(); i < n; i++)
            {
                Part part = multipart.getBodyPart(i);
                if (part.getContent() instanceof Multipart)
                {
                    processMultiPart((Multipart) part.getContent(), sb);
                }
                else
                {
                    processPart(part, sb);
                }
            }
        }
    }

    /**
     * Finds the suitable part from an multipart/alternative and appends it's text content to StringBuilder sb
     *
     * @param multipart
     * @param sb
     * @throws IOException
     * @throws MessagingException
     */
    private void processAlternativeMultipart(Multipart multipart, StringBuilder sb) throws
        IOException, MessagingException
    {
        Part partToUse = null;
        for (int i = 0, n = multipart.getCount(); i < n; i++)
        {
            Part part = multipart.getBodyPart(i);
            if (part.getContentType().contains(MIMETYPE_TEXT_PLAIN))
            {
                partToUse = part;
                break;
            }
            else if (part.getContentType().contains(MIMETYPE_HTML))
            {
                partToUse = part;
            }
            else if (part.getContentType().contains(MIMETYPE_MULTIPART_ALTERNATIVE))
            {
                if (part.getContent() instanceof Multipart)
                {
                    processAlternativeMultipart((Multipart) part.getContent(), sb);
                }
            }
        }
        if (partToUse != null)
        {
            processPart(partToUse, sb);
        }
    }

    /**
     * Finds text on a given mail part. Accepted parts types are text/html and text/plain.
     * Attachments are ignored
     *
     * @param part
     * @param sb
     * @throws IOException
     * @throws MessagingException
     */
    private void processPart(Part part, StringBuilder sb) throws IOException, MessagingException
    {
        boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition());
        if (isAttachment)
        {
            return;
        }
        if (part.getContentType().contains(MIMETYPE_TEXT_PLAIN))
        {
            sb.append(part.getContent().toString());
        }
        else if (part.getContentType().contains(MIMETYPE_HTML))
        {
            String mailPartContent = part.getContent().toString();

            //create a temporary html file with same mail part content and encoding
            File tempHtmlFile = FileManager.TempFileProvider.createTempFile("EMLTransformer_",
                ".html");
            String encoding = getMailPartContentEncoding(part);
            try (OutputStreamWriter osWriter = new OutputStreamWriter(
                new FileOutputStream(tempHtmlFile), encoding))
            {
                osWriter.write(mailPartContent);
            }

            //transform html file's content to plain text
            HtmlParserContentTransformer.EncodingAwareStringBean extractor = new HtmlParserContentTransformer.EncodingAwareStringBean();
            extractor.setCollapse(false);
            extractor.setLinks(false);
            extractor.setReplaceNonBreakingSpaces(false);
            extractor.setURL(tempHtmlFile, encoding);
            sb.append(extractor.getStrings());

            tempHtmlFile.delete();
        }
    }

    private String getMailPartContentEncoding(Part part) throws MessagingException
    {
        String encoding = DEFAULT_ENCODING;
        String contentType = part.getContentType();
        int startIndex = contentType.indexOf(CHARSET);
        if (startIndex > 0)
        {
            encoding = contentType.substring(startIndex + CHARSET.length() + 1)
                                  .replaceAll("\"", "");
        }
        return encoding;
    }
}