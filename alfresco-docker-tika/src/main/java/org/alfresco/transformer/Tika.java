/*
 * #%L
 * Alfresco Enterprise Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 * #L%
 */
package org.alfresco.transformer;

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
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.alfresco.repo.content.MimetypeMap.*;

/**
 * Stripped down command line Tika transformers. Not actually run as a separate process, but the code fits the patten
 * used by transformers that do.
 * <pre>
 *
 *           Archive 0 ms
 *               1) cpio html [100] unlimited
 *               2) cpio txt   [50] unlimited
 *               3) cpio xhtml [100] unlimited
 *               4) cpio xml  [100] unlimited
 *               5) jar  html [100] unlimited
 *               6) jar  txt   [50] unlimited
 *               7) jar  xhtml [100] unlimited
 *               8) jar  xml  [100] unlimited
 *               9) tar  html [100] unlimited
 *              10) tar  txt   [50] unlimited
 *              11) tar  xhtml [100] unlimited
 *              12) tar  xml  [100] unlimited
 *              13) zip  html [100] unlimited
 *              14) zip  txt   [50] unlimited
 *              15) zip  xhtml [100] unlimited
 *              16) zip  xml  [100] unlimited
 *           PdfBox 0 ms
 *               1) pdf  html [110] unlimited
 *               2) pdf  txt   [50] 25 MB
 *               3) pdf  xhtml [110] unlimited
 *               4) pdf  xml  [110] unlimited
 *           OutlookMsg 0 ms
 *               1) msg  html [125] unlimited
 *               2) msg  txt  [125] unlimited
 *               3) msg  xhtml [125] unlimited
 *               4) msg  xml  [125] unlimited
 *           PdfBox 0 ms
 *               1) pdf  html [110] unlimited
 *               2) pdf  txt   [50] 25 MB
 *               3) pdf  xhtml [110] unlimited
 *               4) pdf  xml  [110] unlimited
 *           Office 0 ms
 *               1) doc  html [130] unlimited
 *               2) doc  txt  [130] unlimited
 *               3) doc  xhtml [130] unlimited
 *               4) doc  xml  [130] unlimited
 *               5) mpp  html [130] unlimited
 *               6) mpp  txt  [130] unlimited
 *               7) mpp  xhtml [130] unlimited
 *               8) mpp  xml  [130] unlimited
 *               9) msg  html [130] unlimited
 *              10) msg  txt  [130] unlimited
 *              11) msg  xhtml [130] unlimited
 *              12) msg  xml  [130] unlimited
 *              13) ppt  html [130] unlimited
 *              14) ppt  txt  [130] unlimited
 *              15) ppt  xhtml [130] unlimited
 *              16) ppt  xml  [130] unlimited
 *              17) vsd  html [130] unlimited
 *              18) vsd  txt  [130] unlimited
 *              19) vsd  xhtml [130] unlimited
 *              20) vsd  xml  [130] unlimited
 *           Poi 0 ms
 *               1) xls  csv  [130] unlimited
 *               2) xls  html [130] unlimited
 *               3) xls  txt  [130] unlimited
 *               4) xls  xhtml [130] unlimited
 *               5) xls  xml  [130] unlimited
 *               6) xlsx csv  [130] unlimited
 *               7) xlsx html [130] unlimited
 *               8) xlsx txt  [130] unlimited
 *               9) xlsx xhtml [130] unlimited
 *              10) xlsx xml  [130] unlimited
 *           OOXML 0 ms
 *               1) docm html [130] unlimited
 *               2) docm txt  [130] unlimited
 *               3) docm xhtml [130] unlimited
 *               4) docm xml  [130] unlimited
 *               5) docx html [130] unlimited
 *               6) docx txt  [130] unlimited
 *               7) docx xhtml [130] unlimited
 *               8) docx xml  [130] unlimited
 *               9) dotm html [130] unlimited
 *              10) dotm txt  [130] unlimited
 *              11) dotm xhtml [130] unlimited
 *              12) dotm xml  [130] unlimited
 *              13) dotx html [130] unlimited
 *              14) dotx txt  [130] unlimited
 *              15) dotx xhtml [130] unlimited
 *              16) dotx xml  [130] unlimited
 *              17) potm html [130] unlimited
 *              18) potm txt  [130] unlimited
 *              19) potm xhtml [130] unlimited
 *              20) potm xml  [130] unlimited
 *              21) potx html [130] unlimited
 *              22) potx txt  [130] unlimited
 *              23) potx xhtml [130] unlimited
 *              24) potx xml  [130] unlimited
 *              25) ppam html [130] unlimited
 *              26) ppam txt  [130] unlimited
 *              27) ppam xhtml [130] unlimited
 *              28) ppam xml  [130] unlimited
 *              29) ppsm html [130] unlimited
 *              30) ppsm txt  [130] unlimited
 *              31) ppsm xhtml [130] unlimited
 *              32) ppsm xml  [130] unlimited
 *              33) ppsx html [130] unlimited
 *              34) ppsx txt  [130] unlimited
 *              35) ppsx xhtml [130] unlimited
 *              36) ppsx xml  [130] unlimited
 *              37) pptm html [130] unlimited
 *              38) pptm txt  [130] unlimited
 *              39) pptm xhtml [130] unlimited
 *              40) pptm xml  [130] unlimited
 *              41) pptx html [130] unlimited
 *              42) pptx txt  [130] unlimited
 *              43) pptx xhtml [130] unlimited
 *              44) pptx xml  [130] unlimited
 *              45) sldm html [130] unlimited
 *              46) sldm txt  [130] unlimited
 *              47) sldm xhtml [130] unlimited
 *              48) sldm xml  [130] unlimited
 *              49) sldx html [130] unlimited
 *              50) sldx txt  [130] unlimited
 *              51) sldx xhtml [130] unlimited
 *              52) sldx xml  [130] unlimited
 *              53) xlam html [130] unlimited
 *              54) xlam txt  [130] unlimited
 *              55) xlam xhtml [130] unlimited
 *              56) xlam xml  [130] unlimited
 *              57) xlsb html [130] unlimited
 *              58) xlsb txt  [130] unlimited
 *              59) xlsb xhtml [130] unlimited
 *              60) xlsb xml  [130] unlimited
 *              61) xlsm html [130] unlimited
 *              62) xlsm txt  [130] unlimited
 *              63) xlsm xhtml [130] unlimited
 *              64) xlsm xml  [130] unlimited
 *              65) xlsx html [130] unlimited
 *              66) xlsx txt  [130] unlimited
 *              67) xlsx xhtml [130] unlimited
 *              68) xlsx xml  [130] unlimited
 *              69) xltm html [130] unlimited
 *              70) xltm txt  [130] unlimited
 *              71) xltm xhtml [130] unlimited
 *              72) xltm xml  [130] unlimited
 *              73) xltx html [130] unlimited
 *              74) xltx txt  [130] unlimited
 *              75) xltx xhtml [130] unlimited
 *              76) xltx xml  [130] unlimited
 *           TikaAuto 0 ms
 *               1) cdf  html [120] unlimited
 *               2) cdf  txt  [120] unlimited
 *               3) cdf  xhtml [120] unlimited
 *               4) cdf  xml  [120] unlimited
 *               5) cpio html [120] unlimited
 *               6) cpio txt  [120] unlimited
 *               7) cpio xhtml [120] unlimited
 *               8) cpio xml  [120] unlimited
 *               9) doc  html [120] unlimited
 *              10) doc  txt  [120] unlimited
 *              11) doc  xhtml [120] unlimited
 *              12) doc  xml  [120] unlimited
 *              13) docm html [120] unlimited
 *              14) docm txt  [120] unlimited
 *              15) docm xhtml [120] unlimited
 *              16) docm xml  [120] unlimited
 *              17) docx html [120] unlimited
 *              18) docx txt  [120] unlimited
 *              19) docx xhtml [120] unlimited
 *              20) docx xml  [120] unlimited
 *              21) dotm html [120] unlimited
 *              22) dotm txt  [120] unlimited
 *              23) dotm xhtml [120] unlimited
 *              24) dotm xml  [120] unlimited
 *              25) dotx html [120] unlimited
 *              26) dotx txt  [120] unlimited
 *              27) dotx xhtml [120] unlimited
 *              28) dotx xml  [120] unlimited
 *              29) gzip html [120] unlimited
 *              30) gzip txt  [120] unlimited
 *              31) gzip xhtml [120] unlimited
 *              32) gzip xml  [120] unlimited
 *              33) hdf  html [120] unlimited
 *              34) hdf  txt  [120] unlimited
 *              35) hdf  xhtml [120] unlimited
 *              36) hdf  xml  [120] unlimited
 *              37) html html [120] unlimited
 *              38) html txt  [120] unlimited
 *              39) html xhtml [120] unlimited
 *              40) html xml  [120] unlimited
 *              41) jar  html [120] unlimited
 *              42) jar  txt  [120] unlimited
 *              43) jar  xhtml [120] unlimited
 *              44) jar  xml  [120] unlimited
 *              45) java html [120] unlimited
 *              46) java txt  [120] unlimited
 *              47) java xhtml [120] unlimited
 *              48) java xml  [120] unlimited
 *              49) key  html [120] unlimited
 *              50) key  txt  [120] unlimited
 *              51) key  xhtml [120] unlimited
 *              52) key  xml  [120] unlimited
 *              53) mpp  html [120] unlimited
 *              54) mpp  txt  [120] unlimited
 *              55) mpp  xhtml [120] unlimited
 *              56) mpp  xml  [120] unlimited
 *              57) numbers html [120] unlimited
 *              58) numbers txt  [120] unlimited
 *              59) numbers xhtml [120] unlimited
 *              60) numbers xml  [120] unlimited
 *              61) odc  html [120] unlimited
 *              62) odc  txt  [120] unlimited
 *              63) odc  xhtml [120] unlimited
 *              64) odc  xml  [120] unlimited
 *              65) odi  html [120] unlimited
 *              66) odi  txt  [120] unlimited
 *              67) odi  xhtml [120] unlimited
 *              68) odi  xml  [120] unlimited
 *              69) odm  html [120] unlimited
 *              70) odm  txt  [120] unlimited
 *              71) odm  xhtml [120] unlimited
 *              72) odm  xml  [120] unlimited
 *              73) odp  html [120] unlimited
 *              74) odp  txt  [120] unlimited
 *              75) odp  xhtml [120] unlimited
 *              76) odp  xml  [120] unlimited
 *              77) ods  html [120] unlimited
 *              78) ods  txt  [120] unlimited
 *              79) ods  xhtml [120] unlimited
 *              80) ods  xml  [120] unlimited
 *              81) odt  html [120] unlimited
 *              82) odt  txt  [120] unlimited
 *              83) odt  xhtml [120] unlimited
 *              84) odt  xml  [120] unlimited
 *              85) ogx  html [120] unlimited
 *              86) ogx  txt  [120] unlimited
 *              87) ogx  xhtml [120] unlimited
 *              88) ogx  xml  [120] unlimited
 *              89) oth  html [120] unlimited
 *              90) oth  txt  [120] unlimited
 *              91) oth  xhtml [120] unlimited
 *              92) oth  xml  [120] unlimited
 *              93) otp  html [120] unlimited
 *              94) otp  txt  [120] unlimited
 *              95) otp  xhtml [120] unlimited
 *              96) otp  xml  [120] unlimited
 *              97) ots  html [120] unlimited
 *              98) ots  txt  [120] unlimited
 *              99) ots  xhtml [120] unlimited
 *             100) ots  xml  [120] unlimited
 *             101) ott  html [120] unlimited
 *             102) ott  txt  [120] unlimited
 *             103) ott  xhtml [120] unlimited
 *             104) ott  xml  [120] unlimited
 *             105) pages html [120] unlimited
 *             106) pages txt  [120] unlimited
 *             107) pages xhtml [120] unlimited
 *             108) pages xml  [120] unlimited
 *             109) pdf  html [120] unlimited
 *             110) pdf  txt  [120] 25 MB
 *             111) pdf  xhtml [120] unlimited
 *             112) pdf  xml  [120] unlimited
 *             113) potm html [120] unlimited
 *             114) potm txt  [120] unlimited
 *             115) potm xhtml [120] unlimited
 *             116) potm xml  [120] unlimited
 *             117) potx html [120] unlimited
 *             118) potx txt  [120] unlimited
 *             119) potx xhtml [120] unlimited
 *             120) potx xml  [120] unlimited
 *             121) ppam html [120] unlimited
 *             122) ppam txt  [120] unlimited
 *             123) ppam xhtml [120] unlimited
 *             124) ppam xml  [120] unlimited
 *             125) ppsm html [120] unlimited
 *             126) ppsm txt  [120] unlimited
 *             127) ppsm xhtml [120] unlimited
 *             128) ppsm xml  [120] unlimited
 *             129) ppsx html [120] unlimited
 *             130) ppsx txt  [120] unlimited
 *             131) ppsx xhtml [120] unlimited
 *             132) ppsx xml  [120] unlimited
 *             133) ppt  html [120] unlimited
 *             134) ppt  txt  [120] unlimited
 *             135) ppt  xhtml [120] unlimited
 *             136) ppt  xml  [120] unlimited
 *             137) pptm html [120] unlimited
 *             138) pptm txt  [120] unlimited
 *             139) pptm xhtml [120] unlimited
 *             140) pptm xml  [120] unlimited
 *             141) pptx html [120] unlimited
 *             142) pptx txt  [120] unlimited
 *             143) pptx xhtml [120] unlimited
 *             144) pptx xml  [120] unlimited
 *             145) rar  html [120] unlimited
 *             146) rar  txt  [120] unlimited
 *             147) rar  xhtml [120] unlimited
 *             148) rar  xml  [120] unlimited
 *             149) rss  html [120] unlimited
 *             150) rss  txt  [120] unlimited
 *             151) rss  xhtml [120] unlimited
 *             152) rss  xml  [120] unlimited
 *             153) rtf  html [120] unlimited
 *             154) rtf  txt  [120] unlimited
 *             155) rtf  xhtml [120] unlimited
 *             156) rtf  xml  [120] unlimited
 *             157) sldm html [120] unlimited
 *             158) sldm txt  [120] unlimited
 *             159) sldm xhtml [120] unlimited
 *             160) sldm xml  [120] unlimited
 *             161) sldx html [120] unlimited
 *             162) sldx txt  [120] unlimited
 *             163) sldx xhtml [120] unlimited
 *             164) sldx xml  [120] unlimited
 *             165) sxw  html [120] unlimited
 *             166) sxw  txt  [120] unlimited
 *             167) sxw  xhtml [120] unlimited
 *             168) sxw  xml  [120] unlimited
 *             169) txt  html [120] unlimited
 *             170) txt  txt  [120] unlimited
 *             171) txt  xhtml [120] unlimited
 *             172) txt  xml  [120] unlimited
 *             173) vsd  html [120] unlimited
 *             174) vsd  txt  [120] unlimited
 *             175) vsd  xhtml [120] unlimited
 *             176) vsd  xml  [120] unlimited
 *             177) xhtml html [120] unlimited
 *             178) xhtml txt  [120] unlimited
 *             179) xhtml xhtml [120] unlimited
 *             180) xhtml xml  [120] unlimited
 *             181) xlam html [120] unlimited
 *             182) xlam txt  [120] unlimited
 *             183) xlam xhtml [120] unlimited
 *             184) xlam xml  [120] unlimited
 *             185) xls  html [120] unlimited
 *             186) xls  txt  [120] unlimited
 *             187) xls  xhtml [120] unlimited
 *             188) xls  xml  [120] unlimited
 *             189) xlsb html [120] unlimited
 *             190) xlsb txt  [120] unlimited
 *             191) xlsb xhtml [120] unlimited
 *             192) xlsb xml  [120] unlimited
 *             193) xlsm html [120] unlimited
 *             194) xlsm txt  [120] unlimited
 *             195) xlsm xhtml [120] unlimited
 *             196) xlsm xml  [120] unlimited
 *             197) xlsx html [120] unlimited
 *             198) xlsx txt  [120] unlimited
 *             199) xlsx xhtml [120] unlimited
 *             200) xlsx xml  [120] unlimited
 *             201) xltm html [120] unlimited
 *             202) xltm txt  [120] unlimited
 *             203) xltm xhtml [120] unlimited
 *             204) xltm xml  [120] unlimited
 *             205) xltx html [120] unlimited
 *             206) xltx txt  [120] unlimited
 *             207) xltx xhtml [120] unlimited
 *             208) xltx xml  [120] unlimited
 *             209) xml  html [120] unlimited
 *             210) xml  txt  [120] unlimited
 *             211) xml  xhtml [120] unlimited
 *             212) xml  xml  [120] unlimited
 *             213) z    html [120] unlimited
 *             214) z    txt  [120] unlimited
 *             215) z    xhtml [120] unlimited
 *             216) z    xml  [120] unlimited
 *           TextMining 0 ms
 *               1) doc  html [130] unlimited
 *               2) doc  txt   [50] unlimited
 *               3) doc  xhtml [130] unlimited
 *               4) doc  xml  [130] unlimited
 * </pre>
 */
public class Tika
{
    public static final String ARCHIVE = "Archive";
    public static final String OUTLOOK_MSG = "OutlookMsg";
    public static final String PDF_BOX = "PdfBox";
    public static final String POI_OFFICE = "Office";
    public static final String POI = "Poi";
    public static final String POI_OO_XML = "OOXML";
    public static final String TIKA_AUTO = "TikaAuto";
    public static final String TEXT_MINING = "TextMining";

    public static final List<String> TRANSFORM_NAMES = Arrays.asList(
            ARCHIVE, OUTLOOK_MSG, PDF_BOX, POI_OFFICE, POI, POI_OO_XML, TIKA_AUTO, TEXT_MINING);

    public static final String TARGET_MIMETYPE = "--targetMimetype=";
    public static final String TARGET_ENCODING = "--targetEncoding=";
    public static final String INCLUDE_CONTENTS = "--includeContents";
    public static final String NOT_EXTRACT_BOOKMARKS_TEXT = "--notExtractBookmarksText";

    public static final String CSV     = "csv";
    public static final String DOC     = "doc";
    public static final String DOCX    = "docx";
    public static final String HTML    = "html";
    public static final String MSG     = "msg";
    public static final String PDF     = "pdf";
    public static final String PPTX    = "pptx";
    public static final String TXT     = "txt";
    public static final String XHTML   = "xhtml";
    public static final String XSLX    = "xslx";
    public static final String XML     = "xml";
    public static final String ZIP     = "zip";

    private Parser packageParser = new PackageParser();
    private Parser pdfParser = new PDFParser();
    private Parser officeParser = new OfficeParser();
    private Parser autoDetectParser;
    private Parser ooXmlParser = new OOXMLParser();
    private Parser tikaOfficeDetectParser = new TikaOfficeDetectParser();
    private  PDFParserConfig pdfParserConfig = new PDFParserConfig();

    private DocumentSelector pdfBoxEmbededDocumentSelector = new DocumentSelector()
    {
        private List<String> disabledMediaTypes = Arrays.asList(new String[] {MIMETYPE_IMAGE_JPEG, MIMETYPE_IMAGE_TIFF, MIMETYPE_IMAGE_PNG});

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
        ClassLoader classLoader = getClass().getClassLoader();
        URL tikaConfigXml = classLoader.getResource("tika-config.xml");
        TikaConfig tikaConfig = new TikaConfig(tikaConfigXml);
        autoDetectParser = new AutoDetectParser(tikaConfig);
    }

    // Method included for developer testing
    public static void main(String[] args)
    {
        long start = System.currentTimeMillis();
        try
        {
            new Tika().transform(args);
        }
        catch (IllegalArgumentException e)
        {
            System.err.println("ERROR "+e.getMessage());
            System.exit(-1);
        }
        catch (IllegalStateException | TikaException | IOException | SAXException e)
        {
            System.err.println("ERROR "+e.getMessage());
            e.printStackTrace();
            System.exit(-2);
        }
        System.out.println("Finished in "+(System.currentTimeMillis()-start)+"ms");
    }

    // Extracts parameters form args
    public void transform(String[] args)
    {
        String transform = null;
        String targetMimetype = null;
        String targetEncoding = null;
        String sourceFilename = null;
        String targetFilename = null;
        Boolean includeContents = null;
        Boolean notExtractBookmarksText = null;

        for (String arg: args)
        {
            if (arg.startsWith("--"))
            {
                if (INCLUDE_CONTENTS.startsWith(arg))
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
                    throw new IllegalArgumentException("Unexpected argument "+arg);
                }
            }
            else
            {
                if (transform == null)
                {
                    transform = arg;
                }
                else if (sourceFilename == null)
                {
                    sourceFilename = arg;
                }
                else if (targetFilename == null)
                {
                    targetFilename = arg;
                }
                else
                {
                    throw new IllegalArgumentException("Unexpected argument "+arg);
                }
            }
        }
        if (targetFilename == null)
        {
            throw new IllegalArgumentException("Missing arguments");
        }
        includeContents = includeContents == null ? false : includeContents;
        notExtractBookmarksText = notExtractBookmarksText == null ? false : notExtractBookmarksText; 

        transform(transform, includeContents, notExtractBookmarksText, sourceFilename, targetFilename, targetMimetype, targetEncoding);
    }

    private String getValue(String arg, boolean valueExpected, Object value, String optionName)
    {
        if (value != null)
        {
            throw new IllegalArgumentException("Duplicate "+optionName);
        }
        String stringValue = arg.substring(optionName.length()).trim();
        if (!valueExpected && stringValue.length() > 0)
        {
            throw new IllegalArgumentException("Unexpected value with "+optionName);
        }
        if (valueExpected && stringValue.length() == 0)
        {
            throw new IllegalArgumentException("Expected value with "+optionName);
        }
        return stringValue;
    }

    // Adds transform specific values such as parser and documentSelector.
    private void transform(String transform, Boolean includeContents,
                           Boolean notExtractBookmarksText,
                           String sourceFilename,
                           String targetFilename, String targetMimetype, String targetEncoding)
    {
        Parser parser = null;
        DocumentSelector documentSelector = null;

        switch(transform)
        {
            case ARCHIVE:
                parser = packageParser;
                break;
            case OUTLOOK_MSG:
            case POI_OFFICE:
            case TEXT_MINING:
                parser = officeParser;
                break;
            case PDF_BOX:
                parser = pdfParser;
                documentSelector = pdfBoxEmbededDocumentSelector;
                break;
            case POI:
                parser = tikaOfficeDetectParser;
                break;
            case POI_OO_XML:
                parser = ooXmlParser;
                break;
            case TIKA_AUTO:
                parser = autoDetectParser;
                break;
        }

        transform(parser, documentSelector, includeContents, notExtractBookmarksText, sourceFilename, targetFilename, targetMimetype, targetEncoding);
    }


    private void transform(Parser parser, DocumentSelector documentSelector, Boolean includeContents,
                           Boolean notExtractBookmarksText,
                           String sourceFilename,
                           String targetFilename, String targetMimetype, String targetEncoding)
    {
        InputStream is = null;
        OutputStream os = null;
        Writer ow = null;

        try
        {
            is = new BufferedInputStream(new FileInputStream(sourceFilename));
            os = new FileOutputStream(targetFilename);
            ow = new BufferedWriter(new OutputStreamWriter(os, targetEncoding));
            Metadata metadata = new Metadata();
            ParseContext context = buildParseContext(documentSelector, includeContents, notExtractBookmarksText);
            ContentHandler handler = getContentHandler(targetMimetype, ow);

            parser.parse(is, handler, metadata, context);
        }
        catch (SAXException | TikaException | IOException e)
        {
            throw new IllegalStateException(e.getMessage(), e);
        }
        finally
        {
            if (is != null)
            {
                try { is.close(); } catch (Throwable e) {}
            }
            if (os != null)
            {
                try { os.close(); } catch (Throwable e) {}
            }
            if (ow != null)
            {
                try { ow.close(); } catch (Throwable e) {}
            }
        }
    }

    protected ContentHandler getContentHandler(String targetMimetype, Writer output)
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
                SAXTransformerFactory factory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
                TransformerHandler transformerHandler = null;
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
    protected static class CsvContentHandler extends BodyContentHandler {
        private static final char[] comma = new char[]{ ',' };
        private static final Pattern all_nums = Pattern.compile("[\\d\\.\\-\\+]+");

        private boolean inCell = false;
        private boolean needsComma = false;

        protected CsvContentHandler(Writer output) {
            super(output);
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length)
                throws SAXException {
            if(length == 1 && ch[0] == '\t') {
                // Ignore tabs, as they mess up the CSV output
            } else {
                super.ignorableWhitespace(ch, start, length);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            if(inCell) {
                StringBuffer t = new StringBuffer(new String(ch,start,length));

                // Quote if not all numbers
                if(all_nums.matcher(t).matches())
                {
                    super.characters(ch, start, length);
                }
                else
                {
                    for(int i=t.length()-1; i>=0; i--) {
                        if(t.charAt(i) == '\"') {
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
            } else {
                super.characters(ch, start, length);
            }
        }

        @Override
        public void startElement(String uri, String localName, String name,
                                 Attributes atts) throws SAXException {
            if(localName.equals("td")) {
                inCell = true;
                if(needsComma) {
                    super.characters(comma, 0, 1);
                    needsComma = true;
                }
            } else {
                super.startElement(uri, localName, name, atts);
            }
        }

        @Override
        public void endElement(String uri, String localName, String name)
                throws SAXException {
            if(localName.equals("td")) {
                needsComma = true;
                inCell = false;
            } else {
                if(localName.equals("tr")) {
                    needsComma = false;
                }
                super.endElement(uri, localName, name);
            }
        }
    }

    protected ParseContext buildParseContext(DocumentSelector documentSelector, Boolean includeContents, Boolean notExtractBookmarksText)
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
