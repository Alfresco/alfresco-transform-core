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

import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_DITA;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_JAVASCRIPT;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_TEXT_PLAIN;

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
public class StringExtractingContentTransformer implements SelectableTransformer
{

    private static final Log logger = LogFactory.getLog(StringExtractingContentTransformer.class);

    private static final String DEFAULT_ENCODING = "UTF-8";

    @Override
    public boolean isTransformable(String sourceMimetype, String targetMimetype, Map<String, String> parameters)
    {
        boolean transformable =  (sourceMimetype.startsWith("text/")
                || MIMETYPE_JAVASCRIPT.equals(sourceMimetype)
                || MIMETYPE_DITA.equals(sourceMimetype))
                && MIMETYPE_TEXT_PLAIN.equals(targetMimetype);
        return transformable;
    }

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
        sourceEncoding = sourceEncoding == null ? DEFAULT_ENCODING : sourceEncoding;
        String targetEncoding = parameters.get(TARGET_ENCODING);
        targetEncoding = targetEncoding == null ? DEFAULT_ENCODING : targetEncoding;

        if(logger.isDebugEnabled())
        {
            logger.debug("Performing text to text transform with sourceEncoding=" + sourceEncoding
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
