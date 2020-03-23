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

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.alfresco.transformer.transformers.StringExtractingContentTransformer.SOURCE_ENCODING;
import static org.junit.Assert.assertEquals;

public class HtmlParserContentTransformerTest
{
    private static final String SOURCE_MIMETYPE = "text/html";
    private static final String TARGET_MIMETYPE = "text/plain";

    HtmlParserContentTransformer transformer = new HtmlParserContentTransformer();

    /**
     * Checks that we correctly handle text in different encodings,
     * no matter if the encoding is specified on the Content Property
     * or in a meta tag within the HTML itself. (ALF-10466)
     *
     * On Windows, org.htmlparser.beans.StringBean.carriageReturn() appends a new system dependent new line
     * so we must be careful when checking the returned text
     */
    @Test
    public void testEncodingHandling() throws Exception
    {
        final String NEWLINE = System.getProperty("line.separator");
        final String TITLE = "Testing!";
        final String TEXT_P1 = "This is some text in English";
        final String TEXT_P2 = "This is more text in English";
        final String TEXT_P3 = "C'est en Fran\u00e7ais et Espa\u00f1ol";
        String partA = "<html><head><title>" + TITLE + "</title></head>" + NEWLINE;
        String partB = "<body><p>" + TEXT_P1 + "</p>" + NEWLINE +
                       "<p>" + TEXT_P2 + "</p>" + NEWLINE +
                       "<p>" + TEXT_P3 + "</p>" + NEWLINE;
        String partC = "</body></html>";
        final String expected = TITLE + NEWLINE + TEXT_P1 + NEWLINE + TEXT_P2 + NEWLINE + TEXT_P3 + NEWLINE;

        File tmpS = null;
        File tmpD = null;

        try
        {
            // Content set to ISO 8859-1
            tmpS = File.createTempFile("AlfrescoTestSource_", ".html");
            writeToFile(tmpS, partA + partB + partC, "ISO-8859-1");

            tmpD = File.createTempFile("AlfrescoTestTarget_", ".txt");

            Map<String, String> parameters = new HashMap<>();
            parameters.put(SOURCE_ENCODING, "ISO-8859-1");
            transformer.transform(tmpS, tmpD, SOURCE_MIMETYPE, TARGET_MIMETYPE, parameters);

            assertEquals(expected, readFromFile(tmpD, "UTF-8"));
            tmpS.delete();
            tmpD.delete();

            // Content set to UTF-8
            tmpS = File.createTempFile("AlfrescoTestSource_", ".html");
            writeToFile(tmpS, partA + partB + partC, "UTF-8");

            tmpD = File.createTempFile("AlfrescoTestTarget_", ".txt");
            parameters = new HashMap<>();
            parameters.put(SOURCE_ENCODING, "UTF-8");
            transformer.transform(tmpS, tmpD, SOURCE_MIMETYPE, TARGET_MIMETYPE, parameters);
            assertEquals(expected, readFromFile(tmpD, "UTF-8"));
            tmpS.delete();
            tmpD.delete();

            // Content set to UTF-16
            tmpS = File.createTempFile("AlfrescoTestSource_", ".html");
            writeToFile(tmpS, partA + partB + partC, "UTF-16");

            tmpD = File.createTempFile("AlfrescoTestTarget_", ".txt");
            parameters = new HashMap<>();
            parameters.put(SOURCE_ENCODING, "UTF-16");
            transformer.transform(tmpS, tmpD, SOURCE_MIMETYPE, TARGET_MIMETYPE, parameters);
            assertEquals(expected, readFromFile(tmpD, "UTF-8"));
            tmpS.delete();
            tmpD.delete();

            // Note - since HTML Parser 2.0 META tags specifying the
            // document encoding will ONLY be respected if the original
            // content type was set to ISO-8859-1.
            //
            // This means there is now only one test which we can perform
            // to ensure that this now-limited overriding of the encoding
            // takes effect.

            // Content set to ISO 8859-1, meta set to UTF-8
            tmpS = File.createTempFile("AlfrescoTestSource_", ".html");
            String str = partA +
                         "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" +
                         partB + partC;

            writeToFile(tmpS, str, "UTF-8");

            tmpD = File.createTempFile("AlfrescoTestTarget_", ".txt");

            parameters = new HashMap<>();
            parameters.put(SOURCE_ENCODING, "ISO-8859-1");
            transformer.transform(tmpS, tmpD, SOURCE_MIMETYPE, TARGET_MIMETYPE, parameters);
            assertEquals(expected, readFromFile(tmpD, "UTF-8"));
            tmpS.delete();
            tmpD.delete();

            // Note - we can't test UTF-16 with only a meta encoding,
            //  because without that the parser won't know about the
            //  2 byte format so won't be able to identify the meta tag
        }
        finally
        {
            if (tmpS != null && tmpS.exists()) tmpS.delete();
            if (tmpD != null && tmpD.exists()) tmpD.delete();
        }
    }

    private void writeToFile(File file, String content, String encoding) throws Exception
    {
        try (OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(file), encoding))
        {
            ow.append(content);
        }
    }

    private String readFromFile(File file, final String encoding) throws Exception
    {
        return new String(Files.readAllBytes(file.toPath()), encoding);
    }
}