package org.alfresco.transformer.tika.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tika.metadata.Metadata;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.IOUtils;
import org.apache.tika.io.NullOutputStream;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.external.ExternalParser;
import org.apache.tika.parser.external.ExternalParsersFactory;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ExifToolParser extends ExternalParser {
    
    private static final String EXIFTOOL_PARSER_CONFIG = "parsers/external/config/exiftool-parser.xml";

    public ExifToolParser() throws IOException, TikaException {
        super();
        var parser = ExternalParsersFactory.create(getExternalParserConfigURL()).get(0);
        this.setCommand(parser.getCommand());
        this.setIgnoredLineConsumer(parser.getIgnoredLineConsumer());
        this.setMetadataExtractionPatterns(parser.getMetadataExtractionPatterns());
        this.setSupportedTypes(parser.getSupportedTypes());
    }
    
    private URL getExternalParserConfigURL(){
        ClassLoader classLoader = ExifToolParser.class.getClassLoader();
        return classLoader.getResource(EXIFTOOL_PARSER_CONFIG);
    }

    public void parse(
            InputStream stream, ContentHandler handler,
            Metadata metadata, ParseContext context)
            throws IOException, SAXException, TikaException {
        XHTMLContentHandler xhtml =
            new XHTMLContentHandler(handler, metadata);

        TemporaryResources tmp = new TemporaryResources();
        try {
            parse(TikaInputStream.get(stream, tmp),
                    xhtml, metadata, tmp);
        } finally {
            tmp.dispose();
        }
    }

    private void parse(
            TikaInputStream stream, XHTMLContentHandler xhtml,
            Metadata metadata, TemporaryResources tmp)
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
        for(int i=0; i<cmd.length; i++) {
           if(cmd[i].indexOf(INPUT_FILE_TOKEN) != -1) {
              cmd[i] = cmd[i].replace(INPUT_FILE_TOKEN, stream.getFile().getPath());
              inputToStdIn = false;
           }
           if(cmd[i].indexOf(OUTPUT_FILE_TOKEN) != -1) {
              output = tmp.createTemporaryFile();
              outputFromStdOut = false;
              cmd[i] = cmd[i].replace(OUTPUT_FILE_TOKEN, output.getPath());
           }
        }

        // Execute
        Process process = null;
      try{
        if(cmd.length == 1) {
           process = Runtime.getRuntime().exec( cmd[0] );
        } else {
           process = Runtime.getRuntime().exec( cmd );
        }
      }
      catch(Exception e){
    	  e.printStackTrace();
      }

        try {
            if(inputToStdIn) {
               sendInput(process, stream);
            } else {
               process.getOutputStream().close();
            }

            InputStream out = process.getInputStream();
            InputStream err = process.getErrorStream();
            
            if(hasPatterns) {
               //extractMetadata(err, metadata);
               
               if(outputFromStdOut) {
                  extractOutput(out, xhtml);
               } else {
                  extractMetadata(out, metadata);
               }
            } else {
               ignoreStream(err);
               
               if(outputFromStdOut) {
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
     * Starts a thread that extracts the contents of the standard output
     * stream of the given process to the given XHTML content handler.
     * The standard output stream is closed once fully processed.
     *
     * @param process process
     * @param xhtml XHTML content handler
     * @throws SAXException if the XHTML SAX events could not be handled
     * @throws IOException if an input error occurred
     */
    private void extractOutput(InputStream stream, XHTMLContentHandler xhtml)
            throws SAXException, IOException {
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
        try{
     	   t.join();
        }
        catch(InterruptedException ignore){}        
    }

    /**
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
        try{
     	   t.join();
        }
        catch(InterruptedException ignore){}
    }
    
    private void extractMetadata(final InputStream stream, final Metadata metadata) {
       Thread t = new Thread() {
          public void run() {
             BufferedReader reader;
              reader = new BufferedReader(new InputStreamReader(stream, UTF_8));
             try {
                String line;
                while ( (line = reader.readLine()) != null ) {
                   for(Pattern p : getMetadataExtractionPatterns().keySet()) {
                      Matcher m = p.matcher(line);
                      if(m.find()) {
                    	 if (getMetadataExtractionPatterns().get(p) != null && 
                    			 !getMetadataExtractionPatterns().get(p).equals("")){
                                   metadata.add( getMetadataExtractionPatterns().get(p), m.group(1) );
                    	 }
                    	 else{
                    		 metadata.add( m.group(1), m.group(2));
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
       try{
    	   t.join();
       }
       catch(InterruptedException ignore){}
    }
}
