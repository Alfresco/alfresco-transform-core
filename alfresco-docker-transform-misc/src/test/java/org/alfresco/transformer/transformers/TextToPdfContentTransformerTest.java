package org.alfresco.transformer.transformers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.alfresco.transformer.transformers.TextToPdfContentTransformer.PAGE_LIMIT;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@Import(TextToPdfContentTransformer.class)
public class TextToPdfContentTransformerTest
{
    @MockBean
    SelectingTransformer mockSelectingTransformer;

    @Autowired
    TextToPdfContentTransformer transformer;

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

    private void transformTextAndCheckPageLength(int pageLimit) throws Exception
    {
        int pageLength = 32;
        int lines = (pageLength+10) * ((pageLimit > 0) ? pageLimit : 1);
        StringBuilder sb = new StringBuilder();
        String checkText = null;
        int cutoff = pageLimit * pageLength;
        for (int i=1; i<=lines; i++)
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

    private void transformTextAndCheck(String text, String encoding, String checkText, String pageLimit) throws Exception
    {
        // Get a reader for the text
        File sourceFile = File.createTempFile("AlfrescoTestSource_", ".txt");
        writeToFile(sourceFile,text, encoding);


        // And a temp writer
        File targetFile = File.createTempFile("AlfrescoTestTarget_", ".pdf");

        // Transform to PDF
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PAGE_LIMIT, pageLimit);
        transformer.transform(sourceFile, targetFile, parameters);

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
}