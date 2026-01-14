/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2026 Alfresco Software Limited
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
package org.alfresco.transform.tika;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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

import static org.alfresco.transform.base.html.OptionsHelper.getOptionNames;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_METADATA_EMBED;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_PRESENTATION;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_SPREADSHEET;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_WORDPROCESSING;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OUTLOOK_MSG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_CSV;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_WORD;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_XHTML;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_XML;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_ZIP;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.alfresco.transform.common.RequestParamMap.INCLUDE_CONTENTS;
import static org.alfresco.transform.common.RequestParamMap.NOT_EXTRACT_BOOKMARKS_TEXT;
import static org.alfresco.transform.tika.transformers.Tika.ARCHIVE;
import static org.alfresco.transform.tika.transformers.Tika.CSV;
import static org.alfresco.transform.tika.transformers.Tika.DOC;
import static org.alfresco.transform.tika.transformers.Tika.DOCX;
import static org.alfresco.transform.tika.transformers.Tika.HTML;
import static org.alfresco.transform.tika.transformers.Tika.MSG;
import static org.alfresco.transform.tika.transformers.Tika.OFFICE;
import static org.alfresco.transform.tika.transformers.Tika.OOXML;
import static org.alfresco.transform.tika.transformers.Tika.OUTLOOK_MSG;
import static org.alfresco.transform.tika.transformers.Tika.PDF;
import static org.alfresco.transform.tika.transformers.Tika.PDF_BOX;
import static org.alfresco.transform.tika.transformers.Tika.POI;
import static org.alfresco.transform.tika.transformers.Tika.PPTX;
import static org.alfresco.transform.tika.transformers.Tika.TEXT_MINING;
import static org.alfresco.transform.tika.transformers.Tika.TIKA_AUTO;
import static org.alfresco.transform.tika.transformers.Tika.TXT;
import static org.alfresco.transform.tika.transformers.Tika.XHTML;
import static org.alfresco.transform.tika.transformers.Tika.XLSX;
import static org.alfresco.transform.tika.transformers.Tika.XML;
import static org.alfresco.transform.tika.transformers.Tika.ZIP;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.google.common.collect.ImmutableSet;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import org.alfresco.transform.base.AbstractBaseTest;
import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.model.FileRefEntity;
import org.alfresco.transform.base.model.FileRefResponse;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;

/**
 * Test Tika.
 */
public class TikaTest extends AbstractBaseTest
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

    private String targetEncoding = "UTF-8";
    private String targetMimetype = MIMETYPE_TEXT_PLAIN;

    @BeforeEach
    public void before() throws Exception
    {
        sourceExtension = "pdf";
        targetExtension = "txt";
        sourceMimetype = MIMETYPE_PDF;
        targetMimetype = MIMETYPE_TEXT_PLAIN;
        mockTransformCommand(PDF, TXT, MIMETYPE_PDF, true);
    }

    @Override
    protected void mockTransformCommand(String sourceExtension,
            String targetExtension, String sourceMimetype,
            boolean readTargetFileBytes) throws IOException
    {
        // Tika transform is not mocked. It is run for real.

        this.sourceExtension = sourceExtension;
        this.targetExtension = targetExtension;
        this.sourceMimetype = sourceMimetype;

        expectedOptions = null;
        expectedSourceSuffix = null;
        sourceFileBytes = readTestFile(sourceExtension);
        expectedTargetFileBytes = readTargetFileBytes ? readTestFile(targetExtension) : null;
        sourceFile = new MockMultipartFile("file", "quick." + sourceExtension, sourceMimetype, sourceFileBytes);

        lenient().when(mockExecutionResult.getExitValue()).thenReturn(0);
        lenient().when(mockExecutionResult.getStdErr()).thenReturn("STDERROR");
        lenient().when(mockExecutionResult.getStdOut()).thenReturn("STDOUT");
    }

    private void transform(String transform, String sourceExtension, String targetExtension,
            String sourceMimetype, String targetMimetype,
            Boolean includeContents, String expectedContentContains) throws Exception
    {
        // We don't use targetFileBytes as some of the transforms contain different date text based on the os being used.
        mockTransformCommand(sourceExtension, targetExtension, sourceMimetype, false);
        this.targetMimetype = targetMimetype;

        System.out.println("Test " + transform + " " + sourceExtension + " to " + targetExtension);
        MockMultipartHttpServletRequestBuilder requestBuilder = includeContents == null
                ? mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile,
                        "targetExtension", this.targetExtension)
                : mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile,
                        "targetExtension", this.targetExtension, INCLUDE_CONTENTS, includeContents.toString());
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().is(OK.value()))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''transform." + this.targetExtension))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains(expectedContentContains),
                "The content did not include \"" + expectedContentContains);
    }

    @Override
    // Add extra required parameters to the request.
    protected MockMultipartHttpServletRequestBuilder mockMvcRequest(String url, MockMultipartFile sourceFile, String... params)
    {
        return super.mockMvcRequest(url, sourceFile, params)
                .param("targetEncoding", targetEncoding)
                .param("targetMimetype", targetMimetype)
                .param("sourceMimetype", sourceMimetype);
    }

    @Test
    public void badEncodingTest() throws Exception
    {
        mockTransformCommand(PDF, TXT, MIMETYPE_PDF, true);
        targetEncoding = "rubbish";
        mockMvc.perform(
                mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile, "targetExtension", targetExtension))
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
        transform(OFFICE, MSG, TXT, MIMETYPE_OUTLOOK_MSG, MIMETYPE_TEXT_PLAIN, null,
                EXPECTED_MSG_CONTENT_CONTAINS);
    }

    @Test
    public void docToTxtOfficeTest() throws Exception
    {
        transform(OFFICE, DOC, TXT, MIMETYPE_WORD, MIMETYPE_TEXT_PLAIN, null,
                EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    // --- Poi ---

    @Test
    public void xslxToCsvPoiTest() throws Exception
    {
        transform(POI, XLSX, CSV, MIMETYPE_OPENXML_SPREADSHEET, MIMETYPE_TEXT_CSV, null,
                EXPECTED_CSV_CONTENT_CONTAINS);
    }

    // --- OOXML ---

    @Test
    public void docxToTxtOoXmlTest() throws Exception
    {
        transform(OOXML, DOCX, TXT, MIMETYPE_OPENXML_WORDPROCESSING, MIMETYPE_TEXT_PLAIN, null,
                EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    @Test
    public void pptxToTxtOoXmlTest() throws Exception
    {
        transform(OOXML, PPTX, TXT, MIMETYPE_OPENXML_PRESENTATION, MIMETYPE_TEXT_PLAIN, null,
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
    public void xlsxEmbedTest() throws Exception
    {
        mockTransformCommand(XLSX, XLSX, MIMETYPE_OPENXML_SPREADSHEET, false);

        String metadata = "{\"{http://www.alfresco.org/model/content/1.0}author\":\"author1\"," +
                "\"{http://www.alfresco.org/model/content/1.0}title\":\"title1\"," +
                "\"{http://www.alfresco.org/model/content/1.0}description\":[\"desc1\",\"desc2\"]," +
                "\"{http://www.alfresco.org/model/content/1.0}created\":\"created1\"}";

        MockMultipartHttpServletRequestBuilder requestBuilder = super.mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile,
                "targetExtension", XLSX,
                "metadata", metadata,
                "targetMimetype", MIMETYPE_METADATA_EMBED,
                "sourceMimetype", MIMETYPE_OPENXML_SPREADSHEET);

        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().is(OK.value()))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''transform." + targetExtension))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        POIXMLProperties props = workbook.getProperties();
        POIXMLProperties.CoreProperties coreProp = props.getCoreProperties();
        POIXMLProperties.CustomProperties custProp = props.getCustomProperties();

        assertEquals("author1", coreProp.getCreator());
        assertEquals("title1", coreProp.getTitle());
        assertEquals("desc1, desc2", coreProp.getDescription()); // multi value
        assertEquals("created1", custProp.getProperty("created").getLpwstr());
    }

    @Test
    public void pdfToTxtExtractBookmarksTest() throws Exception
    {
        mockTransformCommand(PDF, TXT, MIMETYPE_PDF, true);
        mockMvc.perform(
                mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile, "targetExtension", targetExtension).param(
                        NOT_EXTRACT_BOOKMARKS_TEXT, "true"))
                .andExpect(status().is(OK.value()))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''transform." + targetExtension));
    }

    @Override
    protected void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest)
    {
        transformRequest.setSourceExtension(sourceExtension);
        transformRequest.setTargetExtension(targetExtension);
        transformRequest.setSourceMediaType(APPLICATION_PDF_VALUE);
        transformRequest.setTargetMediaType(TEXT_PLAIN_VALUE);
        transformRequest.getTransformRequestOptions().put("targetEncoding", "UTF-8");
    }

    @Test
    public void testPojoTransform() throws Exception
    {
        // Files
        String sourceFileRef = UUID.randomUUID().toString();
        File sourceFile = getTestFile("quick." + sourceExtension, true);
        String targetFileRef = UUID.randomUUID().toString();

        TransformRequest transformRequest = createTransformRequest(sourceFileRef, sourceFile);

        // HTTP Request
        HttpHeaders headers = new HttpHeaders();
        headers.set(CONTENT_DISPOSITION, "attachment; filename=quick." + sourceExtension);
        ResponseEntity<Resource> response = new ResponseEntity<>(new FileSystemResource(
                sourceFile), headers, OK);

        lenient().when(sharedFileStoreClient.retrieveFile(sourceFileRef)).thenReturn(response);
        lenient().when(sharedFileStoreClient.saveFile(any()))
                .thenReturn(new FileRefResponse(new FileRefEntity(targetFileRef)));
        lenient().when(mockExecutionResult.getExitValue()).thenReturn(0);

        // Update the Transformation Request with any specific params before sending it
        updateTransformRequestWithSpecificOptions(transformRequest);

        // Serialize and call the transformer
        String tr = objectMapper.writeValueAsString(transformRequest);
        String transformationReplyAsString = mockMvc
                .perform(MockMvcRequestBuilders
                        .post(ENDPOINT_TRANSFORM)
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

    @Test
    @Override
    public void httpTransformRequestUsingDirectAccessUrlTest() throws Exception
    {
        expectedTargetFileBytes = readTestFile(targetExtension);
        super.httpTransformRequestUsingDirectAccessUrlTest();
    }

    @Test
    public void optionListTest()
    {
        assertEquals(ImmutableSet.of(
                "includeContents",
                "targetEncoding",
                "extractMapping",
                "notExtractBookmarksText",
                "metadata"),
                getOptionNames(controller.transformConfig(0).getBody().getTransformOptions()));
    }
}
