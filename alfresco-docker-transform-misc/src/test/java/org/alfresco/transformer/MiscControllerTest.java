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
package org.alfresco.transformer;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transformer.transformers.SelectingTransformer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IWORK_KEYNOTE;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IWORK_NUMBERS;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IWORK_PAGES;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(MiscController.class)
public class MiscControllerTest extends AbstractTransformerControllerTest
{

    @SpyBean
    private MiscController controller;

    @SpyBean
    SelectingTransformer transformer;

    private String sourceEncoding = "UTF-8";
    private String targetEncoding = "UTF-8";
    private String targetMimetype = MIMETYPE_TEXT_PLAIN;

    @Before
    public void before() throws Exception
    {
        sourceMimetype = MIMETYPE_HTML;
        sourceExtension = "html";
        targetExtension = "txt";
        expectedOptions = null;
        expectedSourceSuffix = null;
        expectedSourceFileBytes = readTestFile(sourceExtension);
        expectedTargetFileBytes = readTestFile(targetExtension);
        //expectedTargetFileBytes = null;
        sourceFile = new MockMultipartFile("file", "quick." + sourceExtension, sourceMimetype, expectedSourceFileBytes);

    }

    @Override
    protected void mockTransformCommand(String sourceExtension, String targetExtension, String sourceMimetype, boolean readTargetFileBytes) throws IOException
    {
    }

    @Override
    protected AbstractTransformerController getController()
    {
        return controller;
    }

    @Override
    protected void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest)
    {
    }

    @Override
    // Add extra required parameters to the request.
    protected MockHttpServletRequestBuilder mockMvcRequest(String url, MockMultipartFile sourceFile, String... params)
    {
        return super.mockMvcRequest(url, sourceFile, params)
                .param("targetEncoding", targetEncoding)
                .param("sourceEncoding", sourceEncoding)
                .param("targetMimetype", targetMimetype)
                .param("sourceMimetype", sourceMimetype);
    }

    @Test
    @Override
    public void noTargetFileTest()
    {
        // Ignore the test in super class as the Misc transforms are real rather than mocked up.
        // It is the mock that returns a zero length file for other transformers, when we supply an invalid targetExtension.
    }

    @Test
    public void testHTMLtoString() throws Exception
    {
        final String NEWLINE = System.getProperty ("line.separator");
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

        textBasedTransform("html",
                "UTF-8",
                MIMETYPE_HTML,
                "txt",
                MIMETYPE_TEXT_PLAIN,
                "UTF-8",
                expected.getBytes(),
                expected);
    }

    @Test
    public void testStringtoString() throws Exception
    {
        String expected = null;
        byte[] content = null;
        try
        {
            content = "azAz10!�$%^&*()\t\r\n".getBytes("UTF-8");
            expected = new String(content, "MacDingbat");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new AlfrescoRuntimeException("Encoding not recognised", e);
        }

        textBasedTransform("txt",
                "MacDingbat",
                MIMETYPE_TEXT_PLAIN,
                "txt",
                MIMETYPE_TEXT_PLAIN,
                "UTF-8",
                content,
                expected);
    }

    @Test
    public void testAppleIWorksPages() throws Exception
    {
        imageBasedTransform("pages", MIMETYPE_IWORK_PAGES, MIMETYPE_IMAGE_JPEG, "jpeg");
    }

    @Test
    public void testAppleIWorksNumbers() throws Exception
    {
        imageBasedTransform("numbers", MIMETYPE_IWORK_NUMBERS, MIMETYPE_IMAGE_JPEG, "jpeg");
    }

    @Test
    public void testAppleIWorksKey() throws Exception
    {
        imageBasedTransform("key", MIMETYPE_IWORK_KEYNOTE, MIMETYPE_IMAGE_JPEG, "jpeg");
    }

    private void imageBasedTransform(String sourceExtension, String sourceMimetype, String targetMimetype, String targetExtension) throws Exception
    {
        MockMultipartFile sourceFilex = new MockMultipartFile("file", "test_file." + sourceExtension, sourceMimetype, readTestFile(sourceExtension));

        MockHttpServletRequestBuilder requestBuilder = super.mockMvcRequest("/transform", sourceFilex)
                .param("targetExtension", "jpeg")
                .param("targetMimetype", targetMimetype)
                .param("sourceMimetype", sourceMimetype);

        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().is(OK.value()))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''test_file." + targetExtension))
                .andReturn();
        assertTrue("Expected image content but content is empty.",result.getResponse().getContentLengthLong() > 0L);
    }


    private void textBasedTransform(String sourceExtension,
                           String sourceEncoding,
                           String sourceMimetype,
                           String targetExtension,
                           String targetMimetype,
                           String targetEncoding,
                           byte[] content,
                           String expectedContentContains) throws Exception
    {
        MockMultipartFile sourceFilex = new MockMultipartFile("file", "test_file." + sourceExtension, sourceMimetype, content);

        MockHttpServletRequestBuilder requestBuilder = super.mockMvcRequest("/transform", sourceFilex)
                .param("targetExtension", targetExtension)
                .param("targetEncoding", targetEncoding)
                .param("targetMimetype", targetMimetype)
                .param("sourceEncoding", sourceEncoding)
                .param("sourceMimetype", sourceMimetype);


        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().is(OK.value()))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= "+targetEncoding+"''test_file." + targetExtension)).
                        andReturn();
        String contentResult = new String(result.getResponse().getContentAsByteArray(), targetEncoding);
        assertTrue("The content did not include \""+expectedContentContains, contentResult.contains(expectedContentContains));
    }
}