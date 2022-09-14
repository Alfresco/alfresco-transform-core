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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.alfresco.transform.common.RequestParamMap.PAGE_LIMIT;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_ENCODING;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextToPdfContentTransformerTest
{
    TextToPdfContentTransformer transformer = new TextToPdfContentTransformer();

    @BeforeEach
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
        String expectedByteOrder = "fe ff 00 31 00 20 00 49";
        transformTextAndCheck("UTF-16", true, true, expectedByteOrder);
        transformTextAndCheck("UTF-16", true, true, expectedByteOrder);
        transformTextAndCheck("UTF-16BE", true, true, expectedByteOrder);
        transformTextAndCheck("UTF-16LE", true, true, expectedByteOrder);
    }

    @Test
    public void test2UTF16LittleEndianBomLittleEndianChars() throws Exception
    {
        // 2. BOM indicates LE (ff then fe) + chars appear to be LE (as second byte read tends to be a zero)
        //    Expected with UTF-16. Some systems use BE and other like Windows and Mac used LE
        transformTextAndCheck("UTF-16", false, true, "ff fe 31 00 20 00 49 00");
    }

    @Test
    public void test3UTF16NoBomBigEndianChars() throws Exception
    {
        // 3. No BOM + chars appear to be BE (as first byte read tends to be a zero)
        //    Expected with UTF-16BE
        transformTextAndCheck("UTF-16", true, null, "00 31 00 20 00 49");
    }

    @Test
    public void test4UTF16NoBomLittleEndianChars() throws Exception
    {
        // 4. No BOM + chars appear to be LE (as second byte read tends to be a zero)
        //    Expected with UTF-16LE
        transformTextAndCheck("UTF-16", false, null, "31 00 20 00 49 00");
    }

    @Test
    public void test5UTF16BigEndianBomLittleEndianChars() throws Exception
    {
        // 5. BOM indicates BE (fe then ff) + chars appear to be LE (as second byte read tends to be a zero)
        //    SOMETHING IS WRONG, BUT USE LE!!!!
        transformTextAndCheck("UTF-16", false, false, "fe ff 31 00 20 00 49 00");
    }

    @Test
    public void test6UTF16LittleEndianBomBigEndianChars() throws Exception
    {
        // 6. BOM indicates LE (ff then fe) + chars appear to be BE (as first byte read tends to be a zero)
        //    SOMETHING IS WRONG, BUT USE BE!!!!
        transformTextAndCheck("UTF-16", true, false, "ff fe 00 31 00 20 00 49");
    }

    @Test
    public void testUTF8WithBOM() throws Exception
    {
        transformTextAndCheck("UTF-8", null, true, "ef bb bf 31 20 49 20 6d");
    }

    @Test
    public void testUTF8WithoutBOM() throws Exception
    {
        transformTextAndCheck("UTF-8", null, false, "31 20 49 20 6d 75 73 74");
    }

    /**
     * @param encoding to be used to read the source file
     * @param bigEndian indicates that the file should contain big endian characters, so typically the first byte of
     *                 each char is a zero when using English.
     * @param validBom if not null, the BOM is included. If true it is the one matching bigEndian. If false it is the
     *                 opposite byte order, which really is an error, but we try to recover from it.
     * @param expectedByteOrder The first few bytes of the source file so we can check the test data has been
     *                 correctly created.
     */
    protected void transformTextAndCheck(String encoding, Boolean bigEndian, Boolean validBom,
                                         String expectedByteOrder) throws Exception
    {
        transformTextAndCheckImpl(-1, encoding, bigEndian, validBom, expectedByteOrder);
    }

    protected void transformTextAndCheckPageLength(int pageLimit) throws Exception
    {
        transformTextAndCheckImpl(pageLimit, "UTF-8", null, null, null);
    }

    private void transformTextAndCheckImpl(int pageLimit, String encoding, Boolean bigEndian, Boolean validBom,
                                           String expectedByteOrder) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        String checkText = createTestText(pageLimit, sb);
        String text = sb.toString();

        File sourceFile = File.createTempFile("AlfrescoTestSource_", ".txt");
        writeToFile(sourceFile, text, encoding, bigEndian, validBom);
        checkFileBytes(sourceFile, expectedByteOrder);

        transformTextAndCheck(sourceFile, encoding, checkText, String.valueOf(pageLimit));
    }

    private String createTestText(int pageLimit, StringBuilder sb)
    {
        int pageLength = 32;
        int lines = (pageLength + 10) * ((pageLimit > 0) ? pageLimit : 1);
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
        transformer.transform("text/plain", "application/pdf", parameters, sourceFile, targetFile, null);

        // Read back in the PDF and check it
        PDDocument doc = PDDocument.load(targetFile);
        PDFTextStripper textStripper = new PDFTextStripper();
        StringWriter textWriter = new StringWriter();
        textStripper.writeText(doc, textWriter);
        doc.close();

        String roundTrip = clean(textWriter.toString());

        assertEquals(
            checkText, roundTrip,
            "Incorrect text in PDF when starting from text in " + encoding
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

    private void writeToFile(File file, String content, String encoding, Boolean bigEndian, Boolean validBom) throws Exception
    {
        // If we may have to change the endian or include/exclude the BOM, write initially to a tmp file using
        // UTF-16 which includes the BOM FEFF.
        File originalFile = file;
        if (bigEndian != null)
        {
            file = File.createTempFile("AlfrescoTestTmpSrc_", ".txt");
            encoding = "UTF-16";
        }

        // Use a writer to use the required encoding
        try (OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(file), encoding))
        {
            // Add BOM to UTF-8 file
            if (bigEndian == null && encoding != null && "UTF-8".equals(encoding.toUpperCase()) && validBom != null && validBom)
            {
                ow.append("\ufeff");
            }

            ow.append(content);
        }

        // If we may have to change the endian or include/exclude the BOM, copy the raw bytes to the supplied file
        if (bigEndian != null)
        {
            boolean firstRead = true;
            byte[] bytes = new byte[8192];
            try (InputStream is = new BufferedInputStream(new FileInputStream(file));
                 OutputStream os = new BufferedOutputStream(new FileOutputStream(originalFile)))
            {
                int l;
                int off;
                boolean switchBytes = false;
                do
                {
                    l = is.read(bytes);
                    off = 0;
                    // When we read the first block, change the offset if we don't want the BOM and also work out
                    // if the byte endian need to be switch. The source bytes always start with a standard BOM.
                    if (firstRead)
                    {
                        firstRead = false;
                        boolean actualEndianBytes = bytes[0] == (byte)0xfe; // if true [1] would also be 0xff
                        switchBytes = actualEndianBytes != bigEndian;
                        if (validBom == null)
                        {
                            // Strip the BOM
                            off = 2;
                        }
                        else if (!validBom)
                        {
                            // Reverse the BOM so it does not match the characters!
                            byte aByte = bytes[0];
                            bytes[0] = bytes[1];
                            bytes[1] = aByte;
                        }
                    }
                    int len = l - off;
                    if (len > 0)
                    {
                        if (switchBytes)
                        {
                            // Reverse the byte order of characters including the BOM.
                            for (int i=0; i<l; i+=2)
                            {
                                byte aByte = bytes[i];
                                bytes[i] = bytes[i+1];
                                bytes[i+1] = aByte;
                            }
                        }
                        os.write(bytes, off, len-off);
                    }
                } while (l != -1);
            }
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
            assertEquals(expectedByteOrder, actualByteOrder, "The sourceFile does not contain the expected bytes");
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
