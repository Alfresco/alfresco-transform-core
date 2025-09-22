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

import static org.alfresco.transform.common.RequestParamMap.PAGE_LIMIT;
import static org.alfresco.transform.common.RequestParamMap.PDF_FONT;
import static org.alfresco.transform.common.RequestParamMap.PDF_FONT_SIZE;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_ENCODING;

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
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;

import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.util.autodetect.FontFileFinder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.FontMappers;
import org.apache.pdfbox.pdmodel.font.FontMapping;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.tools.TextToPDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;

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
@Component
public class TextToPdfContentTransformer implements CustomTransformerFileAdaptor
{
    private static final Logger logger = LoggerFactory.getLogger(TextToPdfContentTransformer.class);

    private static final int UTF16_READ_AHEAD_BYTES = 16; // 8 characters including BOM if it exists
    private static final byte FE = (byte) 0xFE;
    private static final byte FF = (byte) 0xFF;
    private static final int UTF8_READ_AHEAD_BYTES = 3;
    private static final byte EF = (byte) 0xEF;
    private static final byte BB = (byte) 0xBB;
    private static final byte BF = (byte) 0xBF;
    private static final String DEFAULT_FONT = "NotoSans-Regular";
    private static final int DEFAULT_FONT_SIZE = 10;

    private final PagedTextToPDF transformer;

    @Value("${transform.core.misc.pdfBox.defaultFont:NotoSans-Regular}")
    private String pdfBoxDefaultFont;

    public TextToPdfContentTransformer()
    {
        transformer = new PagedTextToPDF();
    }

    @PostConstruct
    public void init()
    {
        transformer.setDefaultFont(pdfBoxDefaultFont);
    }

    public void setStandardFont(String fontName)
    {
        try
        {
            transformer.setFont(fontName);
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

    public String getUsedFont()
    {
        return transformer.getFontName();
    }

    @Override
    public String getTransformerName()
    {
        return "textToPdf";
    }

    @Override
    public void transform(final String sourceMimetype, final String targetMimetype, final Map<String, String> transformOptions,
            final File sourceFile, final File targetFile, TransformManager transformManager) throws Exception
    {
        String sourceEncoding = transformOptions.get(SOURCE_ENCODING);
        String stringPageLimit = transformOptions.get(PAGE_LIMIT);
        int pageLimit = -1;
        if (stringPageLimit != null)
        {
            pageLimit = parseInt(stringPageLimit, PAGE_LIMIT);
        }
        String pdfFont = transformOptions.get(PDF_FONT);
        if (pdfFont == null || pdfFont.isBlank())
        {
            pdfFont = pdfBoxDefaultFont;
        }
        String pdfFontSize = transformOptions.get(PDF_FONT_SIZE);
        Integer fontSize = null;
        if (pdfFontSize != null && !pdfFontSize.isBlank())
        {
            try
            {
                fontSize = parseInt(pdfFontSize, PDF_FONT_SIZE);
            }
            catch (Exception e)
            {
                fontSize = DEFAULT_FONT_SIZE;
                logger.error("Error parsing font size {}, going to set it as {}", pdfFontSize, fontSize, e);
            }
        }

        PDDocument pdf = null;
        try (InputStream is = new FileInputStream(sourceFile);
                Reader ir = new BufferedReader(buildReader(is, sourceEncoding));
                OutputStream os = new BufferedOutputStream(new FileOutputStream(targetFile)))
        {
            // TransformationOptionLimits limits = getLimits(reader, writer, options);
            // TransformationOptionPair pageLimits = limits.getPagesPair();
            pdf = transformer.createPDFFromText(ir, pageLimit, pdfFont, fontSize);
            pdf.save(os);
        }
        finally
        {
            if (pdf != null)
            {
                try
                {
                    pdf.close();
                }
                catch (Throwable e)
                {
                    e.printStackTrace();
                }
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
                    is = handleUTF16BOM(is);
                }
                else if ("UTF-8".equals(name))
                {
                    logger.debug("Using UTF-8");
                    charset = Charset.forName("UTF-8");
                    is = handleUTF8BOM(is);
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
        private static final PDType1Font DEFAULT_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private static final Map<String, PDType1Font> STANDARD_14 = Standard14Fonts.getNames().stream()
                .collect(Collectors.toMap(name -> name, name -> new PDType1Font(Standard14Fonts.getMappedFontName(name))));

        private String fontName = null;
        private String defaultFont = null;

        PagedTextToPDF()
        {
            super();
            setFont(DEFAULT_FONT);
        }

        static PDType1Font getStandardFont(String name)
        {
            return STANDARD_14.get(name);
        }

        // The following code is based on the code in TextToPDF with the addition of
        // checks for page limits.
        // The calling code must close the PDDocument once finished with it.
        public PDDocument createPDFFromText(Reader text, int pageLimit, String pdfFontName, Integer pdfFontSize)
                throws IOException
        {
            PDDocument doc = null;
            int pageCount = 0;
            try
            {
                doc = new PDDocument();

                final PDFont font = getFont(doc, pdfFontName);
                final int fontSize = pdfFontSize != null ? pdfFontSize : getFontSize();

                fontName = font.getName();

                logger.debug("Going to use font {} with size {}", fontName, fontSize);

                final int margin = 40;
                float height = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000;

                // calculate font height and increase by 5 percent.
                height = height * fontSize * 1.05f;

                BufferedReader data = (text instanceof BufferedReader) ? (BufferedReader) text : new BufferedReader(text);
                String nextLine;
                PDPage page = new PDPage();
                PDPageContentStream contentStream = null;
                float y = -1;
                float maxStringLength = page.getMediaBox().getWidth() - 2 * margin;

                // There is a special case of creating a PDF document from an empty string.
                boolean textIsEmpty = true;

                outer: while ((nextLine = data.readLine()) != null)
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
                                lengthIfUsingNextWord = (font.getStringWidth(
                                        lineWithNextWord) / 1000) * fontSize;
                            }
                        } while (lineIndex < lineWords.length &&
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
                            contentStream.setFont(font, fontSize);
                            contentStream.beginText();
                            y = page.getMediaBox().getHeight() - margin + height;
                            contentStream.newLineAtOffset(margin, y);
                        }

                        if (contentStream == null)
                        {
                            throw new IOException("Error:Expected non-null content stream.");
                        }
                        contentStream.newLineAtOffset(0, -height);
                        y -= height;
                        contentStream.showText(nextLineToDraw.toString());
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

        public void setFont(String aFontName)
        {
            PDType1Font font = PagedTextToPDF.getStandardFont(aFontName);

            if (font != null)
            {
                super.setFont(font);
                this.fontName = aFontName;
            }
        }

        /**
         * Gets the font that will be used in document transformation using the following approaches:
         * <ol>
         * <li>Standard font map
         * <li>Font Mappers
         * <li>File system fonts
         * <li>Transformer default font
         * <li>PdfBox default font
         * </ol>
         *
         * @param doc
         *            the document that will be transformed
         * @param fontName
         *            the font name that will be used in transformation
         *
         * @return the font that was found
         */
        private PDFont getFont(PDDocument doc, String fontName)
        {
            if (fontName == null)
            {
                fontName = fontName != null ? fontName : getDefaultFont();
            }

            // First, it tries to get the font from PdfBox STANDARD_14 map
            PDFont font = getFromStandardFonts(fontName);

            // If not found, tries to get the font from FontMappers
            if (font == null)
            {
                font = getFromFontMapper(fontName, doc);

                // If still not found, tries to get the font from file system
                if (font == null)
                {
                    font = getFromFileSystem(fontName);

                    // If font is still null:
                    // - it will recursively get the transformer default font
                    // - Otherwise, it will use the PdfBox default font (Helvetica)
                    if (font == null)
                    {
                        if (defaultFont != null && !fontName.equals(defaultFont))
                        {
                            font = getFont(doc, defaultFont);
                        }
                        else
                        {
                            font = getFont();
                        }
                    }
                }

            }

            return font;
        }

        /**
         * Gets the font from PdfBox standard fonts map
         *
         * @param fontName
         *            the font name to obtain
         *
         * @return the font object that has been found, otherwise null
         */
        private PDFont getFromStandardFonts(String fontName)
        {
            return PagedTextToPDF.getStandardFont(fontName);
        }

        /**
         * Gets the font from {@link FontMappers} instance
         *
         * @param fontName
         *            the font name to obtain
         * @param doc
         *            the PDF document
         *
         * @return the font object that has been found, otherwise null
         */
        private PDFont getFromFontMapper(String fontName, PDDocument doc)
        {
            PDFont font = null;
            FontMapping<TrueTypeFont> mapping = FontMappers.instance().getTrueTypeFont(fontName, null);

            if (mapping != null && mapping.getFont() != null && !mapping.isFallback())
            {
                try
                {
                    font = PDType0Font.load(doc, mapping.getFont().getOriginalData());
                }
                catch (Exception e)
                {
                    logger.error("Error loading font mapping {}", fontName, e);
                }
            }

            return font;
        }

        /**
         * Gets the font from existing file system fonts
         *
         * @param fontName
         *            the font name to obtain
         * @return the font object that has been found, otherwise null
         */
        private PDFont getFromFileSystem(String fontName)
        {
            PDFont font = null;
            String nameWithExtension = fontName + ".ttf";

            FontFileFinder fontFileFinder = new FontFileFinder();
            List<URI> uris = fontFileFinder.find();

            for (URI uri : uris)
            {
                if (uri.getPath().contains(nameWithExtension))
                {
                    InputStream fontIS = null;
                    try
                    {
                        fontIS = new FileInputStream(new File(uri));
                        if (null != fontIS)
                        {
                            PDDocument documentMock = new PDDocument();
                            font = PDType0Font.load(documentMock, fontIS);
                            break;
                        }
                    }
                    catch (IOException ioe)
                    {
                        logger.error("Error loading font {} from filesystem", fontName, ioe);
                    }
                    finally
                    {
                        if (fontIS != null)
                        {
                            try
                            {
                                fontIS.close();
                            }
                            catch (Exception e)
                            {
                                logger.error("Error closing font inputstream", e);
                            }
                        }
                    }
                }
            }

            return font;
        }

        public String getFontName()
        {
            return this.fontName;
        }

        public String getDefaultFont()
        {
            if (defaultFont == null || defaultFont.isBlank())
            {
                return TextToPdfContentTransformer.DEFAULT_FONT;
            }

            return defaultFont;
        }

        public void setDefaultFont(String name)
        {
            if (name == null || name.isBlank())
            {
                defaultFont = TextToPdfContentTransformer.DEFAULT_FONT;
            }
            else
            {
                this.defaultFont = name;
            }
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

    /**
     * Skips the BOM character for UTF-8 encoding
     */
    private InputStream handleUTF8BOM(InputStream is)
    {
        return new PushbackInputStream(is, UTF8_READ_AHEAD_BYTES) {
            boolean bomRead;

            @Override
            public int read(byte[] bytes, int off, int len) throws IOException
            {
                int i = 0;
                int b = 0;
                for (; i < len; i++)
                {
                    b = read();
                    if (b == -1)
                    {
                        break;
                    }
                    bytes[off + i] = (byte) b;
                }
                return i == 0 && b == -1 ? -1 : i;
            }

            @Override
            public int read() throws IOException
            {
                if (!bomRead)
                {
                    bomRead = true;
                    byte[] bytes = new byte[UTF8_READ_AHEAD_BYTES];
                    int end = in.read(bytes, 0, UTF8_READ_AHEAD_BYTES);

                    if (bytes[0] == EF && bytes[1] == BB && bytes[2] == BF)
                    {
                        logger.warn("UTF-8 BOM detected, it will be skipped");
                    }
                    else
                    {
                        for (int i = end - 1; i >= 0; i--)
                        {
                            unread(bytes[i]);
                        }
                    }
                }

                return super.read();
            }
        };
    }

    /**
     * Handles the situation where there is a BOM even though the encoding indicates that normally there should not be one for UTF-16BE and UTF-16LE. For extra flexibility includes UTF-16 too which optionally has the BOM. Rather than look at the BOM we look at the number of zero bytes in the first few character. XML files even when not in European languages tend to have more even zero bytes when big-endian encoded and more odd zero bytes when little-endian. Think of: <?xml version="1.0"?> The normal Java decoder does not have this flexibility but other transformers do.
     */
    private InputStream handleUTF16BOM(InputStream is)
    {
        return new PushbackInputStream(is, UTF16_READ_AHEAD_BYTES) {
            boolean bomRead;
            boolean switchByteOrder;
            boolean evenByte = true;

            @Override
            public int read(byte[] bytes, int off, int len) throws IOException
            {
                int i = 0;
                int b = 0;
                for (; i < len; i++)
                {
                    b = read();
                    if (b == -1)
                    {
                        break;
                    }
                    bytes[off + i] = (byte) b;
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

                    for (int i = end - 1; i >= 0; i--)
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
                for (int i = offset; i < UTF16_READ_AHEAD_BYTES; i += 2)
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
}
