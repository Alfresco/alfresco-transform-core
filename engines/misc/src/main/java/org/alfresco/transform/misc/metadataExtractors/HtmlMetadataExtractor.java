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
package org.alfresco.transform.misc.metadataExtractors;

import static org.alfresco.transform.base.metadata.AbstractMetadataExtractorEmbedder.Type.EXTRACTOR;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.swing.text.ChangedCharSetException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.metadata.AbstractMetadataExtractorEmbedder;

/**
 * Metadata extractor for HTML and XHTML.
 *
 * Configuration: (see HtmlMetadataExtractor_metadata_extract.properties and misc_engine_config.json)
 *
 * <pre>
 *   <b>author:</b>                 --      cm:author
 *   <b>title:</b>                  --      cm:title
 *   <b>description:</b>            --      cm:description
 * </pre>
 *
 * Based on HtmlMetadataExtracter from the content repository.
 *
 * @author Jesper Steen Møller
 * @author Derek Hulley
 * @author adavis
 */
@Component
public class HtmlMetadataExtractor extends AbstractMetadataExtractorEmbedder
{
    private static final Logger logger = LoggerFactory.getLogger(HtmlMetadataExtractor.class);

    private static final String KEY_AUTHOR = "author";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESCRIPTION = "description";

    public HtmlMetadataExtractor()
    {
        super(EXTRACTOR, logger);
    }

    @Override
    public String getTransformerName()
    {
        return getClass().getSimpleName();
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
        final Map<String, Serializable> rawProperties = new HashMap<>();

        // This Extractor retries if the encoding needs to be changed, so we need to reread the source,
        // so cannot use the input stream provided, as it will get closed.
        final File sourceFile = transformManager.createSourceFile();

        HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {
            StringBuffer title = null;
            boolean inHead = false;

            public void handleText(char[] data, int pos)
            {
                if (title != null)
                {
                    title.append(data);
                }
            }

            public void handleComment(char[] data, int pos)
            {
                // Perhaps sniff for Office 9+ metadata in here?
            }

            public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos)
            {
                if (HTML.Tag.HEAD.equals(t))
                {
                    inHead = true;
                }
                else if (HTML.Tag.TITLE.equals(t) && inHead)
                {
                    title = new StringBuffer();
                }
                else
                {
                    handleSimpleTag(t, a, pos);
                }
            }

            public void handleEndTag(HTML.Tag t, int pos)
            {
                if (HTML.Tag.HEAD.equals(t))
                {
                    inHead = false;
                }
                else if (HTML.Tag.TITLE.equals(t) && title != null)
                {
                    putRawValue(KEY_TITLE, title.toString(), rawProperties);
                    title = null;
                }
            }

            public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos)
            {
                if (HTML.Tag.META.equals(t))
                {
                    Object nameO = a.getAttribute(HTML.Attribute.NAME);
                    Object valueO = a.getAttribute(HTML.Attribute.CONTENT);
                    if (nameO == null || valueO == null)
                    {
                        return;
                    }
                    String name = nameO.toString();

                    if (name.equalsIgnoreCase("creator") || name.equalsIgnoreCase("author")
                            || name.equalsIgnoreCase("dc.creator"))
                    {
                        putRawValue(KEY_AUTHOR, valueO.toString(), rawProperties);
                    }
                    else if (name.equalsIgnoreCase("description") || name.equalsIgnoreCase("dc.description"))
                    {
                        putRawValue(KEY_DESCRIPTION, valueO.toString(), rawProperties);
                    }
                }
            }

            public void handleError(String errorMsg, int pos)
            {}
        };

        String charsetGuess = "UTF-8";
        int tries = 0;
        while (tries < 3)
        {
            rawProperties.clear();
            Reader r = null;

            try (InputStream cis = new FileInputStream(sourceFile))
            {
                // TODO: for now, use default charset; we should attempt to map from html meta-data
                r = new InputStreamReader(cis, charsetGuess);
                HTMLEditorKit.Parser parser = new ParserDelegator();
                parser.parse(r, callback, tries > 0);
                break;
            }
            catch (ChangedCharSetException ccse)
            {
                tries++;
                charsetGuess = ccse.getCharSetSpec();
                int begin = charsetGuess.indexOf("charset=");
                if (begin > 0)
                {
                    charsetGuess = charsetGuess.substring(begin + 8, charsetGuess.length());
                }
            }
            finally
            {
                if (r != null)
                {
                    r.close();
                }
            }
        }

        return rawProperties;
    }
}
