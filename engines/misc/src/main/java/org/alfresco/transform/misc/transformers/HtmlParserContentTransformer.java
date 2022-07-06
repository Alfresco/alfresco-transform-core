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
package org.alfresco.transform.misc.transformers;

import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.htmlparser.Parser;
import org.htmlparser.beans.StringBean;
import org.htmlparser.util.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Map;

import static org.alfresco.transform.common.RequestParamMap.SOURCE_ENCODING;

/**
 * Content transformer which wraps the HTML Parser library for
 * parsing HTML content.
 *
 * <p>
 * This code is based on a class of the same name originally implemented in alfresco-repository.
 * </p>
 *
 * <p>
 * Since HTML Parser was updated from v1.6 to v2.1, META tags
 * defining an encoding for the content via http-equiv=Content-Type
 * will ONLY be respected if the encoding of the content item
 * itself is set to ISO-8859-1.
 * </p>
 *
 * <p>
 * Tika Note - could be converted to use the Tika HTML parser,
 * but we'd potentially need a custom text handler to replicate
 * the current settings around links and non-breaking spaces.
 * </p>
 *
 * @author Derek Hulley
 * @author eknizat
 * @see <a href="http://htmlparser.sourceforge.net/">http://htmlparser.sourceforge.net</a>
 * @see org.htmlparser.beans.StringBean
 * @see <a href="http://sourceforge.net/tracker/?func=detail&aid=1644504&group_id=24399&atid=381401">HTML Parser</a>
 */
@Component
public class HtmlParserContentTransformer implements CustomTransformerFileAdaptor
{
    private static final Logger logger = LoggerFactory.getLogger(
        HtmlParserContentTransformer.class);

    @Override
    public String getTransformerName()
    {
        return "html";
    }

    @Override
    public void transform(final String sourceMimetype, final String targetMimetype,
                          final Map<String, String> transformOptions,
                          final File sourceFile, final File targetFile) throws Exception
    {
        String sourceEncoding = transformOptions.get(SOURCE_ENCODING);
        checkEncodingParameter(sourceEncoding, SOURCE_ENCODING);

        if (logger.isDebugEnabled())
        {
            logger.debug("Performing HTML to text transform with sourceEncoding=" + sourceEncoding);
        }

        // Create the extractor
        EncodingAwareStringBean extractor = new EncodingAwareStringBean();
        extractor.setCollapse(false);
        extractor.setLinks(false);
        extractor.setReplaceNonBreakingSpaces(false);
        extractor.setURL(sourceFile, sourceEncoding);
        // get the text
        String text = extractor.getStrings();

        // write it to the writer
        try (Writer writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(targetFile))))
        {
            writer.write(text);
        }
    }

    private void checkEncodingParameter(String encoding, String parameterName)
    {
        try
        {
            if (encoding != null && !Charset.isSupported(encoding))
            {
                throw new IllegalArgumentException(
                    parameterName + "=" + encoding + " is not supported by the JVM.");
            }
        }
        catch (IllegalCharsetNameException e)
        {
            throw new IllegalArgumentException(
                parameterName + "=" + encoding + " is not a valid encoding.");
        }
    }

    /**
     * <p>
     * This code is based on a class of the same name, originally implemented in alfresco-repository.
     * </p>
     *
     * A version of {@link StringBean} which allows control of the
     * encoding in the underlying HTML Parser.
     * Unfortunately, StringBean doesn't allow easy over-riding of
     * this, so we have to duplicate some code to control this.
     * This allows us to correctly handle HTML files where the encoding
     * is specified against the content property (rather than in the
     * HTML Head Meta), see ALF-10466 for details.
     */
    public static class EncodingAwareStringBean extends StringBean
    {
        private static final long serialVersionUID = -9033414360428669553L;

        /**
         * Sets the File to extract strings from, and the encoding
         * it's in (if known to Alfresco)
         *
         * @param file     The File that text should be fetched from.
         * @param encoding The encoding of the input
         */
        public void setURL(File file, String encoding)
        {
            String previousURL = getURL();
            String newURL = file.getAbsolutePath();

            if (previousURL == null || !newURL.equals(previousURL))
            {
                try
                {
                    URLConnection conn = getConnection();

                    if (null == mParser)
                    {
                        mParser = new Parser(newURL);
                    }
                    else
                    {
                        mParser.setURL(newURL);
                    }

                    if (encoding != null)
                    {
                        mParser.setEncoding(encoding);
                    }

                    mPropertySupport.firePropertyChange(StringBean.PROP_URL_PROPERTY, previousURL,
                        getURL());
                    mPropertySupport.firePropertyChange(StringBean.PROP_CONNECTION_PROPERTY, conn,
                        mParser.getConnection());
                    setStrings();
                }
                catch (ParserException pe)
                {
                    updateStrings(pe.toString());
                }
            }
        }

        public String getEncoding()
        {
            return mParser.getEncoding();
        }
    }
}
