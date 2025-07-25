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

import static org.alfresco.transform.common.RequestParamMap.SOURCE_ENCODING;
import static org.alfresco.transform.common.RequestParamMap.TARGET_ENCODING;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;

/**
 * Converts any textual format to plain text.
 * <p>
 * The transformation is sensitive to the source and target string encodings.
 *
 *
 * <p>
 * This code is based on a class of the same name originally implemented in alfresco-repository.
 * </p>
 *
 * @author Derek Hulley
 * @author eknizat
 */
@Component
public class StringExtractingContentTransformer implements CustomTransformerFileAdaptor
{
    private static final Logger logger = LoggerFactory.getLogger(StringExtractingContentTransformer.class);

    public String getTransformerName()
    {
        return "string";
    }

    /**
     * Text to text conversions are done directly using the content reader and writer string manipulation methods.
     * <p>
     * Extraction of text from binary content attempts to take the possible character encoding into account. The text produced from this will, if the encoding was correct, be unformatted but valid.
     */
    @Override
    public void transform(final String sourceMimetype, final String targetMimetype, final Map<String, String> transformOptions,
            final File sourceFile, final File targetFile, TransformManager transformManager) throws Exception
    {
        String sourceEncoding = transformOptions.get(SOURCE_ENCODING);
        String targetEncoding = transformOptions.get(TARGET_ENCODING);

        if (logger.isDebugEnabled())
        {
            logger.debug("Performing text to text transform with sourceEncoding=" + sourceEncoding
                    + " targetEncoding=" + targetEncoding);
        }

        Reader charReader = null;
        Writer charWriter = null;
        try
        {
            // Build reader
            if (sourceEncoding == null)
            {
                charReader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(sourceFile)));
            }
            else
            {
                checkEncodingParameter(sourceEncoding, SOURCE_ENCODING);
                charReader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(sourceFile), sourceEncoding));
            }

            // Build writer
            if (targetEncoding == null)
            {
                charWriter = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(targetFile)));
            }
            else
            {
                checkEncodingParameter(targetEncoding, TARGET_ENCODING);
                charWriter = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(targetFile), targetEncoding));
            }

            // copy from the one to the other
            char[] buffer = new char[8192];
            int readCount = 0;
            while (readCount > -1)
            {
                // write the last read count number of bytes
                charWriter.write(buffer, 0, readCount);
                // fill the buffer again
                readCount = charReader.read(buffer);
            }
        }
        finally
        {
            if (charReader != null)
            {
                try
                {
                    charReader.close();
                }
                catch (Throwable e)
                {
                    logger.error("Failed to close charReader", e);
                }
            }
            if (charWriter != null)
            {
                try
                {
                    charWriter.close();
                }
                catch (Throwable e)
                {
                    logger.error("Failed to close charWriter", e);
                }
            }
        }
        // done
    }

    private void checkEncodingParameter(String encoding, String paramterName)
    {
        try
        {
            if (!Charset.isSupported(encoding))
            {
                throw new IllegalArgumentException(
                        paramterName + "=" + encoding + " is not supported by the JVM.");
            }
        }
        catch (IllegalCharsetNameException e)
        {
            throw new IllegalArgumentException(
                    paramterName + "=" + encoding + " is not a valid encoding.");
        }
    }
}
