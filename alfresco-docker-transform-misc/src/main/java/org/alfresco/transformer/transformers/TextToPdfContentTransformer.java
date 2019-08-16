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

import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_DITA;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_TEXT_CSV;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_XML;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.tools.TextToPDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This code is based on a class of the same name originally implemented in alfresco-repository.
 * </p>
 *
 * Makes use of the <a href="http://www.pdfbox.org/">PDFBox</a> library's <code>TextToPDF</code> utility.
 *
 * @author Derek Hulley
 * @author eknizat
 */
public class TextToPdfContentTransformer implements SelectableTransformer
{
    private static final Logger logger = LoggerFactory.getLogger(TextToPdfContentTransformer.class);

    public static final String PAGE_LIMIT = "pageLimit";

    private final PagedTextToPDF transformer;

    public TextToPdfContentTransformer()
    {
        transformer = new PagedTextToPDF();
    }

    public void setStandardFont(String fontName)
    {
        try
        {
            transformer.setFont(PagedTextToPDF.getStandardFont(fontName));
        }
        catch (Throwable e)
        {
            throw new AlfrescoRuntimeException(
                "Unable to set Standard Font for PDF generation: " + fontName, e);
        }
    }

    public void setFontSize(int fontSize)
    {
        try
        {
            transformer.setFontSize(fontSize);
        }
        catch (Throwable e)
        {
            throw new AlfrescoRuntimeException(
                "Unable to set Font Size for PDF generation: " + fontSize);
        }
    }

    @Override
    public boolean isTransformable(String sourceMimetype, String targetMimetype,
        Map<String, String> parameters)
    {
        return (MIMETYPE_TEXT_PLAIN.equals(sourceMimetype) ||
                MIMETYPE_TEXT_CSV.equals(sourceMimetype) ||
                MIMETYPE_DITA.equals(sourceMimetype) ||
                MIMETYPE_XML.equals(sourceMimetype)) &&
               MIMETYPE_PDF.equals(targetMimetype);
    }

    @Override
    public void transform(File sourceFile, File targetFile, Map<String, String> parameters)
        throws Exception
    {
        String sourceEncoding = parameters.get(SOURCE_ENCODING);
        String stringPageLimit = parameters.get(PAGE_LIMIT);
        int pageLimit = -1;
        if (stringPageLimit != null)
        {
            pageLimit = parseInt(stringPageLimit, PAGE_LIMIT);
        }

        PDDocument pdf = null;
        try (InputStream is = new FileInputStream(sourceFile);
             Reader ir = new BufferedReader(buildReader(is, sourceEncoding));
             OutputStream os = new BufferedOutputStream(new FileOutputStream(targetFile)))
        {
            //TransformationOptionLimits limits = getLimits(reader, writer, options);
            //TransformationOptionPair pageLimits = limits.getPagesPair();
            pdf = transformer.createPDFFromText(ir, pageLimit);
            pdf.save(os);
        }
        finally
        {
            if (pdf != null)
            {
                try { pdf.close(); } catch (Throwable e) {e.printStackTrace(); }
            }
        }
    }

    protected InputStreamReader buildReader(InputStream is, String encoding)
    {
        // If they gave an encoding, try to use it
        if (encoding != null)
        {
            Charset charset = null;
            try
            {
                charset = Charset.forName(encoding);
            }
            catch (Exception e)
            {
                logger.warn("JVM doesn't understand encoding '" + encoding +
                            "' when transforming text to pdf");
            }
            if (charset != null)
            {
                logger.debug("Processing plain text in encoding " + charset.displayName());
                return new InputStreamReader(is, charset);
            }
        }

        // Fall back on the system default
        logger.debug("Processing plain text using system default encoding");
        return new InputStreamReader(is);
    }

    private static class PagedTextToPDF extends TextToPDF
    {
        // REPO-1066: duplicating the following lines from org.apache.pdfbox.tools.TextToPDF because they made them private
        // before the upgrade to pdfbox 2.0.8, in pdfbox 1.8, this piece of code was public in org.apache.pdfbox.pdmodel.font.PDType1Font
        static PDType1Font getStandardFont(String name)
        {
            return STANDARD_14.get(name);
        }

        private static final Map<String, PDType1Font> STANDARD_14 = new HashMap<>();

        static
        {
            STANDARD_14.put(PDType1Font.TIMES_ROMAN.getBaseFont(), PDType1Font.TIMES_ROMAN);
            STANDARD_14.put(PDType1Font.TIMES_BOLD.getBaseFont(), PDType1Font.TIMES_BOLD);
            STANDARD_14.put(PDType1Font.TIMES_ITALIC.getBaseFont(), PDType1Font.TIMES_ITALIC);
            STANDARD_14.put(PDType1Font.TIMES_BOLD_ITALIC.getBaseFont(),
                PDType1Font.TIMES_BOLD_ITALIC);
            STANDARD_14.put(PDType1Font.HELVETICA.getBaseFont(), PDType1Font.HELVETICA);
            STANDARD_14.put(PDType1Font.HELVETICA_BOLD.getBaseFont(), PDType1Font.HELVETICA_BOLD);
            STANDARD_14.put(PDType1Font.HELVETICA_OBLIQUE.getBaseFont(),
                PDType1Font.HELVETICA_OBLIQUE);
            STANDARD_14.put(PDType1Font.HELVETICA_BOLD_OBLIQUE.getBaseFont(),
                PDType1Font.HELVETICA_BOLD_OBLIQUE);
            STANDARD_14.put(PDType1Font.COURIER.getBaseFont(), PDType1Font.COURIER);
            STANDARD_14.put(PDType1Font.COURIER_BOLD.getBaseFont(), PDType1Font.COURIER_BOLD);
            STANDARD_14.put(PDType1Font.COURIER_OBLIQUE.getBaseFont(), PDType1Font.COURIER_OBLIQUE);
            STANDARD_14.put(PDType1Font.COURIER_BOLD_OBLIQUE.getBaseFont(),
                PDType1Font.COURIER_BOLD_OBLIQUE);
            STANDARD_14.put(PDType1Font.SYMBOL.getBaseFont(), PDType1Font.SYMBOL);
            STANDARD_14.put(PDType1Font.ZAPF_DINGBATS.getBaseFont(), PDType1Font.ZAPF_DINGBATS);
        }
        //duplicating until here

        // The following code is based on the code in TextToPDF with the addition of
        // checks for page limits.
        // The calling code must close the PDDocument once finished with it.
        public PDDocument createPDFFromText(Reader text, int pageLimit)
            throws IOException
        {
            //int pageLimit = (int)pageLimits.getValue();
            PDDocument doc = null;
            int pageCount = 0;
            try
            {
                final int margin = 40;
                float height = getFont().getFontDescriptor().getFontBoundingBox().getHeight() / 1000;

                //calculate font height and increase by 5 percent.
                height = height * getFontSize() * 1.05f;
                doc = new PDDocument();
                BufferedReader data = new BufferedReader(text);
                String nextLine = null;
                PDPage page = new PDPage();
                PDPageContentStream contentStream = null;
                float y = -1;
                float maxStringLength = page.getMediaBox().getWidth() - 2 * margin;

                // There is a special case of creating a PDF document from an empty string.
                boolean textIsEmpty = true;

                outer:
                while ((nextLine = data.readLine()) != null)
                {

                    // The input text is nonEmpty. New pages will be created and added
                    // to the PDF document as they are needed, depending on the length of
                    // the text.
                    textIsEmpty = false;

                    String[] lineWords = nextLine.trim().split(" ");
                    int lineIndex = 0;
                    while (lineIndex < lineWords.length)
                    {
                        final StringBuilder nextLineToDraw = new StringBuilder();
                        float lengthIfUsingNextWord = 0;
                        do
                        {
                            nextLineToDraw.append(lineWords[lineIndex]);
                            nextLineToDraw.append(" ");
                            lineIndex++;
                            if (lineIndex < lineWords.length)
                            {
                                String lineWithNextWord = nextLineToDraw.toString() + lineWords[lineIndex];
                                lengthIfUsingNextWord =
                                    (getFont().getStringWidth(
                                        lineWithNextWord) / 1000) * getFontSize();
                            }
                        }
                        while (lineIndex < lineWords.length &&
                               lengthIfUsingNextWord < maxStringLength);
                        if (y < margin)
                        {
                            int test = pageCount + 1;
                            if (pageLimit > 0 && (pageCount++ >= pageLimit))
                            {
//                                pageLimits.getAction().throwIOExceptionIfRequired("Page limit ("+pageLimit+
//                                        ") reached.", transformerDebug);
                                break outer;
                            }

                            // We have crossed the end-of-page boundary and need to extend the
                            // document by another page.
                            page = new PDPage();
                            doc.addPage(page);
                            if (contentStream != null)
                            {
                                contentStream.endText();
                                contentStream.close();
                            }
                            contentStream = new PDPageContentStream(doc, page);
                            contentStream.setFont(getFont(), getFontSize());
                            contentStream.beginText();
                            y = page.getMediaBox().getHeight() - margin + height;
                            contentStream.moveTextPositionByAmount(
                                margin, y);
                        }
                        //System.out.println( "Drawing string at " + x + "," + y );

                        if (contentStream == null)
                        {
                            throw new IOException("Error:Expected non-null content stream.");
                        }
                        contentStream.moveTextPositionByAmount(0, -height);
                        y -= height;
                        contentStream.drawString(nextLineToDraw.toString());
                    }
                }

                // If the input text was the empty string, then the above while loop will have short-circuited
                // and we will not have added any PDPages to the document.
                // So in order to make the resultant PDF document readable by Adobe Reader etc, we'll add an empty page.
                if (textIsEmpty)
                {
                    doc.addPage(page);
                }

                if (contentStream != null)
                {
                    contentStream.endText();
                    contentStream.close();
                }
            }
            catch (IOException io)
            {
                if (doc != null)
                {
                    doc.close();
                }
                throw io;
            }
            return doc;
        }
    }

    private int parseInt(String s, String paramName)
    {
        try
        {
            return Integer.valueOf(s);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(paramName + " parameter must be an integer.");
        }
    }
}
