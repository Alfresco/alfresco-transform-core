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
package org.alfresco.transformer.transformers;

import org.alfresco.transformer.AbstractMetadataExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.ChangedCharSetException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Extracts the following values from HTML documents:
 * <pre>
 *   <b>author:</b>                 --      cm:author
 *   <b>title:</b>                  --      cm:title
 *   <b>description:</b>            --      cm:description
 * </pre>
 *
 * TIKA note - all metadata will be present, but will need to
 * search for the variant names ourselves as tika puts them
 * in as-is.
 *
 * Based on HtmlMetadataExtracter from the content repository.
 *
 * @author Jesper Steen Møller
 * @author Derek Hulley
 * @author adavis
 */
public class HtmlMetadataExecutor extends AbstractMetadataExtractor implements SelectableTransformer
{
    private static final Logger logger = LoggerFactory.getLogger(HtmlMetadataExecutor.class);

    private static final String KEY_AUTHOR = "author";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESCRIPTION= "description";

    @Override
    public void transform(File sourceFile, File targetFile, String sourceMimetype, String targetMimetype,
                          Map<String, String> parameters) throws Exception
    {
        Map<String, Serializable> metadata = extractMetadata(sourceFile);

        if (logger.isDebugEnabled())
        {
            metadata.forEach((k,v) -> logger.debug(k+"="+v));
        }

        writeMetadataIntoTargetFile(targetFile, metadata, logger);
    }

    private Map<String, Serializable> extractMetadata(File sourceFile)
    {
        final Map<String, Serializable> rawProperties = new HashMap<>();

        HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback()
        {
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
            {
            }
        };

        try
        {
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
        }
        catch (IOException e)
        {
            new IllegalArgumentException("Failed to extract metadata. "+e.getMessage());
        }
        return rawProperties;
    }
}
