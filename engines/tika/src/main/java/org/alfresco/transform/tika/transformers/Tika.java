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
package org.alfresco.transform.tika.transformers;

import com.google.common.collect.ImmutableList;
import org.alfresco.transform.tika.parsers.TikaOfficeDetectParser;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.DocumentSelector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.EmptyParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.parser.pkg.PackageParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ExpandedTitleContentHandler;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_TIFF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_CSV;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_XHTML;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_XML;

@Component
public class Tika
{
    public static final String ARCHIVE = "Archive";
    public static final String OUTLOOK_MSG = "OutlookMsg";
    public static final String PDF_BOX = "PdfBox";
    public static final String OFFICE = "Office";
    public static final String POI = "Poi";
    public static final String OOXML = "OOXML";
    public static final String TIKA_AUTO = "TikaAuto";
    public static final String TEXT_MINING = "TextMining";

    public static final String TARGET_MIMETYPE = "--targetMimetype=";
    public static final String TARGET_ENCODING = "--targetEncoding=";
    public static final String INCLUDE_CONTENTS = "--includeContents";
    public static final String NOT_EXTRACT_BOOKMARKS_TEXT = "--notExtractBookmarksText";

    public static final String CSV = "csv";
    public static final String DOC = "doc";
    public static final String DOCX = "docx";
    public static final String HTML = "html";
    public static final String MSG = "msg";
    public static final String PDF = "pdf";
    public static final String PPTX = "pptx";
    public static final String TXT = "txt";
    public static final String XHTML = "xhtml";
    public static final String XSLX = "xslx";
    public static final String XML = "xml";
    public static final String ZIP = "zip";

    public static final Parser packageParser = new PackageParser();
    public static final Parser pdfParser = new PDFParser();
    public static final Parser officeParser = new OfficeParser();
    public final Parser autoDetectParser;
    public static final Parser ooXmlParser = new OOXMLParser();
    public static final Parser tikaOfficeDetectParser = new TikaOfficeDetectParser();
    public final PDFParserConfig pdfParserConfig = new PDFParserConfig();

    public static final DocumentSelector pdfBoxEmbededDocumentSelector = new DocumentSelector()
    {
        private final List<String> disabledMediaTypes = ImmutableList.of(MIMETYPE_IMAGE_JPEG,
            MIMETYPE_IMAGE_TIFF, MIMETYPE_IMAGE_PNG);

        @Override
        public boolean select(Metadata metadata)
        {
            String contentType = metadata.get(Metadata.CONTENT_TYPE);
            if (contentType == null || contentType.equals("") || disabledMediaTypes == null)
            {
                return true;
            }
            return !disabledMediaTypes.contains(contentType);
        }
    };

    public Tika() throws TikaException, IOException, SAXException
    {
        TikaConfig tikaConfig = readTikaConfig();
        autoDetectParser = new AutoDetectParser(tikaConfig);
    }

    public static TikaConfig readTikaConfig(Logger logger)
    {
        try
        {
            return readTikaConfig();
        }
        catch (Exception e)
        {
            logger.error("Failed to read tika-config.xml", e);
            return null;
        }
    }

    private static TikaConfig readTikaConfig() throws TikaException, IOException, SAXException
    {
        ClassLoader classLoader = Tika.class.getClassLoader();
        URL tikaConfigXml = classLoader.getResource("tika-config.xml");
        return new TikaConfig(tikaConfigXml);
    }

    // Extracts parameters form args
    void transform(Parser parser, DocumentSelector documentSelector, InputStream inputStream,
            OutputStream outputStream, String[] args)
    {
        String targetMimetype = null;
        String targetEncoding = null;
        Boolean includeContents = null;
        Boolean notExtractBookmarksText = null;

        for (String arg : args)
        {
            if (Objects.isNull(arg))
            {
                // ignore
            }
            else if (arg.startsWith(INCLUDE_CONTENTS))
            {
                getValue(arg, false, includeContents, INCLUDE_CONTENTS);
                includeContents = true;
            }
            else if (arg.startsWith(TARGET_ENCODING))
            {
                targetEncoding = getValue(arg, true, targetEncoding, TARGET_ENCODING);
            }
            else if (arg.startsWith(TARGET_MIMETYPE))
            {
                targetMimetype = getValue(arg, true, targetMimetype, TARGET_MIMETYPE);
            }
            else if (arg.startsWith(NOT_EXTRACT_BOOKMARKS_TEXT))
            {
                getValue(arg, false, notExtractBookmarksText, NOT_EXTRACT_BOOKMARKS_TEXT);
                notExtractBookmarksText = true;
            }
            else
            {
                throw new IllegalArgumentException("Unexpected argument " + arg);
            }
        }
        includeContents = includeContents == null ? false : includeContents;
        notExtractBookmarksText = notExtractBookmarksText == null ? false : notExtractBookmarksText;

        transform(parser, documentSelector, includeContents, notExtractBookmarksText, inputStream,
            outputStream, targetMimetype, targetEncoding);
    }

    private String getValue(String arg, boolean valueExpected, Object value, String optionName)
    {
        if (value != null)
        {
            throw new IllegalArgumentException("Duplicate " + optionName);
        }
        String stringValue = arg.substring(optionName.length()).trim();
        if (!valueExpected && stringValue.length() > 0)
        {
            throw new IllegalArgumentException("Unexpected value with " + optionName);
        }
        if (valueExpected && stringValue.length() == 0)
        {
            throw new IllegalArgumentException("Expected value with " + optionName);
        }
        return stringValue;
    }

    private void transform(Parser parser, DocumentSelector documentSelector,
        Boolean includeContents,
        Boolean notExtractBookmarksText,
        InputStream inputStream,
        OutputStream outputStream, String targetMimetype, String targetEncoding)
    {
        try (Writer ow = new BufferedWriter(new OutputStreamWriter(outputStream, targetEncoding)))
        {
            Metadata metadata = new Metadata();
            ParseContext context = buildParseContext(documentSelector, includeContents,
                notExtractBookmarksText);
            ContentHandler handler = getContentHandler(targetMimetype, ow);

            parser.parse(inputStream, handler, metadata, context);
        }
        catch (SAXException | TikaException | IOException e)
        {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private ContentHandler getContentHandler(String targetMimetype, Writer output)
    {
        try
        {
            ContentHandler handler;
            if (MIMETYPE_TEXT_PLAIN.equals(targetMimetype))
            {
                handler = new BodyContentHandler(output);
            }
            else
            {
                SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
                TransformerHandler transformerHandler;
                transformerHandler = factory.newTransformerHandler();
                transformerHandler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
                transformerHandler.setResult(new StreamResult(output));
                handler = transformerHandler;

                if (MIMETYPE_HTML.equals(targetMimetype))
                {
                    transformerHandler.getTransformer().setOutputProperty(OutputKeys.METHOD, HTML);
                    return new ExpandedTitleContentHandler(transformerHandler);
                }
                else if (MIMETYPE_XHTML.equals(targetMimetype) ||
                         MIMETYPE_XML.equals(targetMimetype))
                {
                    transformerHandler.getTransformer().setOutputProperty(OutputKeys.METHOD, XML);
                }
                else if (MIMETYPE_TEXT_CSV.equals(targetMimetype))
                {
                    handler = new CsvContentHandler(output);
                }
                else
                {
                    throw new IllegalArgumentException("Invalid target mimetype " + targetMimetype);
                }
            }
            return handler;
        }
        catch (TransformerConfigurationException e)
        {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * A wrapper around the normal Tika BodyContentHandler for CSV rather encoding than tab separated.
     */
    protected static class CsvContentHandler extends BodyContentHandler
    {
        private static final char[] comma = new char[]{','};
        private static final Pattern all_nums = Pattern.compile("[\\d\\.\\-\\+]+");

        private boolean inCell = false;
        private boolean needsComma = false;

        protected CsvContentHandler(Writer output)
        {
            super(output);
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException
        {
            if (length == 1 && ch[0] == '\t')
            {
                // Ignore tabs, as they mess up the CSV output
            }
            else
            {
                super.ignorableWhitespace(ch, start, length);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length)
            throws SAXException
        {
            if (inCell)
            {
                StringBuffer t = new StringBuffer(new String(ch, start, length));

                // Quote if not all numbers
                if (all_nums.matcher(t).matches())
                {
                    super.characters(ch, start, length);
                }
                else
                {
                    for (int i = t.length() - 1; i >= 0; i--)
                    {
                        if (t.charAt(i) == '\"')
                        {
                            // Double up double quotes
                            t.insert(i, '\"');
                            i--;
                        }
                    }
                    t.insert(0, '\"');
                    t.append('\"');
                    char[] c = t.toString().toCharArray();
                    super.characters(c, 0, c.length);
                }
            }
            else
            {
                super.characters(ch, start, length);
            }
        }

        @Override
        public void startElement(String uri, String localName, String name,
            Attributes atts) throws SAXException
        {
            if (localName.equals("td"))
            {
                inCell = true;
                if (needsComma)
                {
                    super.characters(comma, 0, 1);
                    needsComma = true;
                }
            }
            else
            {
                super.startElement(uri, localName, name, atts);
            }
        }

        @Override
        public void endElement(String uri, String localName, String name)
            throws SAXException
        {
            if (localName.equals("td"))
            {
                needsComma = true;
                inCell = false;
            }
            else
            {
                if (localName.equals("tr"))
                {
                    needsComma = false;
                }
                super.endElement(uri, localName, name);
            }
        }
    }

    private ParseContext buildParseContext(DocumentSelector documentSelector,
        Boolean includeContents, Boolean notExtractBookmarksText)
    {
        ParseContext context = new ParseContext();

        if (documentSelector != null)
        {
            context.set(DocumentSelector.class, documentSelector);
        }

        if (notExtractBookmarksText.equals(true))
        {
            pdfParserConfig.setExtractBookmarksText(false);
            // pdfParserConfig is set to override default settings
            context.set(PDFParserConfig.class, pdfParserConfig);
        }

        // If Archive transform
        if (includeContents != null)
        {
            context.set(Parser.class, includeContents ? autoDetectParser : new EmptyParser());
        }

        return context;
    }
}
