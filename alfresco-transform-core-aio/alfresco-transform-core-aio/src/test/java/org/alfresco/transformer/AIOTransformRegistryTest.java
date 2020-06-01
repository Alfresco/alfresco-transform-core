/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.transform.client.model.config.TransformConfig;
import org.alfresco.transformer.transformers.MiscAdapter;
import org.alfresco.transformer.transformers.TikaAdapter;
import org.alfresco.transformer.transformers.Transformer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.alfresco.transformer.transformers.TextToPdfContentTransformer.PAGE_LIMIT;
import static org.alfresco.transformer.transformers.Transformer.TRANSFORM_NAME_PARAMETER;
import static org.junit.Assert.*;

public class AIOTransformRegistryTest
{
    private static final String SOURCE_MIMETYPE = "text/html";
    private static final String TARGET_MIMETYPE = "text/plain";
    String SOURCE_ENCODING = "sourceEncoding";
    String TARGET_ENCODING = "targetEncoding";

    AIOTransformRegistry aioTransformerRegistry = new AIOTransformRegistry();
    ObjectMapper objectMapper = new ObjectMapper();


    @Before
    public void before() throws Exception
    {
        aioTransformerRegistry.registerTransformer(new MiscAdapter());
        aioTransformerRegistry.registerTransformer(new TikaAdapter());

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

    private TransformConfig loadConfig(String s) throws Exception
    {
        return objectMapper.readValue(new ClassPathResource(s).getFile(), TransformConfig.class);
    }

    @Test
    public void testConfigAggregation() throws Exception
    {
        List<String> expectedTransformNames = Arrays.asList("html", "string", "appleIWorks", "textToPdf", "rfc822",
                "Archive", "OutlookMsg", "PdfBox", "Office", "Poi", "OOXML", "TikaAuto", "TextMining");

        List<String> expectedTransformOptionNames = Arrays.asList("tikaOptions", "archiveOptions", "pdfboxOptions",
                "textToPdfOptions", "stringOptions");

        TransformConfig miscConfig = loadConfig("misc_engine_config.json");
        TransformConfig tikaConfig = loadConfig("tika_engine_config.json");

        // check correct number of transformers
        assertEquals("Number of expected transformers",
                miscConfig.getTransformers().size() + tikaConfig.getTransformers().size(),
                aioTransformerRegistry.getTransformConfig().getTransformers().size());

        List<String> actualTransformerNames = aioTransformerRegistry.getTransformConfig().getTransformers()
                .stream().map(t -> t.getTransformerName()).collect(Collectors.toList());
        // check all transformers are there
        for(String transformNames : expectedTransformNames)
        {
            assertTrue("Expected transformer missing.",  actualTransformerNames.contains(transformNames));
        }

        // check correct number of options
        assertEquals("Number of expected transformers",
                miscConfig.getTransformOptions().size() + tikaConfig.getTransformOptions().size(),
                aioTransformerRegistry.getTransformConfig().getTransformOptions().size());

        Set<String> actualOptionNames = aioTransformerRegistry.getTransformConfig().getTransformOptions().keySet();

        // check all options are there
        for (String optionName : expectedTransformOptionNames)
        {
            assertTrue("Expected transform option missing.",  actualOptionNames.contains(optionName));
        }
    }

    @Test
    public void testTransformerMapping()
    {
        List<String> tikaTransforms = Arrays.asList("Archive", "OutlookMsg", "PdfBox", "Office", "Poi", "OOXML", "TikaAuto", "TextMining");
        List<String> miscTransforms = Arrays.asList("html", "string", "appleIWorks", "textToPdf", "rfc822");

        for (String transform : tikaTransforms)
        {
            String actualId = aioTransformerRegistry.getByTransformName(transform).getTransformerId();
            assertEquals("Wrong mapping for transform "+transform, "tika", actualId);
        }

        for (String transform : miscTransforms)
        {
            String actualId = aioTransformerRegistry.getByTransformName(transform).getTransformerId();
            assertEquals("Wrong mapping for transform "+transform, "misc", actualId);
        }
    }

    @Test(expected = Exception.class)
    public void testDuplicateTransformsException() throws Exception
    {
        // The Misc transformers are already registered
        aioTransformerRegistry.registerTransformer(new MiscAdapter());
    }

    // Test copied from Misc (HtmlParserContentTransformerTest) See ATS-712 aioTransformerRegistry - html
    @Test
    public void testMiscHtml() throws Exception
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
            parameters.put(TRANSFORM_NAME_PARAMETER, "html");
            Transformer transformer = aioTransformerRegistry.getByTransformName("html");
            transformer.transform(tmpS, tmpD, SOURCE_MIMETYPE, TARGET_MIMETYPE, parameters);

            assertEquals(expected, readFromFile(tmpD, "UTF-8"));
            tmpS.delete();
            tmpD.delete();

            // Content set to UTF-8
            tmpS = File.createTempFile("AlfrescoTestSource_", ".html");
            writeToFile(tmpS, partA + partB + partC, "UTF-8");

            tmpD = File.createTempFile("AlfrescoTestTarget_", ".txt");
            parameters = new HashMap<>();
            parameters.put(TRANSFORM_NAME_PARAMETER, "html");
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
            parameters.put(TRANSFORM_NAME_PARAMETER, "html");
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
            parameters.put(TRANSFORM_NAME_PARAMETER, "html");
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

    // Test copied from Misc (TextToPdfContentTransformerTest) See ATS-712 aioTransformerRegistry - pdf
    @Test
    public void testMiscPdf() throws Exception
    {
        transformTextAndCheckPageLength(-1);
    }

    private void transformTextAndCheckPageLength(int pageLimit) throws Exception
    {
        int pageLength = 32;
        int lines = (pageLength + 10) * ((pageLimit > 0) ? pageLimit : 1);
        StringBuilder sb = new StringBuilder();
        String checkText = null;
        int cutoff = pageLimit * pageLength;
        for (int i = 1; i <= lines; i++)
        {
            sb.append(i);
            sb.append(" I must not talk in class or feed my homework to my cat.\n");
            if (i == cutoff)
                checkText = sb.toString();
        }
        sb.append("\nBart\n");
        String text = sb.toString();
        checkText = (checkText == null) ? clean(text) : clean(checkText);
        transformTextAndCheck(text, "UTF-8", checkText, String.valueOf(pageLimit));
    }

    private void transformTextAndCheck(String text, String encoding, String checkText,
                                       String pageLimit) throws Exception
    {
        // Get a reader for the text
        File sourceFile = File.createTempFile("AlfrescoTestSource_", ".txt");
        writeToFile(sourceFile, text, encoding);

        // And a temp writer
        File targetFile = File.createTempFile("AlfrescoTestTarget_", ".pdf");

        // Transform to PDF
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PAGE_LIMIT, pageLimit);
        parameters.put(TRANSFORM_NAME_PARAMETER, "textToPdf");
        Transformer transformer = aioTransformerRegistry.getByTransformName("textToPdf");
        transformer.transform(sourceFile, targetFile, "text/plain", "application/pdf", parameters);

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

}