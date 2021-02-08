/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.alfresco.transformer.util.RequestParamMap.PAGE_LIMIT;
import static org.alfresco.transformer.util.RequestParamMap.SOURCE_ENCODING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TextToPdfContentTransformerTest
{
    public static final Character REVERSE_BOM = Character.valueOf('\uFFFE');
    public static final Character NORMAL_BOM = Character.valueOf('\uFEFF');
    TextToPdfContentTransformer transformer = new TextToPdfContentTransformer();

    @Before
    public void setUp()
    {
        transformer.setStandardFont("Times-Roman");
        transformer.setFontSize(20);
    }

    @Test
    public void testUnlimitedPages() throws Exception
    {
        transformTextAndCheckPageLength(-1);
    }

    @Test
    public void testLimitedTo1Page() throws Exception
    {
        transformTextAndCheckPageLength(1);
    }

    @Test
    public void testLimitedTo2Pages() throws Exception
    {
        transformTextAndCheckPageLength(2);
    }

    @Test
    public void testLimitedTo50Pages() throws Exception
    {
        transformTextAndCheckPageLength(50);
    }

    @Test
    public void test1UTF16BigEndianBomBigEndianChars() throws Exception
    {
        // 1. BOM indicates BE (fe then ff) + chars appear to be BE (as first byte read tends to be a zero)
        //    Expected with UTF-16. Some systems use BE and other like Windows and Mac used LE
        transformTextAndCheck("UTF-16", "UTF-16BE", NORMAL_BOM,
                "fe ff 00 31 00 20 00 49");
        transformTextAndCheck("UTF-16", "UTF-16", null,
                "fe ff 00 31 00 20 00 49");

        transformTextAndCheck("UTF-16BE", "UTF-16", null,
                "fe ff 00 31 00 20 00 49");
        transformTextAndCheck("UTF-16LE", "UTF-16", null,
                "fe ff 00 31 00 20 00 49");
    }

    @Test
    public void test2UTF16LittleEndianBomLittleEndianChars() throws Exception
    {
        // 2. BOM indicates LE (ff then fe) + chars appear to be LE (as second byte read tends to be a zero)
        //    Expected with UTF-16. Some systems use BE and other like Windows and Mac used LE
        transformTextAndCheck("UTF-16", "UTF-16LE", REVERSE_BOM,
                "ff fe 31 00 20 00 49 00");
    }

    @Test
    public void test3UTF16NoBomBigEndianChars() throws Exception
    {
        // 3. No BOM + chars appear to be BE (as first byte read tends to be a zero)
        //    Expected with UTF-16BE
        transformTextAndCheck("UTF-16", "UTF-16BE", null,
                "00 31 00 20 00 49");
    }

    @Test
    public void test4UTF16NoBomLittleEndianChars() throws Exception
    {
        // 4. No BOM + chars appear to be LE (as second byte read tends to be a zero)
        //    Expected with UTF-16LE
        transformTextAndCheck("UTF-16", "UTF-16LE", null,
                "31 00 20 00 49 00");
    }

    @Test
    public void test5UTF16BigEndianBomLittleEndianChars() throws Exception
    {
        // 5. BOM indicates BE (fe then ff) + chars appear to be LE (as second byte read tends to be a zero)
        //    SOMETHING IS WRONG, BUT USE LE!!!!
        transformTextAndCheck("UTF-16", "UTF-16LE", NORMAL_BOM,
                "fe ff 31 00 20 00 49 00");
    }

    @Test
    public void test6UTF16LittleEndianBomBigEndianChars() throws Exception
    {
        // 6. BOM indicates LE (ff then fe) + chars appear to be BE (as first byte read tends to be a zero)
        //    SOMETHING IS WRONG, BUT USE BE!!!!
        transformTextAndCheck("UTF-16", "UTF-16BE", REVERSE_BOM,
                "ff fe 00 31 00 20 00 49");
    }

    private void transformTextAndCheck(String encoding, String actualSourceEncoding,
                                       Character byteOrderMark, String expectedByteOrder) throws Exception
    {
        transformTextAndCheckPageLength(-1, encoding, actualSourceEncoding, byteOrderMark, expectedByteOrder);
    }

    private void transformTextAndCheckPageLength(int pageLimit) throws Exception
    {
        transformTextAndCheck("UTF-8", "UTF-8", null, null);
    }

    private void transformTextAndCheckPageLength(int pageLimit, String encoding, String actualSourceEncoding,
                                                 Character byteOrderMark, String expectedByteOrder) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        String checkText = createTestText(pageLimit, byteOrderMark, sb);
        String text = sb.toString();

        File sourceFile = File.createTempFile("AlfrescoTestSource_", ".txt");
        writeToFile(sourceFile, text, actualSourceEncoding);
        checkFileBytes(sourceFile, expectedByteOrder);

        transformTextAndCheck(sourceFile, encoding, checkText, String.valueOf(pageLimit));
    }

    private String createTestText(int pageLimit, Character byteOrderMark, StringBuilder sb)
    {
        int pageLength = 32;
        int lines = (pageLength + 10) * ((pageLimit > 0) ? pageLimit : 1);
        if (byteOrderMark != null)
        {
            sb.append(byteOrderMark);
        }
        String checkText = null;
        int cutoff = pageLimit * pageLength;
        for (int i = 1; i <= lines; i++)
        {
            sb.append(Integer.toString(i));
            sb.append(" I must not talk in class or feed my homework to my cat.\n");
            if (i == cutoff)
            {
                checkText = sb.toString();
            }
        }
        sb.append("\nBart\n");

        String text = sb.toString();
        checkText = checkText == null ? clean(text) : clean(checkText);
        checkText =  byteOrderMark != null ? checkText.substring(1) : checkText;

        return checkText;
    }

    private void transformTextAndCheck(File sourceFile, String encoding, String checkText,
        String pageLimit) throws Exception
    {
        // And a temp writer
        File targetFile = File.createTempFile("AlfrescoTestTarget_", ".pdf");

        // Transform to PDF
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PAGE_LIMIT, pageLimit);
        parameters.put(SOURCE_ENCODING, encoding);
        transformer.transform("text/plain", "application/pdf", parameters, sourceFile, targetFile);

        // Read back in the PDF and check it
        PDDocument doc = PDDocument.load(targetFile);
        PDFTextStripper textStripper = new PDFTextStripper();
        StringWriter textWriter = new StringWriter();
        textStripper.writeText(doc, textWriter);
        doc.close();

        String roundTrip = clean(textWriter.toString());

        assertEquals(
            "Incorrect text in PDF when starting from text in " + encoding,
            checkText, roundTrip
        );

        sourceFile.delete();
        targetFile.delete();
    }

    private String clean(String text)
    {
        text = text.replaceAll("\\s+\\r", "");
        text = text.replaceAll("\\s+\\n", "");
        text = text.replaceAll("\\r", "");
        text = text.replaceAll("\\n", "");
        return text;
    }

    private void writeToFile(File file, String content, String encoding) throws Exception
    {
        try (OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(file), encoding))
        {
            ow.append(content);
        }
    }

    /**
     * Check the first few bytes in the source file match what we planed to use later as test data.
     */
    private void checkFileBytes(File sourceFile, String expectedByteOrder) throws Exception
    {
        if (expectedByteOrder != null)
        {
            byte[] expectedBytes = hexToBytes(expectedByteOrder); // new BigInteger(expectedByteOrder,16).toByteArray();
            int l = expectedBytes.length;
            byte[] actualBytes = new byte[l];

            FileInputStream is = new FileInputStream(sourceFile);
            is.read(actualBytes, 0, l);
            String actualByteOrder = bytesToHex(actualBytes);
            assertEquals("The sourceFile does not contain the expected bytes", expectedByteOrder, actualByteOrder);
        }
    }

    private byte[] hexToBytes(String hexString)
    {
        hexString = hexString.replaceAll(" *", "");
        int len = hexString.length() / 2;
        byte[] bytes = new byte[len];
        for (int j=0, i=0; i<len; i++)
        {
            int firstDigit = Character.digit(hexString.charAt(j++), 16);
            int secondDigit = Character.digit(hexString.charAt(j++), 16);
            bytes[i] = (byte)((firstDigit << 4) + secondDigit);
        }
        return bytes;
    }

    private String bytesToHex(byte[] bytes)
    {
        StringBuffer sb = new StringBuffer();
        int len = bytes.length;
        for (int i=0; i<len; i++)
        {
            if (sb.length() > 0)
            {
                sb.append(' ');
            }
            sb.append(Character.forDigit((bytes[i] >> 4) & 0xF, 16));
            sb.append(Character.forDigit((bytes[i] & 0xF), 16));
        }
        return sb.toString();
    }
}
