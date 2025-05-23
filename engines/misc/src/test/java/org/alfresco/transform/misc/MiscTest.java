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
package org.alfresco.transform.misc;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IWORK_KEYNOTE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IWORK_NUMBERS;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_WORDPROCESSING;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_RFC822;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transform.common.RequestParamMap.TARGET_MIMETYPE;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import org.alfresco.transform.base.AbstractBaseTest;

/**
 * Test Misc. Includes calling the 3rd party libraries.
 */
public class MiscTest extends AbstractBaseTest
{
    protected final String sourceEncoding = "UTF-8";
    protected final String targetEncoding = "UTF-8";
    protected final String targetMimetype = MIMETYPE_TEXT_PLAIN;

    @BeforeEach
    public void before() throws Exception
    {
        sourceMimetype = MIMETYPE_HTML;
        sourceExtension = "html";
        targetExtension = "txt";
        expectedOptions = null;
        expectedSourceSuffix = null;
        sourceFileBytes = readTestFile(sourceExtension);
        expectedTargetFileBytes = Files.readAllBytes(getTestFile("quick3." + targetExtension, true).toPath());
        sourceFile = new MockMultipartFile("file", "quick." + sourceExtension, sourceMimetype, sourceFileBytes);
    }

    @Override
    protected MockHttpServletRequestBuilder mockMvcRequest(String url, MockMultipartFile sourceFile, String... params)
    {
        final MockHttpServletRequestBuilder builder = super.mockMvcRequest(url, sourceFile, params)
                .param("sourceEncoding", sourceEncoding)
                .param("targetMimetype", targetMimetype)
                .param("sourceMimetype", sourceMimetype);

        // Only the 'string' transformer should have the targetEncoding.
        if (!"message/rfc822".equals(sourceMimetype) && !"text/html".equals(sourceMimetype))
        {
            builder.param("targetEncoding", targetEncoding);
        }
        return builder;
    }

    /**
     * Test transforming a valid eml file to text
     */
    @Test
    public void testRFC822ToText() throws Exception
    {
        String expected = "Gym class featuring a brown fox and lazy dog";
        MvcResult result = sendRequest("eml",
                null,
                MIMETYPE_RFC822,
                "txt",
                MIMETYPE_TEXT_PLAIN,
                null,
                null,
                null,
                readTestFile("eml"));
        assertTrue(result.getResponse().getContentAsString().contains(expected),
                "Content from eml transform didn't contain expected value. ");
    }

    /**
     * Test transforming a non-ascii eml file to text
     */
    @Test
    public void testNonAsciiRFC822ToText() throws Exception
    {
        String expected = "El r\u00E1pido zorro marr\u00F3n salta sobre el perro perezoso";
        MvcResult result = sendRequest("eml",
                null,
                MIMETYPE_RFC822,
                "txt",
                MIMETYPE_TEXT_PLAIN,
                null,
                null,
                null, readTestFile("spanish.eml"));

        String contentResult = new String(result.getResponse().getContentAsByteArray(), UTF_8);
        assertTrue(contentResult.contains(expected),
                "Content from eml transform didn't contain expected value. ");
    }

    /**
     * Test transforming a valid eml with an attachment to text; attachment should be ignored
     */
    @Test
    public void testRFC822WithAttachmentToText() throws Exception
    {
        String expected = "Mail with attachment content";
        String notExpected = "File attachment content";
        MvcResult result = sendRequest("eml",
                null,
                MIMETYPE_RFC822,
                "txt",
                MIMETYPE_TEXT_PLAIN,
                null,
                null,
                null,
                readTestFile("attachment.eml"));
        assertTrue(result.getResponse().getContentAsString().contains(expected),
                "Content from eml transform didn't contain expected value. ");
        assertFalse(result.getResponse().getContentAsString().contains(notExpected));
    }

    /**
     * Test transforming a valid eml with minetype multipart/alternative to text
     */
    @Test
    public void testRFC822AlternativeToText() throws Exception
    {
        String expected = "alternative plain text";
        MvcResult result = sendRequest("eml",
                null,
                MIMETYPE_RFC822,
                "txt",
                MIMETYPE_TEXT_PLAIN,
                null,
                null,
                null,
                readTestFile("alternative.eml"));
        assertTrue(result.getResponse().getContentAsString().contains(expected),
                "Content from eml transform didn't contain expected value. ");
    }

    /**
     * Test transforming a valid eml with nested mimetype multipart/alternative to text
     */
    @Test
    public void testRFC822NestedAlternativeToText() throws Exception
    {
        String expected = "nested alternative plain text";
        MvcResult result = sendRequest("eml",
                null,
                MIMETYPE_RFC822,
                "txt",
                MIMETYPE_TEXT_PLAIN,
                null,
                null,
                null,
                readTestFile("nested.alternative.eml"));
        assertTrue(result.getResponse().getContentAsString().contains(expected),
                "Content from eml transform didn't contain expected value. ");
    }

    /**
     * Test extracting default metadata from a valid eml file
     */
    @Test
    public void testExtractMetadataRFC822() throws Exception
    {
        String expected = "{" +
                "\"{http://www.alfresco.org/model/content/1.0}addressee\":\"Nevin Nollop <nevin.nollop@gmail.com>\"," +
                "\"{http://www.alfresco.org/model/content/1.0}addressees\":\"Nevin Nollop <nevinn@alfresco.com>\"," +
                "\"{http://www.alfresco.org/model/content/1.0}description\":\"The quick brown fox jumps over the lazy dog\"," +
                "\"{http://www.alfresco.org/model/content/1.0}originator\":\"Nevin Nollop <nevin.nollop@alfresco.com>\"," +
                "\"{http://www.alfresco.org/model/content/1.0}sentdate\":1086351802000," +
                "\"{http://www.alfresco.org/model/content/1.0}subjectline\":\"The quick brown fox jumps over the lazy dog\"," +
                "\"{http://www.alfresco.org/model/content/1.0}title\":\"The quick brown fox jumps over the lazy dog\"," +
                "\"{http://www.alfresco.org/model/imap/1.0}dateSent\":1086351802000," +
                "\"{http://www.alfresco.org/model/imap/1.0}messageCc\":\"Nevin Nollop <nevinn@alfresco.com>\"," +
                "\"{http://www.alfresco.org/model/imap/1.0}messageFrom\":\"Nevin Nollop <nevin.nollop@alfresco.com>\"," +
                "\"{http://www.alfresco.org/model/imap/1.0}messageId\":\"<20040604122322.GV1905@phoenix.home>\"," +
                "\"{http://www.alfresco.org/model/imap/1.0}messageSubject\":\"The quick brown fox jumps over the lazy dog\"," +
                "\"{http://www.alfresco.org/model/imap/1.0}messageTo\":\"Nevin Nollop <nevin.nollop@gmail.com>\"" +
                "}";
        MvcResult result = sendRequest("eml",
                null,
                MIMETYPE_RFC822,
                "json",
                "alfresco-metadata-extract",
                null,
                null,
                null,
                readTestFile("eml"));
        String metadata = result.getResponse().getContentAsString();
        assertEquals(expected, metadata, "Metadata extract");
    }

    /**
     * Test extracting metadata specified in an option from a valid eml file
     */
    @Test
    public void testExtractMetadataOptionRFC822() throws Exception
    {
        // {"messageSubject":["{http://www.alfresco.org/model/imap/1.0}messageSubject","{http://www.alfresco.org/model/content/1.0}subjectline","{http://www.alfresco.org/model/content/1.0}description","{http://www.alfresco.org/model/content/1.0}title"],"Thread-Index":["{http://www.alfresco.org/model/imap/1.0}threadIndex"],"messageTo":["{http://www.alfresco.org/model/imap/1.0}messageTo","{http://www.alfresco.org/model/content/1.0}addressee"],"messageSent":["{http://www.alfresco.org/model/content/1.0}sentdate","{http://www.alfresco.org/model/imap/1.0}dateSent"],"Message-ID":["{http://www.alfresco.org/model/imap/1.0}messageId"],"messageCc":["{http://www.alfresco.org/model/imap/1.0}messageCc","{http://www.alfresco.org/model/content/1.0}addressees"],"messageReceived":["{http://www.alfresco.org/model/imap/1.0}dateReceived"],"messageFrom":["{http://www.alfresco.org/model/imap/1.0}messageFrom","{http://www.alfresco.org/model/content/1.0}originator"]}
        String extractMapping = "{\"messageSubject\":[" +
                "\"{http://www.alfresco.org/model/imap/1.0}messageSubject\"," +
                "\"{http://www.alfresco.org/model/content/1.0}title\"]," +
                "\"Thread-Index\":[" +
                "\"{http://www.alfresco.org/model/imap/1.0}threadIndex\"]," +
                "\"messageFrom\":[" +
                "\"{http://www.alfresco.org/model/dod5015/1.0}dodProp1\"]}\n";
        String expected = "{\"{http://www.alfresco.org/model/content/1.0}title\":\"The quick brown fox jumps over the lazy dog\"," +
                "\"{http://www.alfresco.org/model/dod5015/1.0}dodProp1\":\"Nevin Nollop <nevin.nollop@alfresco.com>\"," +
                "\"{http://www.alfresco.org/model/imap/1.0}messageSubject\":\"The quick brown fox jumps over the lazy dog\"}";
        MvcResult result = sendRequest("eml",
                null,
                MIMETYPE_RFC822,
                "json",
                "alfresco-metadata-extract",
                null,
                null,
                extractMapping,
                readTestFile("eml"));
        String metadata = result.getResponse().getContentAsString();
        assertEquals(expected, metadata, "Option metadata extract");
    }

    /**
     * Test transforming a valid eml with a html part containing html special characters to text
     */
    @Test
    public void testHtmlSpecialCharsToText() throws Exception
    {
        String expected = "&nbsp;";
        MvcResult result = sendRequest("eml",
                null,
                MIMETYPE_RFC822,
                "txt",
                MIMETYPE_TEXT_PLAIN,
                null,
                null,
                null,
                readTestFile("htmlChars.eml"));
        assertFalse(result.getResponse().getContentAsString().contains(expected));
    }

    @Test
    public void testHTMLtoString() throws Exception
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
        final String expected = TITLE + " " + TEXT_P1 + " " + TEXT_P2 + " " + TEXT_P3;

        MvcResult result = sendRequest("html",
                "UTF-8",
                MIMETYPE_HTML,
                "txt",
                MIMETYPE_TEXT_PLAIN,
                null,
                null,
                null,
                expected.getBytes());

        String contentResult = new String(result.getResponse().getContentAsByteArray(),
                targetEncoding);
        assertTrue(contentResult.contains(expected), "The content did not include \"" + expected);
    }

    @Test
    public void testStringToString() throws Exception
    {
        String expected;
        byte[] content;
        try
        {
            content = "azAz10!�$%^&*()\t\r\n".getBytes(UTF_8);
            expected = new String(content, "MacDingbat");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("Encoding not recognised", e);
        }

        MvcResult result = sendRequest("txt",
                "MacDingbat",
                MIMETYPE_TEXT_PLAIN,
                "txt",
                MIMETYPE_TEXT_PLAIN,
                "UTF-8",
                null,
                null,
                content);

        String contentResult = new String(result.getResponse().getContentAsByteArray(),
                targetEncoding);
        assertTrue(contentResult.contains(expected), "The content did not include \"" + expected);
    }

    @Test
    public void testEmptyTextFileReturnsEmptyFile() throws Exception
    {
        // Use empty content to create an empty source file
        byte[] content = new byte[0];

        MvcResult result = sendRequest("txt",
                "UTF-8",
                MIMETYPE_TEXT_PLAIN,
                "txt",
                MIMETYPE_TEXT_PLAIN,
                "UTF-8",
                null,
                null,
                content);

        assertEquals(0, result.getResponse().getContentLength(),
                "Returned content should be empty for an empty source file");
    }

    @Test
    public void textToPdf() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++)
        {
            sb.append(Integer.toString(i));
            sb.append(" I must not talk in class or feed my homework to my cat.\n");
        }
        sb.append("\nBart\n");
        String expected = sb.toString();

        MvcResult result = sendRequest("txt",
                "UTF-8",
                MIMETYPE_TEXT_PLAIN,
                "pdf",
                MIMETYPE_PDF,
                null,
                "1",
                null,
                expected.getBytes());

        // Read back in the PDF and check it
        PDDocument doc = PDDocument.load(result.getResponse().getContentAsByteArray());
        PDFTextStripper textStripper = new PDFTextStripper();
        StringWriter textWriter = new StringWriter();
        textStripper.writeText(doc, textWriter);
        doc.close();

        expected = clean(expected);
        String actual = clean(textWriter.toString());

        assertEquals(expected, actual, "The content did not match.");
    }

    @Test
    public void testAppleIWorksPages() throws Exception
    {
        MvcResult result = sendRequest("numbers", null, MIMETYPE_IWORK_NUMBERS,
                "jpeg", MIMETYPE_IMAGE_JPEG, null, null, null, readTestFile("pages"));
        assertTrue(result.getResponse().getContentAsByteArray().length > 0L,
                "Expected image content but content is empty.");
    }

    @Test
    public void testAppleIWorksNumbers() throws Exception
    {
        MvcResult result = sendRequest("numbers", null, MIMETYPE_IWORK_NUMBERS,
                "jpeg", MIMETYPE_IMAGE_JPEG, null, null, null, readTestFile("numbers"));
        assertTrue(result.getResponse().getContentAsByteArray().length > 0L,
                "Expected image content but content is empty.");
    }

    @Test
    public void testAppleIWorksKey() throws Exception
    {
        MvcResult result = sendRequest("key", null, MIMETYPE_IWORK_KEYNOTE,
                "jpeg", MIMETYPE_IMAGE_JPEG, null, null, null, readTestFile("key"));
        assertTrue(result.getResponse().getContentAsByteArray().length > 0L,
                "Expected image content but content is empty.");
    }

    // @Test
    // TODO Doesn't work with java 11, enable when fixed
    public void testOOXML() throws Exception
    {
        MvcResult result = sendRequest("docx", null, MIMETYPE_OPENXML_WORDPROCESSING,
                "jpeg", MIMETYPE_IMAGE_JPEG, null, null, null, readTestFile("docx"));
        assertTrue(result.getResponse().getContentAsByteArray().length > 0L,
                "Expected image content but content is empty.");
    }

    private MvcResult sendRequest(String sourceExtension,
            String sourceEncoding,
            String sourceMimetype,
            String targetExtension,
            String targetMimetype,
            String targetEncoding,
            String pageLimit,
            String extractMapping,
            byte[] content) throws Exception
    {
        final MockMultipartFile sourceFile = new MockMultipartFile("file",
                "test_file." + sourceExtension, sourceMimetype, content);

        final MockHttpServletRequestBuilder requestBuilder = super.mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile)
                .param(TARGET_MIMETYPE, targetMimetype)
                .param(SOURCE_MIMETYPE, sourceMimetype);

        // SourceEncoding is available in the options but is not used to select the transformer as it is a known
        // like the source mimetype.
        if (sourceEncoding != null)
        {
            requestBuilder.param("sourceEncoding", sourceEncoding);
        }
        if (targetEncoding != null)
        {
            requestBuilder.param("targetEncoding", targetEncoding);
        }
        if (pageLimit != null)
        {
            requestBuilder.param("pageLimit", pageLimit);
        }
        if (extractMapping != null)
        {
            requestBuilder.param("extractMapping", extractMapping);
        }

        return mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=" +
                                (targetEncoding == null ? "UTF-8" : targetEncoding) +
                                "''transform." + targetExtension))
                .andReturn();
    }

    private String clean(String text)
    {
        text = text.replaceAll("\\s+\\r", "");
        text = text.replaceAll("\\s+\\n", "");
        text = text.replaceAll("\\r", "");
        text = text.replaceAll("\\n", "");
        return text;
    }

    @Test
    @Override
    public void queueTransformRequestUsingDirectAccessUrlTest() throws Exception
    {
        super.targetMimetype = this.targetMimetype;
        super.queueTransformRequestUsingDirectAccessUrlTest();
    }
}
