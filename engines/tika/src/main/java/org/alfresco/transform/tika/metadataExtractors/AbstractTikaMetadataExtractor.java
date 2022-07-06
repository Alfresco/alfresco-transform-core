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
package org.alfresco.transform.tika.metadataExtractors;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.metadataExtractors.AbstractMetadataExtractor;
import org.apache.tika.embedder.Embedder;
import org.apache.tika.extractor.DocumentSelector;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.apache.tika.sax.XHTMLContentHandler;
import org.apache.tika.sax.xpath.Matcher;
import org.apache.tika.sax.xpath.MatchingContentHandler;
import org.apache.tika.sax.xpath.XPathParser;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The parent of all Metadata Extractors which use Apache Tika under the hood. This handles all the
 * common parts of processing the files, and the common mappings.

 * <pre>
 *   <b>author:</b>                 --      cm:author
 *   <b>title:</b>                  --      cm:title
 *   <b>subject:</b>                --      cm:description
 *   <b>created:</b>                --      cm:created
 *   <b>comments:</b>
 * </pre>
 *
 * @author Nick Burch
 * @author adavis
 */
public abstract class AbstractTikaMetadataExtractor extends AbstractMetadataExtractor
{
    protected static final String KEY_AUTHOR = "author";
    protected static final String KEY_TITLE = "title";
    protected static final String KEY_SUBJECT = "subject";
    protected static final String KEY_CREATED = "created";
    protected static final String KEY_DESCRIPTION = "description";
    protected static final String KEY_COMMENTS = "comments";
    protected static final String KEY_TAGS = DublinCore.SUBJECT.getName();

    private static final String METADATA_SEPARATOR = ",";

    private final DateTimeFormatter tikaUTCDateFormater;
    private final DateTimeFormatter tikaDateFormater;

    public AbstractTikaMetadataExtractor(Type type, Logger logger)
    {
        super(type, logger);

        // TODO Once TIKA-451 is fixed this list will get nicer
        DateTimeParser[] parsersUTC = {
                DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").getParser(),
                DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").getParser()
        };
        DateTimeParser[] parsers = {
                DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").getParser(),
                DateTimeFormat.forPattern("yyyy-MM-dd").getParser(),
                DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss").getParser(),
                DateTimeFormat.forPattern("yyyy/MM/dd").getParser(),
                DateTimeFormat.forPattern("EEE MMM dd hh:mm:ss zzz yyyy").getParser()
        };

        tikaUTCDateFormater = new DateTimeFormatterBuilder().append(null, parsersUTC).toFormatter().withZone(DateTimeZone.UTC);
        tikaDateFormater = new DateTimeFormatterBuilder().append(null, parsers).toFormatter();
    }

    /**
     * Version which also tries the ISO-8601 formats (in order..),
     *  and similar formats, which Tika makes use of
     */
    protected Serializable makeDate(String dateStr)
    {
        // Try our formats first, in order
        try
        {
            return this.tikaUTCDateFormater.parseDateTime(dateStr).toDate();
        }
        catch (IllegalArgumentException ignore) {}

        try
        {
            return this.tikaUTCDateFormater.withLocale(Locale.US).parseDateTime(dateStr).toDate();
        }
        catch (IllegalArgumentException ignore) {}

        try
        {
            return this.tikaDateFormater.parseDateTime(dateStr).toDate();
        }
        catch (IllegalArgumentException ignore) {}

        try
        {
            return this.tikaDateFormater.withLocale(Locale.US).parseDateTime(dateStr).toDate();
        }
        catch (IllegalArgumentException ignore) {}

        // Fall back to the normal ones: We just return the String as AbstractMappingMetadataExtracter
        // convertSystemPropertyValues in the repo will do the conversion that was previously done here.
        return dateStr;
    }

    /**
     * Returns the correct Tika Parser to process the document.
     * If you don't know which you want, use {@link TikaAutoMetadataExtractor}
     * which makes use of the Tika auto-detection.
     */
    protected abstract Parser getParser();

    /**
     * Returns the Tika Embedder to modify
     * the document.
     *
     * @return the Tika embedder
     */
    protected Embedder getEmbedder()
    {
        // TODO make this an abstract method once more extracters support embedding
        return null;
    }

    /**
     * Do we care about the contents of the
     *  extracted header, or nothing at all?
     */
    protected boolean needHeaderContents()
    {
        return false;
    }

    /**
     * Allows implementation specific mappings to be done.
     */
    protected Map<String, Serializable> extractSpecific(Metadata metadata,
                                                        Map<String, Serializable> properties, Map<String,String> headers)
    {
        return properties;
    }

    /**
     * Gets the document selector, used for determining whether to parse embedded resources,
     * null by default so parse all.
     */
    protected DocumentSelector getDocumentSelector(Metadata metadata, String targetMimeType)
    {
        return null;
    }

    /**
     * By default returns a new ParseContent
     */
    private ParseContext buildParseContext(Metadata metadata, String sourceMimeType)
    {
        ParseContext context = new ParseContext();
        DocumentSelector selector = getDocumentSelector(metadata, sourceMimeType);
        if (selector != null)
        {
            context.set(DocumentSelector.class, selector);
        }
        return context;
    }

    @Override
    public Map<String, Serializable> extractMetadata(String sourceMimetype, Map<String, String> transformOptions,
                                                     File sourceFile) throws Exception
    {
        Map<String, Serializable> rawProperties = new HashMap<>();

        try (InputStream is = new FileInputStream(sourceFile))
        {
            Parser parser = getParser();

            Metadata metadata = new Metadata();
            metadata.add(Metadata.CONTENT_TYPE, sourceMimetype);

            ParseContext context = buildParseContext(metadata, sourceMimetype);

            ContentHandler handler;
            Map<String,String> headers = null;
            if (needHeaderContents())
            {
                MapCaptureContentHandler headerCapture =
                        new MapCaptureContentHandler();
                headers = headerCapture.tags;
                handler = new HeadContentHandler(headerCapture);
            }
            else
            {
                handler = new NullContentHandler();
            }

            parser.parse(is, handler, metadata, context);

            // First up, copy all the Tika metadata over
            // This allows people to map any of the Tika
            //  keys onto their own content model
            for (String tikaKey : metadata.names())
            {
                // TODO review this change (part of MNT-15267) - should we really force string concatenation here !?
                putRawValue(tikaKey, getMetadataValue(metadata, Property.internalText(tikaKey)), rawProperties);
            }

            // Now, map the common Tika metadata keys onto
            //  the common Alfresco metadata keys. This allows
            //  existing mapping properties files to continue
            //  to work without needing any changes

            // The simple ones
            putRawValue(KEY_AUTHOR, getMetadataValue(metadata, TikaCoreProperties.CREATOR), rawProperties);
            putRawValue(KEY_TITLE, getMetadataValue(metadata, TikaCoreProperties.TITLE), rawProperties);
            putRawValue(KEY_COMMENTS, getMetadataValue(metadata, TikaCoreProperties.COMMENTS), rawProperties);

            // Tags
            putRawValue(KEY_TAGS, getMetadataValues(metadata, KEY_TAGS), rawProperties);

            // Get the subject and description, despite things not
            //  being nearly as consistent as one might hope
            String subject = getMetadataValue(metadata, TikaCoreProperties.SUBJECT);
            String description = getMetadataValue(metadata, TikaCoreProperties.DESCRIPTION);
            if (subject != null && description != null)
            {
                putRawValue(KEY_DESCRIPTION, description, rawProperties);
                putRawValue(KEY_SUBJECT, subject, rawProperties);
            }
            else if (subject != null)
            {
                putRawValue(KEY_DESCRIPTION, subject, rawProperties);
                putRawValue(KEY_SUBJECT, subject, rawProperties);
            }
            else if (description != null)
            {
                putRawValue(KEY_DESCRIPTION, description, rawProperties);
                putRawValue(KEY_SUBJECT, description, rawProperties);
            }

            // Try for the dates two different ways too
            if (metadata.get(TikaCoreProperties.CREATED) != null)
            {
                putRawValue(KEY_CREATED, metadata.get(TikaCoreProperties.CREATED), rawProperties);
            }
            else if (metadata.get(TikaCoreProperties.MODIFIED) != null)
            {
                putRawValue(KEY_CREATED, metadata.get(TikaCoreProperties.MODIFIED), rawProperties);
            }

            // If people created a specific instance
            //  (eg OfficeMetadataExtractor), then allow that
            //  instance to map the Tika keys onto its
            //  existing namespace so that older properties
            //  files continue to map correctly
            rawProperties = extractSpecific(metadata, rawProperties, headers);
        }

        return rawProperties;
    }

    /**
     * @deprecated The content repository's TikaPoweredMetadataExtracter provides no non test implementations.
     *             This code exists in case there are custom implementations, that need to be converted to T-Engines.
     *             It is simply a copy and paste from the content repository and has received limited testing.
     */
    @Override
    public void embedMetadata(String sourceMimetype, InputStream inputStream,
            String targetMimetype, OutputStream outputStream,
            Map<String, String> transformOptions, TransformManager transformManager) throws Exception
    {
        Embedder embedder = getEmbedder();
        if (embedder == null)
        {
            return;
        }

        Metadata metadataToEmbed = getTikaMetadata(transformOptions);
        embedder.embed(metadataToEmbed, inputStream, outputStream, null);
    }

    private Metadata getTikaMetadata(Map<String, String> transformOptions)
    {
        Metadata metadataToEmbed = new Metadata();
        Map<String, Serializable> properties = getMetadata(transformOptions);
        for (String metadataKey : properties.keySet())
        {
            Serializable value = properties.get(metadataKey);
            if (value == null)
            {
                continue;
            }
            if (value instanceof Collection<?>)
            {
                for (Object singleValue : (Collection<?>) value)
                {
                    try
                    {
                        metadataToEmbed.add(metadataKey, (String)singleValue);
                    }
                    catch (ClassCastException e)
                    {
                        logger.info("Could not convert " + metadataKey + ": " + e.getMessage());
                    }
                }
            }
            else
            {
                try
                {
                    metadataToEmbed.add(metadataKey, (String)value);
                }
                catch (ClassCastException e)
                {
                    logger.info("Could not convert " + metadataKey + ": " + e.getMessage());
                }
            }
        }
        return metadataToEmbed;
    }

    private Serializable getMetadataValues(Metadata metadata, String key)
    {
        // Use Set to prevent duplicates.
        Set<String> valuesSet = new LinkedHashSet<String>();
        String[] values = metadata.getValues(key);

        for (int i = 0; i < values.length; i++)
        {
            String[] parts = values[i].split(METADATA_SEPARATOR);

            for (String subPart : parts)
            {
                valuesSet.add(subPart.trim());
            }
        }

        Object[] objArrayValues = valuesSet.toArray();
        values = Arrays.copyOf(objArrayValues, objArrayValues.length, String[].class);

        return values.length == 0 ? null : (values.length == 1 ? values[0] : values);
    }

    private String getMetadataValue(Metadata metadata, Property key)
    {
        if (metadata.isMultiValued(key))
        {
            return distinct(metadata.getValues(key)).collect(Collectors.joining(", "));
        }
        else
        {
            return metadata.get(key);
        }
    }

    protected static Stream<String> distinct(final String[] strings)
    {
        return Stream.of(strings)
                     .filter(Objects::nonNull)
                     .map(String::strip)
                     .filter(s -> !s.isEmpty())
                     .distinct();
    }

    /**
     * This content handler will capture entries from within
     *  the header of the Tika content XHTML, but ignore the
     *  rest.
     */
    protected static class HeadContentHandler extends ContentHandlerDecorator
    {
        /**
         * XHTML XPath parser.
         */
        private static final XPathParser PARSER =
                new XPathParser("xhtml", XHTMLContentHandler.XHTML);

        /**
         * The XPath matcher used to select the XHTML body contents.
         */
        private static final Matcher MATCHER =
                PARSER.parse("/xhtml:html/xhtml:head/descendant:node()");

        /**
         * Creates a content handler that passes all XHTML body events to the
         * given underlying content handler.
         *
         * @param handler content handler
         */
        protected HeadContentHandler(ContentHandler handler)
        {
            super(new MatchingContentHandler(handler, MATCHER));
        }
    }
    /**
     * This content handler will grab all tags and attributes,
     *  and record the textual content of the last seen one
     *  of them.
     * Normally only used with {@link HeadContentHandler}
     */
    protected static class MapCaptureContentHandler implements ContentHandler
    {
        protected Map<String, String> tags = new HashMap<>();
        private StringBuffer text;

        public void characters(char[] ch, int start, int len)
        {
            if (text != null)
            {
                text.append(ch, start, len);
            }
        }

        public void endElement(String namespace, String localname, String qname)
        {
            if (text != null && text.length() > 0)
            {
                tags.put(qname, text.toString());
            }
            text = null;
        }

        public void startElement(String namespace, String localname, String qname, Attributes attrs)
        {
            for(int i=0; i<attrs.getLength(); i++)
            {
                tags.put(attrs.getQName(i), attrs.getValue(i));
            }
            text = new StringBuffer();
        }

        public void endDocument() {}
        public void endPrefixMapping(String paramString) {}
        public void ignorableWhitespace(char[] paramArrayOfChar, int paramInt1, int paramInt2) {}
        public void processingInstruction(String paramString1, String paramString2) {}
        public void setDocumentLocator(Locator paramLocator) {}
        public void skippedEntity(String paramString) {}
        public void startDocument() {}
        public void startPrefixMapping(String paramString1, String paramString2) {}
    }

    /**
     * A content handler that ignores all the content it finds.
     * Normally used when we only want the metadata, and don't
     *  care about the file contents.
     */
    protected static class NullContentHandler implements ContentHandler
    {
        public void characters(char[] paramArrayOfChar, int paramInt1, int paramInt2) {}
        public void endDocument() {}
        public void endElement(String paramString1, String paramString2, String paramString3) {}
        public void endPrefixMapping(String paramString) {}
        public void ignorableWhitespace(char[] paramArrayOfChar, int paramInt1, int paramInt2) {}
        public void processingInstruction(String paramString1, String paramString2) {}
        public void setDocumentLocator(Locator paramLocator) {}
        public void skippedEntity(String paramString) {}
        public void startDocument()  {}
        public void startElement(String paramString1, String paramString2,
                                 String paramString3, Attributes paramAttributes) {}
        public void startPrefixMapping(String paramString1, String paramString2) {}
    }
}
