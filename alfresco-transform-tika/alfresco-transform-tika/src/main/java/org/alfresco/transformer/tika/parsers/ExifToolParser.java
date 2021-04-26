/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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
package org.alfresco.transformer.tika.parsers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IMAGE_TIFF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.IOUtils;
import org.apache.tika.io.NullOutputStream;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.external.ExternalParser;
import org.apache.tika.parser.external.ExternalParsersFactory;
import org.apache.tika.parser.image.ImageParser;
import org.apache.tika.parser.image.TiffParser;
import org.apache.tika.parser.jpeg.JpegParser;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class ExifToolParser extends ExternalParser {
    
    private static final String EXIFTOOL_PARSER_CONFIG = "parsers/external/config/exiftool-parser.xml";

    public ExifToolParser() throws IOException, TikaException {
        super();
        ExternalParser eParser = ExternalParsersFactory.create(getExternalParserConfigURL()).get(0);
        this.setCommand(eParser.getCommand());
        this.setIgnoredLineConsumer(eParser.getIgnoredLineConsumer());
        this.setMetadataExtractionPatterns(eParser.getMetadataExtractionPatterns());
        this.setSupportedTypes(eParser.getSupportedTypes());
    }
    
    private URL getExternalParserConfigURL(){
        ClassLoader classLoader = ExifToolParser.class.getClassLoader();
        return classLoader.getResource(EXIFTOOL_PARSER_CONFIG);
    }

    /**
     * Adapted from {@link org.apache.tika.parser.external.ExternalParser} 
     * due to errors attempting to {@link #extractMetadata} from the errorStream in original implementation.  <p>
     * Executes the configured external command and passes the given document
     *  stream as a simple XHTML document to the given SAX content handler.
     * Metadata is only extracted if {@link #setMetadataExtractionPatterns(Map)}
     *  has been called to set patterns.
     */
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context)
            throws IOException, SAXException, TikaException {
        XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);

        MediaType mediaType = MediaType.parse(metadata.get(Metadata.CONTENT_TYPE));
        TemporaryResources tmp = new TemporaryResources();
        try {
            TikaInputStream tis = TikaInputStream.get(stream, tmp);
            parse(tis, xhtml, metadata, tmp);
            switch (mediaType.getType()+"/"+mediaType.getSubtype()) {
                case MIMETYPE_IMAGE_JPEG: 
                    parseAdditional(new JpegParser(), tis, handler, metadata, context, mediaType);
                    break;
                case MIMETYPE_IMAGE_TIFF:
                    parseAdditional(new TiffParser(), tis, handler, metadata, context, mediaType);
                    break;
                default:
                    parseAdditional(new ImageParser(), tis, handler, metadata, context, mediaType);
            }
        } finally {
            tmp.dispose();
        }
    }

    private void parseAdditional(Parser parser, TikaInputStream tis, ContentHandler handler, Metadata metadata, ParseContext context,
            MediaType mediaType) throws IOException, SAXException, TikaException {
        if (parser.getSupportedTypes(context).contains(mediaType)) {
                parser.parse(tis, handler, metadata, context);
        }
    }

    private void parse(TikaInputStream stream, XHTMLContentHandler xhtml, Metadata metadata, TemporaryResources tmp)
            throws IOException, SAXException, TikaException {
        boolean inputToStdIn = true;
        boolean outputFromStdOut = true;
        boolean hasPatterns = (getMetadataExtractionPatterns() != null && !getMetadataExtractionPatterns().isEmpty());

        File output = null;

        // Build our getCommand()
        String[] cmd;
        if (getCommand().length == 1) {
            cmd = getCommand()[0].split(" ");
        } else {
            cmd = new String[getCommand().length];
            System.arraycopy(getCommand(), 0, cmd, 0, getCommand().length);
        }
        for (int i = 0; i < cmd.length; i++) {
            if (cmd[i].indexOf(INPUT_FILE_TOKEN) != -1) {
                cmd[i] = cmd[i].replace(INPUT_FILE_TOKEN, stream.getFile().getPath());
                inputToStdIn = false;
            }
            if (cmd[i].indexOf(OUTPUT_FILE_TOKEN) != -1) {
                output = tmp.createTemporaryFile();
                outputFromStdOut = false;
                cmd[i] = cmd[i].replace(OUTPUT_FILE_TOKEN, output.getPath());
            }
        }

        // Execute
        Process process = null;
        try {
            if (cmd.length == 1) {
                process = Runtime.getRuntime().exec(cmd[0]);
            } else {
                process = Runtime.getRuntime().exec(cmd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (inputToStdIn) {
                sendInput(process, stream);
            } else {
                process.getOutputStream().close();
            }

            InputStream out = process.getInputStream();
            InputStream err = process.getErrorStream();

            if (hasPatterns) {

                if (outputFromStdOut) {
                    extractOutput(out, xhtml);
                } else {
                    extractMetadata(out, metadata);
                }
            } else {
                ignoreStream(err);

                if (outputFromStdOut) {
                    extractOutput(out, xhtml);
                } else {
                    ignoreStream(out);
                }
            }
        } finally {
            try {
                process.waitFor();
            } catch (InterruptedException ignore) {
            }
        }

        // Grab the output if we haven't already
        if (!outputFromStdOut) {
            extractOutput(new FileInputStream(output), xhtml);
        }
    }

    /**
     * Adapted from {@link org.apache.tika.parser.external.ExternalParser}<p>
     * Starts a thread that extracts the contents of the standard output
     * stream of the given process to the given XHTML content handler.
     * The standard output stream is closed once fully processed.
     *
     * @param process process
     * @param xhtml XHTML content handler
     * @throws SAXException if the XHTML SAX events could not be handled
     * @throws IOException if an input error occurred
     */
    private void extractOutput(InputStream stream, XHTMLContentHandler xhtml) throws SAXException, IOException {
        try (Reader reader = new InputStreamReader(stream, UTF_8)) {
            xhtml.startDocument();
            xhtml.startElement("p");
            char[] buffer = new char[1024];
            for (int n = reader.read(buffer); n != -1; n = reader.read(buffer)) {
                xhtml.characters(buffer, 0, n);
            }
            xhtml.endElement("p");
            xhtml.endDocument();
        }
    }

    /**
     * Adapted from {@link org.apache.tika.parser.external.ExternalParser}<p>
     * Starts a thread that sends the contents of the given input stream
     * to the standard input stream of the given process. Potential
     * exceptions are ignored, and the standard input stream is closed
     * once fully processed. Note that the given input stream is <em>not</em>
     * closed by this method.
     *
     * @param process process
     * @param stream input stream
     */
    private void sendInput(final Process process, final InputStream stream) {
        Thread t = new Thread() {
            public void run() {
                OutputStream stdin = process.getOutputStream();
                try {
                    IOUtils.copy(stream, stdin);
                } catch (IOException e) {
                }
            }
        };
        t.start();
        try {
            t.join();
        } catch (InterruptedException ignore) {
        }
    }

    /**
     * Adapted from {@link org.apache.tika.parser.external.ExternalParser}<p>
     * Starts a thread that reads and discards the contents of the
     * standard stream of the given process. Potential exceptions
     * are ignored, and the stream is closed once fully processed.
     *
     * @param process process
     */
    private void ignoreStream(final InputStream stream) {
        Thread t = new Thread() {
            public void run() {
                try {
                    IOUtils.copy(stream, new NullOutputStream());
                } catch (IOException e) {
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }
        };
        t.start();
        try {
            t.join();
        } catch (InterruptedException ignore) {
        }
    }

    private void extractMetadata(final InputStream stream, final Metadata metadata) {
        Thread t = new Thread() {
            public void run() {
                BufferedReader reader;
                reader = new BufferedReader(new InputStreamReader(stream, UTF_8));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        for (Pattern p : getMetadataExtractionPatterns().keySet()) {
                            Matcher m = p.matcher(line);
                            if (m.find()) {
                                if (getMetadataExtractionPatterns().get(p) != null
                                        && !getMetadataExtractionPatterns().get(p).equals("")) {
                                    metadata.add(getMetadataExtractionPatterns().get(p), m.group(1));
                                } else {
                                    metadata.add(m.group(1), m.group(2));
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    // Ignore
                } finally {
                    IOUtils.closeQuietly(reader);
                    IOUtils.closeQuietly(stream);
                }
            }
        };
        t.start();
        try {
            t.join();
        } catch (InterruptedException ignore) {
        }
    }
}
