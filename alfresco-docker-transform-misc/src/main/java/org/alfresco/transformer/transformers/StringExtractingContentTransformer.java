/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

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
 *
 * @author Derek Hulley
 * @author eknizat
 */
public class StringExtractingContentTransformer implements JavaTransformer
{

    public static final String SOURCE_ENCODING = "sourceEncoding";
    public static final String TARGET_ENCODING = "targetEncoding";

    private static final Log logger = LogFactory.getLog(StringExtractingContentTransformer.class);

    /**
     * Text to text conversions are done directly using the content reader and writer string
     * manipulation methods.
     * <p>
     * Extraction of text from binary content attempts to take the possible character
     * encoding into account.  The text produced from this will, if the encoding was correct,
     * be unformatted but valid.
     */
    @Override
    public void transform(File sourceFile, File targetFile, Map<String, String> parameters)
    {

        String sourceEncoding = parameters.get(SOURCE_ENCODING);
        String targetEncoding = parameters.get(TARGET_ENCODING);

        if (sourceEncoding == null || sourceEncoding.isEmpty())
        {
            throw new IllegalArgumentException("sourceEncoding must be specified.");
        }

        if (targetEncoding == null || targetEncoding.isEmpty())
        {
            throw new IllegalArgumentException("targetEncoding must be specified.");
        }

        if(logger.isDebugEnabled())
        {
            logger.debug("Performing String to String transform with sourceEncoding=" + sourceEncoding
                    + " targetEncoding=" + targetEncoding);
        }

        // get a char reader and writer
        Reader charReader = null;
        Writer charWriter = null;
        try
        {
            if (sourceEncoding == null)
            {
                charReader = new InputStreamReader(new FileInputStream(sourceFile));
            }
            else
            {
                charReader = new InputStreamReader(new FileInputStream(sourceFile), sourceEncoding);
            }
            if (targetEncoding == null)
            {
                charWriter = new OutputStreamWriter(new FileOutputStream(targetFile));
            }
            else
            {
                charWriter = new OutputStreamWriter(new FileOutputStream(targetFile), targetEncoding);
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
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            if (charReader != null)
            {
                try { charReader.close(); } catch (Throwable e) { logger.error(e); }
            }
            if (charWriter != null)
            {
                try { charWriter.close(); } catch (Throwable e) { logger.error(e); }
            }
        }
        // done
    }
}
