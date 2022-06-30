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

import org.alfresco.transformer.util.RequestParamMap;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.tools.TextToPDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.alfresco.transform.common.RequestParamMap.SOURCE_ENCODING;

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

    private static final int UTF16_READ_AHEAD_BYTES = 16; // 8 characters including BOM if it exists
    private static final byte FE = (byte) 0xFE;
    private static final byte FF = (byte) 0xFF;

    public static final String PAGE_LIMIT = RequestParamMap.PAGE_LIMIT;

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
            throw new RuntimeException(
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
            throw new RuntimeException(
                "Unable to set Font Size for PDF generation: " + fontSize);
        }
    }

    @Override
    public void transform(final String sourceMimetype, final String targetMimetype, final Map<String, String> parameters,
                          final File sourceFile, final File targetFile) throws Exception
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
                // Handles the situation where there is a BOM even though the encoding indicates that normally
                // there should not be one for UTF-16BE and UTF-16LE. For extra flexibility includes UTF-16 too
                // which optionally has the BOM. Rather than look at the BOM we look at the number of zero bytes
                // in the first few character. XML files even when not in European languages tend to have more
                // even zero bytes when big-endian encoded and more odd zero bytes when little-endian.
                // Think of: <?xml version="1.0"?> The normal Java decoder does not have this flexibility but
                // other transformers do.
                String name = charset.displayName();
                if ("UTF-16".equals(name) || "UTF-16BE".equals(name) || "UTF-16LE".equals(name))
                {
                    logger.debug("Handle big and little endian UTF-16 text. Using UTF-16 rather than encoding " + name);
                    charset = Charset.forName("UTF-16");
                    is = new PushbackInputStream(is, UTF16_READ_AHEAD_BYTES)
                    {
                        boolean bomRead;
                        boolean switchByteOrder;
                        boolean evenByte = true;

                        @Override
                        public int read(byte[] bytes, int off, int len) throws IOException
                        {
                            int i = 0;
                            int b = 0;
                            for (; i<len; i++)
                            {
                                b = read();
                                if (b == -1)
                                {
                                    break;
                                }
                                bytes[off+i] = (byte)b;
                            }
                            return i == 0 && b == -1 ? -1 : i;
                        }

                        @Override
                        public int read() throws IOException
                        {
                            if (!bomRead)
                            {
                                bomRead = true;
                                boolean switchBom = false;
                                byte[] bytes = new byte[UTF16_READ_AHEAD_BYTES];
                                int end = in.read(bytes, 0, UTF16_READ_AHEAD_BYTES);
                                int evenZeros = countZeros(bytes, 0);
                                int oddZeros = countZeros(bytes, 1);
                                if (evenZeros > oddZeros)
                                {
                                    if (bytes[0] == FF && bytes[1] == FE)
                                    {
                                        switchByteOrder = true;
                                        switchBom = true;
                                        logger.warn("Little-endian BOM FFFE read, but characters are big-endian");
                                    }
                                    else
                                    {
                                        logger.debug("More even zero bytes, so normal read for big-endian");
                                    }
                                }
                                else
                                {
                                    if (bytes[0] == FE && bytes[1] == FF)
                                    {
                                        switchBom = true;
                                        logger.debug("Big-endian BOM FEFF read, but characters are little-endian");
                                    }
                                    else
                                    {
                                        switchByteOrder = true;
                                        logger.debug("More odd zero bytes, so switch bytes from little-endian");
                                    }
                                }

                                if (switchBom)
                                {
                                    byte b = bytes[0];
                                    bytes[0] = bytes[1];
                                    bytes[1] = b;
                                }

                                for (int i = end-1; i>=0; i--)
                                {
                                    unread(bytes[i]);
                                }
                            }

                            if (switchByteOrder)
                            {
                                if (evenByte)
                                {
                                    int b1 = super.read();
                                    int b2 = super.read();
                                    if (b1 != -1)
                                    {
                                        unread(b1);
                                    }
                                    if (b2 != -1)
                                    {
                                        unread(b2);
                                    }
                                }
                                evenByte = !evenByte;
                            }

                            return super.read();
                        }

                        // Counts the number of even or odd 00 bytes
                        private int countZeros(byte[] b, int offset)
                        {
                            int count = 0;
                            for (int i=offset; i<UTF16_READ_AHEAD_BYTES; i+=2)
                            {
                                if (b[i] == 0)
                                {
                                    count++;
                                }
                            }
                            return count;
                        }
                    };
                }
                logger.debug("Processing plain text in encoding " + name);
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
            PDDocument doc = null;
            int pageCount = 0;
            try
            {
                final int margin = 40;
                float height = getFont().getFontDescriptor().getFontBoundingBox().getHeight() / 1000;

                //calculate font height and increase by 5 percent.
                height = height * getFontSize() * 1.05f;
                doc = new PDDocument();
                BufferedReader data = (text instanceof BufferedReader) ? (BufferedReader) text : new BufferedReader(text);
                String nextLine;
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
                            contentStream.moveTextPositionByAmount(margin, y);
                        }

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
