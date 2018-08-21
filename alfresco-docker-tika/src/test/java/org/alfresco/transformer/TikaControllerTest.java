/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transformer;

import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_HTML;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_OPENXML_PRESENTATION;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_OPENXML_SPREADSHEET;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_OPENXML_WORDPROCESSING;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_OUTLOOK_MSG;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_PDF;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_TEXT_CSV;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_WORD;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_XHTML;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_XML;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_ZIP;
import static org.alfresco.transformer.Tika.ARCHIVE;
import static org.alfresco.transformer.Tika.CSV;
import static org.alfresco.transformer.Tika.DOC;
import static org.alfresco.transformer.Tika.DOCX;
import static org.alfresco.transformer.Tika.HTML;
import static org.alfresco.transformer.Tika.MSG;
import static org.alfresco.transformer.Tika.OUTLOOK_MSG;
import static org.alfresco.transformer.Tika.PDF;
import static org.alfresco.transformer.Tika.PDF_BOX;
import static org.alfresco.transformer.Tika.POI;
import static org.alfresco.transformer.Tika.POI_OFFICE;
import static org.alfresco.transformer.Tika.POI_OO_XML;
import static org.alfresco.transformer.Tika.PPTX;
import static org.alfresco.transformer.Tika.TEXT_MINING;
import static org.alfresco.transformer.Tika.TIKA_AUTO;
import static org.alfresco.transformer.Tika.TXT;
import static org.alfresco.transformer.Tika.XHTML;
import static org.alfresco.transformer.Tika.XML;
import static org.alfresco.transformer.Tika.XSLX;
import static org.alfresco.transformer.Tika.ZIP;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.alfresco.transform.client.model.TransformRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Test the TikaController without a server.
 * Super class includes tests for the AbstractTransformerController.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(TikaController.class)
public class TikaControllerTest extends AbstractTransformerControllerTest
{
    public static final String EXPECTED_XHTML_CONTENT_CONTAINS = "<p>The quick brown fox jumps over the lazy dog</p>";
    public static final String EXPECTED_TEXT_CONTENT_CONTAINS  =    "The quick brown fox jumps over the lazy dog";
    public static final String EXPECTED_MSG_CONTENT_CONTAINS = "Recipients\n" +
            "\tmark.rogers@alfresco.com; speedy@quick.com; mrquick@nowhere.com\n" +
            "\n" +
            "The quick brown fox jumps over the lazy dogs";
    public static final String EXPECTED_CSV_CONTENT_CONTAINS = "\"The\",\"quick\",\"brown\",\"fox\"";

    @SpyBean
    private TikaController controller;

    String transform = PDF_BOX;
    String targetEncoding = "UTF-8";
    String targetMimetype = MIMETYPE_TEXT_PLAIN;

    @Before
    public void before() throws Exception
    {
        controller.setAlfrescoSharedFileStoreClient(alfrescoSharedFileStoreClient);
        super.controller = controller;

        sourceExtension = "pdf";
        targetExtension = "txt";
    }

    private void transform(String transform, String sourceExtension, String targetExtension,
                           String sourceMimetype, String targetMimetype,
                           Boolean includeContents, String expectedContentContains) throws Exception
    {
        // We don't use targetFileBytes as some of the transforms contain different date text based on the os being used.
        super.mockTransformCommand(controller, sourceExtension, targetExtension, sourceMimetype, false);
        this.transform = transform;
        this.targetMimetype = targetMimetype;

        System.out.println("Test "+transform+" "+ sourceExtension +" to "+targetExtension);
        MockHttpServletRequestBuilder requestBuilder = includeContents == null
            ? mockMvcRequest("/transform", sourceFile, "targetExtension", this.targetExtension)
            : mockMvcRequest("/transform", sourceFile, "targetExtension", this.targetExtension, "includeContents", includeContents.toString());
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().is(200))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick." + this.targetExtension)).
                        andReturn();
        String content = result.getResponse().getContentAsString();
        assertTrue("The content did not include \""+expectedContentContains, content.contains(expectedContentContains));
    }

    @Override
    // Add extra required parameters to the request.
    protected MockHttpServletRequestBuilder mockMvcRequest(String url, MockMultipartFile sourceFile, String... params)
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
        super.mockTransformCommand(controller, PDF, TXT, MIMETYPE_PDF, true);
        super.simpleTransformTest();
    }

    @Test
    @Override
    public void testDelayTest() throws Exception
    {
        super.mockTransformCommand(controller, PDF, TXT, MIMETYPE_PDF, true);
        super.testDelayTest();
    }

    @Test
    @Override
    public void badExitCodeTest() throws Exception
    {
        // Ignore the test in super class as the Tika transforms are real rather than mocked up.
        // It is the mock that returns a non zero exit code.
    }

    @Test
    @Override
    public void noTargetFileTest() throws Exception
    {
        // Ignore the test in super class as the Tika transforms are real rather than mocked up.
        // It is the mock that returns a zero length file for other transformers, when we supply an invalid targetExtension.
    }

    // --- Super class tests (need modified setup) ---

    @Test
    @Override
    public void dotDotSourceFilenameTest() throws Exception
    {
        super.mockTransformCommand(controller, PDF, TXT, MIMETYPE_PDF, true);
        super.dotDotSourceFilenameTest();
    }

    @Test
    @Override
    public void noExtensionSourceFilenameTest() throws Exception
    {
        super.mockTransformCommand(controller, PDF, TXT, MIMETYPE_PDF, true);
        super.noExtensionSourceFilenameTest();
    }

    @Test
    @Override
    public void badSourceFilenameTest() throws Exception
    {
        super.mockTransformCommand(controller, PDF, TXT, MIMETYPE_PDF, true);
        super.badSourceFilenameTest();
    }

    @Test
    @Override
    public void blankSourceFilenameTest() throws Exception
    {
        super.mockTransformCommand(controller, PDF, TXT, MIMETYPE_PDF, true);
        super.blankSourceFilenameTest();
    }

    @Test
    @Override
    public void noTargetExtensionTest() throws Exception
    {
        super.mockTransformCommand(controller, PDF, TXT, MIMETYPE_PDF, true);
        super.noTargetExtensionTest();
    }

    @Test
    @Override
    public void calculateMaxTime() throws Exception
    {
        super.mockTransformCommand(controller, PDF, TXT, MIMETYPE_PDF, true);
        super.calculateMaxTime();
    }

    // --- General Tika tests ---

    @Test
    public void badEncodingTest() throws Exception
    {
        super.mockTransformCommand(controller, PDF, TXT, MIMETYPE_PDF, true);
        targetEncoding = "rubbish";
        mockMvc.perform(mockMvcRequest("/transform", sourceFile, "targetExtension", targetExtension))
                .andExpect(status().is(500));
    }

    // --- Archive ---

    @Test
    public void zipToTextArchiveTest() throws Exception
    {
        transform(ARCHIVE, ZIP, TXT, MIMETYPE_ZIP, MIMETYPE_TEXT_PLAIN,false,
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
        transform(ARCHIVE, ZIP, TXT, MIMETYPE_ZIP, MIMETYPE_TEXT_PLAIN,true,
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
        transform(OUTLOOK_MSG, MSG, TXT, MIMETYPE_OUTLOOK_MSG, MIMETYPE_TEXT_PLAIN, null, EXPECTED_MSG_CONTENT_CONTAINS);
    }

    // --- PdfBox ---

    @Test
    public void pdfToTxtPdfBoxTest() throws Exception
    {
        transform(PDF_BOX, PDF, TXT, MIMETYPE_PDF, MIMETYPE_TEXT_PLAIN, null, EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    @Test
    public void pdfToCsvPdfBoxTest() throws Exception
    {
        transform(PDF_BOX, PDF, CSV, MIMETYPE_PDF, MIMETYPE_TEXT_CSV, null, EXPECTED_TEXT_CONTENT_CONTAINS); // Yes it is just text
    }

    @Test
    public void pdfToXmlPdfBoxTest() throws Exception
    {
        transform(PDF_BOX, PDF, XML, MIMETYPE_PDF, MIMETYPE_XML, null, EXPECTED_XHTML_CONTENT_CONTAINS); // Yes it is just XHTML
    }

    @Test
    public void pdfToXhtmlPdfBoxTest() throws Exception
    {
        transform(PDF_BOX, PDF, XHTML, MIMETYPE_PDF, MIMETYPE_XHTML, null, EXPECTED_XHTML_CONTENT_CONTAINS);
    }

    @Test
    public void pdfToHtmlPdfBoxTest() throws Exception
    {
        transform(PDF_BOX, PDF, HTML, MIMETYPE_PDF, MIMETYPE_HTML, null, EXPECTED_XHTML_CONTENT_CONTAINS); // Yes it is just XHTML
    }

    // --- Office ---

    @Test
    public void msgToTxtOfficeTest() throws Exception
    {
        transform(POI_OFFICE, MSG, TXT, MIMETYPE_OUTLOOK_MSG, MIMETYPE_TEXT_PLAIN, null, EXPECTED_MSG_CONTENT_CONTAINS);
    }

    @Test
    public void docToTxtOfficeTest() throws Exception
    {
        transform(POI_OFFICE, DOC, TXT, MIMETYPE_WORD, MIMETYPE_TEXT_PLAIN, null, EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    // --- Poi ---

    @Test
    public void xslxToCsvPoiTest() throws Exception
    {
        transform(POI, XSLX, CSV, MIMETYPE_OPENXML_SPREADSHEET, MIMETYPE_TEXT_CSV, null, EXPECTED_CSV_CONTENT_CONTAINS);
    }

    // --- OOXML ---

    @Test
    public void docxToTxtOoXmlTest() throws Exception
    {
        transform(POI_OO_XML, DOCX, TXT, MIMETYPE_OPENXML_WORDPROCESSING, MIMETYPE_TEXT_PLAIN, null, EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    @Test
    public void pptxToTxtOoXmlTest() throws Exception
    {
        transform(POI_OO_XML, PPTX, TXT, MIMETYPE_OPENXML_PRESENTATION, MIMETYPE_TEXT_PLAIN, null, EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    // --- TikaAuto ---

    @Test
    public void ppxtToTxtTikaAutoTest() throws Exception
    {
        transform(TIKA_AUTO, PPTX, TXT, MIMETYPE_OPENXML_PRESENTATION, MIMETYPE_TEXT_PLAIN, null, EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    @Test
    public void doctToTxtTikaAutoTest() throws Exception
    {
        transform(TIKA_AUTO, DOCX, TXT, MIMETYPE_OPENXML_WORDPROCESSING, MIMETYPE_TEXT_PLAIN, null, EXPECTED_TEXT_CONTENT_CONTAINS);
    }

    // --- TextMining ---

    @Test
    public void docToTxtTextMiningTest() throws Exception
    {
        transform(TEXT_MINING, DOC, TXT, MIMETYPE_WORD, MIMETYPE_TEXT_PLAIN, null, EXPECTED_TEXT_CONTENT_CONTAINS);
    }
    
    @Test
    public void pdfToTxtExtractBookmarksTest() throws Exception
    {
        super.mockTransformCommand(controller, PDF, TXT, MIMETYPE_PDF, true);
        mockMvc.perform(mockMvcRequest("/transform", sourceFile, "targetExtension", targetExtension).param("notExtractBookmarksText", "true"))
                .andExpect(status().is(200))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick." + targetExtension));
    }

    @Override
    protected void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest)
    {
        transformRequest.setSourceExtension(sourceExtension);
        transformRequest.setTargetExtension(targetExtension);
        transformRequest.setSourceMediaType(MediaType.APPLICATION_PDF_VALUE);
        transformRequest.setTargetMediaType(MediaType.TEXT_PLAIN_VALUE);
        transformRequest.getTransformRequestOptions().put("transform", "PdfBox");
        transformRequest.getTransformRequestOptions().put("targetMimetype", MediaType.TEXT_PLAIN_VALUE);
        transformRequest.getTransformRequestOptions().put("targetEncoding", "UTF-8");
    }
}
