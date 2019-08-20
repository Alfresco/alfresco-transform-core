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

import static java.nio.file.Files.readAllBytes;
import static org.alfresco.transformer.executors.Tika.ARCHIVE;
import static org.alfresco.transformer.executors.Tika.CSV;
import static org.alfresco.transformer.executors.Tika.DOC;
import static org.alfresco.transformer.executors.Tika.DOCX;
import static org.alfresco.transformer.executors.Tika.HTML;
import static org.alfresco.transformer.executors.Tika.MSG;
import static org.alfresco.transformer.executors.Tika.OUTLOOK_MSG;
import static org.alfresco.transformer.executors.Tika.PDF;
import static org.alfresco.transformer.executors.Tika.PDF_BOX;
import static org.alfresco.transformer.executors.Tika.POI;
import static org.alfresco.transformer.executors.Tika.POI_OFFICE;
import static org.alfresco.transformer.executors.Tika.POI_OO_XML;
import static org.alfresco.transformer.executors.Tika.PPTX;
import static org.alfresco.transformer.executors.Tika.TEXT_MINING;
import static org.alfresco.transformer.executors.Tika.TIKA_AUTO;
import static org.alfresco.transformer.executors.Tika.TXT;
import static org.alfresco.transformer.executors.Tika.XHTML;
import static org.alfresco.transformer.executors.Tika.XML;
import static org.alfresco.transformer.executors.Tika.XSLX;
import static org.alfresco.transformer.executors.Tika.ZIP;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_HTML;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENXML_PRESENTATION;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENXML_SPREADSHEET;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENXML_WORDPROCESSING;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OUTLOOK_MSG;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_PDF;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_TEXT_CSV;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_WORD;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_XHTML;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_XML;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_ZIP;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.util.StringUtils.getFilenameExtension;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transformer.executors.RuntimeExec;
import org.alfresco.transformer.executors.TikaJavaExecutor;
import org.alfresco.transformer.model.FileRefEntity;
import org.alfresco.transformer.model.FileRefResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Test the TikaController without a server.
 * Super class includes tests for the AbstractTransformerController.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(TikaController.class)
public class TikaControllerTest extends AbstractTransformerControllerTest
{
    private static final String EXPECTED_XHTML_CONTENT_CONTAINS = "<p>The quick brown fox jumps over the lazy dog</p>";
    private static final String EXPECTED_TEXT_CONTENT_CONTAINS = "The quick brown fox jumps over the lazy dog";
    private static final String EXPECTED_MSG_CONTENT_CONTAINS = "Recipients\n" +
                                                                "\tmark.rogers@alfresco.com; speedy@quick.com; mrquick@nowhere.com\n" +
                                                                "\n" +
                                                                "The quick brown fox jumps over the lazy dogs";
    private static final String EXPECTED_CSV_CONTENT_CONTAINS = "\"The\",\"quick\",\"brown\",\"fox\"";

    @Mock
    private RuntimeExec.ExecutionResult mockExecutionResult;

    @Mock
    private RuntimeExec mockTransformCommand;

    @Mock
    private RuntimeExec mockCheckCommand;

    @SpyBean
    private TikaJavaExecutor javaExecutor;

    @SpyBean
    private TikaController controller;

    private String transform = PDF_BOX;
    private String targetEncoding = "UTF-8";
    private String targetMimetype = MIMETYPE_TEXT_PLAIN;

    @Before
    public void before()
    {
        sourceExtension = "pdf";
        targetExtension = "txt";
    }

    @Override
    protected void mockTransformCommand(String sourceExtension,
        String targetExtension, String sourceMimetype,
        boolean readTargetFileBytes) throws IOException
    {
        this.sourceExtension = sourceExtension;
        this.targetExtension = targetExtension;
        this.sourceMimetype = sourceMimetype;

        expectedOptions = null;
        expectedSourceSuffix = null;
        expectedSourceFileBytes = readTestFile(sourceExtension);
        expectedTargetFileBytes = readTargetFileBytes ? readTestFile(targetExtension) : null;
        sourceFile = new MockMultipartFile("file", "quick." + sourceExtension, sourceMimetype,
            expectedSourceFileBytes);

        when(mockTransformCommand.execute(any(), anyLong())).thenAnswer(
            (Answer<RuntimeExec.ExecutionResult>) invocation -> {
                Map<String, String> actualProperties = invocation.getArgument(0);
                assertEquals("There should be 3 properties", 3, actualProperties.size());

                String actualOptions = actualProperties.get("options");
                String actualSource = actualProperties.get("source");
                String actualTarget = actualProperties.get("target");
                String actualTargetExtension = getFilenameExtension(actualTarget);

                assertNotNull(actualSource);
                assertNotNull(actualTarget);
                if (expectedSourceSuffix != null)
                {
                    assertTrue(
                        "The source file \"" + actualSource + "\" should have ended in \"" + expectedSourceSuffix + "\"",
                        actualSource.endsWith(expectedSourceSuffix));
                    actualSource = actualSource.substring(0,
                        actualSource.length() - expectedSourceSuffix.length());
                }

                assertNotNull(actualOptions);
                if (expectedOptions != null)
                {
                    assertEquals("expectedOptions", expectedOptions, actualOptions);
                }

                Long actualTimeout = invocation.getArgument(1);
                assertNotNull(actualTimeout);
                if (expectedTimeout != null)
                {
                    assertEquals("expectedTimeout", expectedTimeout, actualTimeout);
                }

                // Copy a test file into the target file location if it exists
                int i = actualTarget.lastIndexOf('_');
                if (i >= 0)
                {
                    String testFilename = actualTarget.substring(i + 1);
                    File testFile = getTestFile(testFilename, false);
                    File targetFile = new File(actualTarget);
                    generateTargetFileFromResourceFile(actualTargetExtension, testFile,
                        targetFile);
                }

                // Check the supplied source file has not been changed.
                byte[] actualSourceFileBytes = readAllBytes(new File(actualSource).toPath());
                assertArrayEquals("Source file is not the same", expectedSourceFileBytes,
                    actualSourceFileBytes);

                return mockExecutionResult;
            });

        when(mockExecutionResult.getExitValue()).thenReturn(0);
        when(mockExecutionResult.getStdErr()).thenReturn("STDERROR");
        when(mockExecutionResult.getStdOut()).thenReturn("STDOUT");
    }

    @Override
    protected AbstractTransformerController getController()
    {
        return controller;
    }

    private void transform(String transform, String sourceExtension, String targetExtension,
        String sourceMimetype, String targetMimetype,
        Boolean includeContents, String expectedContentContains) throws Exception
    {
        // We don't use targetFileBytes as some of the transforms contain different date text based on the os being used.
        mockTransformCommand(sourceExtension, targetExtension, sourceMimetype, false);
        this.transform = transform;
        this.targetMimetype = targetMimetype;

        System.out.println("Test " + transform + " " + sourceExtension + " to " + targetExtension);
        MockHttpServletRequestBuilder requestBuilder = includeContents == null
                                                       ? mockMvcRequest("/transform", sourceFile,
            "targetExtension", this.targetExtension)
                                                       : mockMvcRequest("/transform", sourceFile,
            "targetExtension", this.targetExtension, "includeContents", includeContents.toString());
        MvcResult result = mockMvc.perform(requestBuilder)
                                  .andExpect(status().is(OK.value()))
                                  .andExpect(header().string("Content-Disposition",
                                      "attachment; filename*= UTF-8''quick." + this.targetExtension)).
                                      andReturn();
        String content = result.getResponse().getContentAsString();
        assertTrue("The content did not include \"" + expectedContentContains,
            content.contains(expectedContentContains));
    }

    @Override
    // Add extra required parameters to the request.
    protected MockHttpServletRequestBuilder mockMvcRequest(String url, MockMultipartFile sourceFile,
        String... params)
    {
        return super.mockMvcRequest(url, sourceFile, params)
                    .param("transform", transform)
                    .param("targetEncoding", targetEncoding)
                    .param("targetMimetype", targetMimetype);
    }

    @Test
    @Override
    public void simpleTransformTest() throws Exception
    {
        mockTransformCommand(PDF, TXT, MIMETYPE_PDF, true);
        super.simpleTransformTest();
    }

    @Test
    @Override
    public void testDelayTest() throws Exception
    {
        mockTransformCommand(PDF, TXT, MIMETYPE_PDF, true);
        super.testDelayTest();
    }

    @Test
    @Override
    public void noTargetFileTest()
    {
        // Ignore the test in super class as the Tika transforms are real rather than mocked up.
        // It is the mock that returns a zero length file for other transformers, when we supply an invalid targetExtension.
    }

    // --- Super class tests (need modified setup) ---

    @Test
    @Override
    public void dotDotSourceFilenameTest() throws Exception
    {
        mockTransformCommand(PDF, TXT, MIMETYPE_PDF, true);
        super.dotDotSourceFilenameTest();
    }

    @Test
    @Override
    public void noExtensionSourceFilenameTest() throws Exception
    {
        mockTransformCommand(PDF, TXT, MIMETYPE_PDF, true);
        super.noExtensionSourceFilenameTest();
    }

    @Test
    @Override
    public void badSourceFilenameTest() throws Exception
    {
        mockTransformCommand(PDF, TXT, MIMETYPE_PDF, true);
        super.badSourceFilenameTest();
    }

    @Test
    @Override
    public void blankSourceFilenameTest() throws Exception
    {
        mockTransformCommand(PDF, TXT, MIMETYPE_PDF, true);
        super.blankSourceFilenameTest();
    }

    @Test
    @Override
    public void noTargetExtensionTest() throws Exception
    {
        mockTransformCommand(PDF, TXT, MIMETYPE_PDF, true);
        super.noTargetExtensionTest();
    }

    @Test
    @Override
    public void calculateMaxTime() throws Exception
    {
        mockTransformCommand(PDF, TXT, MIMETYPE_PDF, true);
        super.calculateMaxTime();
    }

    // --- General Tika tests ---

    @Test
    public void badEncodingTest() throws Exception
    {
        mockTransformCommand(PDF, TXT, MIMETYPE_PDF, true);
        targetEncoding = "rubbish";
        mockMvc.perform(
            mockMvcRequest("/transform", sourceFile, "targetExtension", targetExtension))
               .andExpect(status().is(INTERNAL_SERVER_ERROR.value()));
    }

    // --- Archive ---

    @Test
    public void zipToTextArchiveTest() throws Exception
    {
        transform(ARCHIVE, ZIP, TXT, MIMETYPE_ZIP, MIMETYPE_TEXT_PLAIN, false,
            "quick.html\n" +
            "\n" +
            "\n" +
            "quick.pdf\n" +
            "\n" +
            "\n");
    }

    @Test
    public void zipToTextIncludeArchiveTest() throws Exception
    {
        transform(ARCHIVE, ZIP, TXT, MIMETYPE_ZIP, MIMETYPE_TEXT_PLAIN, true,
            "quick.html\n" +
            "\n" +
            "\n" +
            "The quick brown fox jumps over the lazy dog\n" +
            "\n" +
            "\n" +
            "\n" +
            "quick.pdf\n" +
            "\n" +
            "\n" +
            "The quick brown fox jumps over the lazy dog" +
            "\n" +
            "\n");
    }

    @Test
    public void zipToTextExcludeArchiveTest() throws Exception
    {
        transform(ARCHIVE, ZIP, TXT, MIMETYPE_ZIP, MIMETYPE_TEXT_PLAIN,
            false, "\n" +
                   "folder/subfolder/quick.jpg\n" +
                   "\n" +
                   "\n" +
                   "quick.doc\n" +
                   "\n" +
                   "\n" +
                   "quick.html\n" +
                   "\n" +
                   "\n" +
                   "quick.pdf\n" +
                   "\n" +
                   "\n" +
                   "quick.txt\n" +
                   "\n" +
                   "\n" +
                   "quick.xml\n" +
                   "\n");
    }

    // --- OutlookMsg ---

    @Test
    public void msgToTxtOutlookMsgTest() throws Exception
    {
        transform(OUTLOOK_MSG, MSG, TXT, MIMETYPE_OUTLOOK_MSG, MIMETYPE_TEXT_PLAIN, null,
            EXPECTED_MSG_CONTENT_CONTAINS);
    }

    // --- PdfBox ---

    @Test
    public void pdfToTxtPdfBoxTest() throws Exception
    {
        transform(PDF_BOX, PDF, TXT, MIMETYPE_PDF, MIMETYPE_TEXT_PLAIN, null,
            EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    @Test
    public void pdfToCsvPdfBoxTest() throws Exception
    {
        transform(PDF_BOX, PDF, CSV, MIMETYPE_PDF, MIMETYPE_TEXT_CSV, null,
            EXPECTED_TEXT_CONTENT_CONTAINS); // Yes it is just text
    }

    @Test
    public void pdfToXmlPdfBoxTest() throws Exception
    {
        transform(PDF_BOX, PDF, XML, MIMETYPE_PDF, MIMETYPE_XML, null,
            EXPECTED_XHTML_CONTENT_CONTAINS); // Yes it is just XHTML
    }

    @Test
    public void pdfToXhtmlPdfBoxTest() throws Exception
    {
        transform(PDF_BOX, PDF, XHTML, MIMETYPE_PDF, MIMETYPE_XHTML, null,
            EXPECTED_XHTML_CONTENT_CONTAINS);
    }

    @Test
    public void pdfToHtmlPdfBoxTest() throws Exception
    {
        transform(PDF_BOX, PDF, HTML, MIMETYPE_PDF, MIMETYPE_HTML, null,
            EXPECTED_XHTML_CONTENT_CONTAINS); // Yes it is just XHTML
    }

    // --- Office ---

    @Test
    public void msgToTxtOfficeTest() throws Exception
    {
        transform(POI_OFFICE, MSG, TXT, MIMETYPE_OUTLOOK_MSG, MIMETYPE_TEXT_PLAIN, null,
            EXPECTED_MSG_CONTENT_CONTAINS);
    }

    @Test
    public void docToTxtOfficeTest() throws Exception
    {
        transform(POI_OFFICE, DOC, TXT, MIMETYPE_WORD, MIMETYPE_TEXT_PLAIN, null,
            EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    // --- Poi ---

    @Test
    public void xslxToCsvPoiTest() throws Exception
    {
        transform(POI, XSLX, CSV, MIMETYPE_OPENXML_SPREADSHEET, MIMETYPE_TEXT_CSV, null,
            EXPECTED_CSV_CONTENT_CONTAINS);
    }

    // --- OOXML ---

    @Test
    public void docxToTxtOoXmlTest() throws Exception
    {
        transform(POI_OO_XML, DOCX, TXT, MIMETYPE_OPENXML_WORDPROCESSING, MIMETYPE_TEXT_PLAIN, null,
            EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    @Test
    public void pptxToTxtOoXmlTest() throws Exception
    {
        transform(POI_OO_XML, PPTX, TXT, MIMETYPE_OPENXML_PRESENTATION, MIMETYPE_TEXT_PLAIN, null,
            EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    // --- TikaAuto ---

    @Test
    public void ppxtToTxtTikaAutoTest() throws Exception
    {
        transform(TIKA_AUTO, PPTX, TXT, MIMETYPE_OPENXML_PRESENTATION, MIMETYPE_TEXT_PLAIN, null,
            EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    @Test
    public void doctToTxtTikaAutoTest() throws Exception
    {
        transform(TIKA_AUTO, DOCX, TXT, MIMETYPE_OPENXML_WORDPROCESSING, MIMETYPE_TEXT_PLAIN, null,
            EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    // --- TextMining ---

    @Test
    public void docToTxtTextMiningTest() throws Exception
    {
        transform(TEXT_MINING, DOC, TXT, MIMETYPE_WORD, MIMETYPE_TEXT_PLAIN, null,
            EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    @Test
    public void pdfToTxtExtractBookmarksTest() throws Exception
    {
        mockTransformCommand(PDF, TXT, MIMETYPE_PDF, true);
        mockMvc.perform(
            mockMvcRequest("/transform", sourceFile, "targetExtension", targetExtension).param(
                "notExtractBookmarksText", "true"))
               .andExpect(status().is(OK.value()))
               .andExpect(header().string("Content-Disposition",
                   "attachment; filename*= UTF-8''quick." + targetExtension));
    }

    @Override
    protected void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest)
    {
        transformRequest.setSourceExtension(sourceExtension);
        transformRequest.setTargetExtension(targetExtension);
        transformRequest.setSourceMediaType(APPLICATION_PDF_VALUE);
        transformRequest.setTargetMediaType(TEXT_PLAIN_VALUE);
        transformRequest.getTransformRequestOptions().put("transform", "PdfBox");
        transformRequest.getTransformRequestOptions().put("targetMimetype", TEXT_PLAIN_VALUE);
        transformRequest.getTransformRequestOptions().put("targetEncoding", "UTF-8");
    }

    @Test
    public void testPojoTransform() throws Exception
    {
        // Files
        String sourceFileRef = UUID.randomUUID().toString();
        File sourceFile = getTestFile("quick." + sourceExtension, true);
        String targetFileRef = UUID.randomUUID().toString();

        // Transformation Request POJO
        TransformRequest transformRequest = new TransformRequest();
        transformRequest.setRequestId("1");
        transformRequest.setSchema(1);
        transformRequest.setClientData("Alfresco Digital Business Platform");
        transformRequest.setTransformRequestOptions(new HashMap<>());
        transformRequest.setSourceReference(sourceFileRef);
        transformRequest.setSourceExtension(sourceExtension);
        transformRequest.setSourceSize(sourceFile.length());
        transformRequest.setTargetExtension(targetExtension);

        // HTTP Request
        HttpHeaders headers = new HttpHeaders();
        headers.set(CONTENT_DISPOSITION, "attachment; filename=quick." + sourceExtension);
        ResponseEntity<Resource> response = new ResponseEntity<>(new FileSystemResource(
            sourceFile), headers, OK);

        when(alfrescoSharedFileStoreClient.retrieveFile(sourceFileRef)).thenReturn(response);
        when(alfrescoSharedFileStoreClient.saveFile(any()))
            .thenReturn(new FileRefResponse(new FileRefEntity(targetFileRef)));
        when(mockExecutionResult.getExitValue()).thenReturn(0);

        // Update the Transformation Request with any specific params before sending it
        updateTransformRequestWithSpecificOptions(transformRequest);

        // Serialize and call the transformer
        String tr = objectMapper.writeValueAsString(transformRequest);
        String transformationReplyAsString = mockMvc
            .perform(MockMvcRequestBuilders
                .post("/transform")
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .content(tr))
            .andExpect(status().is(CREATED.value()))
            .andReturn().getResponse().getContentAsString();

        TransformReply transformReply = objectMapper.readValue(transformationReplyAsString,
            TransformReply.class);

        // Assert the reply
        assertEquals(transformRequest.getRequestId(), transformReply.getRequestId());
        assertEquals(transformRequest.getClientData(), transformReply.getClientData());
        assertEquals(transformRequest.getSchema(), transformReply.getSchema());
    }
}
