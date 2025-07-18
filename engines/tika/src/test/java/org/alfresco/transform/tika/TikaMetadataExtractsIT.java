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
package org.alfresco.transform.tika;

import static org.alfresco.transform.base.clients.FileInfo.testFile;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_APP_DWG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_AUDIO_MP4;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_EXCEL;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_BMP;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_GIF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_ARW;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_CR2;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_NEF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_RAF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_RAW_RW2;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_TIFF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IWORK_KEYNOTE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IWORK_NUMBERS;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IWORK_PAGES;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_MP3;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_FORMULA;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_GRAPHICS;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_GRAPHICS_TEMPLATE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_PRESENTATION;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_PRESENTATION_TEMPLATE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_SPREADSHEET;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_SPREADSHEET_TEMPLATE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_TEXT;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_TEXT_TEMPLATE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENOFFICE1_WRITER;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_PRESENTATION;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_SPREADSHEET;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_WORDPROCESSING;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OUTLOOK_MSG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PPT;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_VIDEO_3GP;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_VIDEO_3GP2;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_VIDEO_FLV;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_VIDEO_MP4;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_VIDEO_QUICKTIME;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_VISIO;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_VORBIS;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_WORD;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_XML;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_ZIP;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.alfresco.transform.base.clients.FileInfo;
import org.alfresco.transform.base.metadata.AbstractMetadataExtractsIT;

/**
 * Metadata integration tests in the Tika T-Engine.
 *
 * @author adavis
 * @author dedwards
 */
public class TikaMetadataExtractsIT extends AbstractMetadataExtractsIT
{

    @ParameterizedTest
    @MethodSource("engineTransformations")
    @Override
    public void testTransformation(FileInfo fileInfo)
    {
        super.testTransformation(fileInfo);
    }

    private static Stream<FileInfo> engineTransformations()
    {
        // The following files are the ones tested in the content repository.
        // There are many more mimetypes supported by these extractors.

        // Where a line has been commented out, the repository code tries to test it but stops because there is
        // either no quick file or the target extension has not been registered.

        return Stream.of(
                // IPTCMetadataExtractor
                testFile(MIMETYPE_IMAGE_JPEG, "jpg", "quick.jpg"),
                testFile(MIMETYPE_IMAGE_JPEG, "jpg", "quickIPTC-EXT.jpg"),
                testFile(MIMETYPE_IMAGE_JPEG, "jpg", "quickIPTC-multi-creator.jpg"),
                testFile(MIMETYPE_IMAGE_JPEG, "jpg", "testJPEG_IPTC_EXT.jpg"),
                testFile(MIMETYPE_IMAGE_GIF, "gif", "quickIPTC.gif"),
                testFile(MIMETYPE_IMAGE_PNG, "png", "quickIPTC.png"),
                testFile(MIMETYPE_IMAGE_RAW_ARW, "arw", "20140614_163822_Photogrpahy_Class.ARW"),
                testFile(MIMETYPE_IMAGE_RAW_CR2, "cr2", "20141227_134519_Palace.CR2"),
                testFile(MIMETYPE_IMAGE_RAW_RW2, "rw2", "20140629_145035_Flower.RW2"),
                testFile(MIMETYPE_IMAGE_RAW_NEF, "nef", "20150408_074941_Bush.NEF"),
                testFile(MIMETYPE_IMAGE_RAW_RAF, "raf", "20160502_190928_London_Underground.RAF"),

                // DWGMetadataExtractor
                testFile(MIMETYPE_APP_DWG, "dwg", "quick2010CustomProps.dwg"),

                // MailMetadataExtractor
                testFile(MIMETYPE_OUTLOOK_MSG, "msg", "quick.msg"),

                // MP3MetadataExtractor
                testFile(MIMETYPE_MP3, "mp3", "quick.mp3"),

                // OfficeMetadataExtractor
                testFile(MIMETYPE_WORD, "doc", "quick.doc"),
                // testFile("application/x-tika-msoffice-embedded; format=ole10_native", "", ""),
                testFile(MIMETYPE_VISIO, "vsd", "quick.vsd"),
                // testFile("application/vnd.ms-project", "mpp", ""),
                // testFile("application/x-tika-msworks-spreadsheet", "", ""),
                // testFile("application/x-mspublisher", "", ""),
                testFile(MIMETYPE_PPT, "ppt", "quick.ppt"),
                // testFile("application/x-tika-msoffice", "", ""),
                // testFile(MIMETYPE_VISIO_2013, "vsdx", ""),
                // testFile("application/sldworks", "", ""),
                // testFile(MIMETYPE_ENCRYPTED_OFFICE, "", ""),
                testFile(MIMETYPE_EXCEL, "xls", "quick.xls"),

                // OpenDocumentMetadataExtractor
                // testFile("application/x-vnd.oasis.opendocument.presentation", "", ""),
                // testFile(MIMETYPE_OPENDOCUMENT_CHART, "odc", ""),
                // testFile(MIMETYPE_OPENDOCUMENT_IMAGE_TEMPLATE, "", ""),
                // testFile("application/x-vnd.oasis.opendocument.text-web", "", ""),
                // testFile("application/x-vnd.oasis.opendocument.image", "", ""),
                testFile(MIMETYPE_OPENDOCUMENT_GRAPHICS_TEMPLATE, "otg", "quick.otg"),
                // testFile(MIMETYPE_OPENDOCUMENT_TEXT_WEB, "oth", ""),
                // testFile("application/x-vnd.oasis.opendocument.spreadsheet-template", "", ""),
                testFile(MIMETYPE_OPENDOCUMENT_SPREADSHEET_TEMPLATE, "ots", "quick.ots"),
                testFile(MIMETYPE_OPENOFFICE1_WRITER, "sxw", "quick.sxw"),
                // testFile("application/x-vnd.oasis.opendocument.graphics-template", "", ""),
                testFile(MIMETYPE_OPENDOCUMENT_GRAPHICS, "odg", "quick.odg"),
                testFile(MIMETYPE_OPENDOCUMENT_SPREADSHEET, "ods", "quick.ods"),
                // testFile("application/x-vnd.oasis.opendocument.chart", "", ""),
                // testFile("application/x-vnd.oasis.opendocument.spreadsheet", "", ""),
                // testFile(MIMETYPE_OPENDOCUMENT_IMAGE, "odi", ""),
                // testFile("application/x-vnd.oasis.opendocument.text", "", ""),
                // testFile("application/x-vnd.oasis.opendocument.text-template", "", ""),
                // testFile("application/vnd.oasis.opendocument.formula-template", "", ""),
                // testFile("application/x-vnd.oasis.opendocument.formula", "", ""),
                // testFile("application/vnd.oasis.opendocument.image-template", "", ""),
                // testFile("application/x-vnd.oasis.opendocument.image-template", "", ""),
                // testFile("application/x-vnd.oasis.opendocument.presentation-template", "", ""),
                testFile(MIMETYPE_OPENDOCUMENT_PRESENTATION_TEMPLATE, "otp", "quick.otp"),
                testFile(MIMETYPE_OPENDOCUMENT_TEXT, "odt", "quick.odt"),
                // testFile(MIMETYPE_OPENDOCUMENT_FORMULA_TEMPLATE, "", ""),
                testFile(MIMETYPE_OPENDOCUMENT_TEXT_TEMPLATE, "ott", "quick.ott"),
                // testFile("application/vnd.oasis.opendocument.chart-template", "", ""),
                // testFile("application/x-vnd.oasis.opendocument.chart-template", "", ""),
                // testFile("application/x-vnd.oasis.opendocument.formula-template", "", ""),
                // testFile(MIMETYPE_OPENDOCUMENT_DATABASE, "odb", ""),
                // testFile("application/x-vnd.oasis.opendocument.text-master", "", ""),
                testFile(MIMETYPE_OPENDOCUMENT_PRESENTATION, "odp", "quick.odp"),
                // testFile(MIMETYPE_OPENDOCUMENT_CHART_TEMPLATE, "", ""),
                // testFile("application/x-vnd.oasis.opendocument.graphics", "", ""),
                testFile(MIMETYPE_OPENDOCUMENT_FORMULA, "odf", "quick.odf"),
                // testFile(MIMETYPE_OPENDOCUMENT_TEXT_MASTER, "odm", ""),

                // PdfBoxMetadataExtractor
                testFile(MIMETYPE_PDF, "pdf", "quick.pdf"),
                // testFile(MIMETYPE_APPLICATION_ILLUSTRATOR, "ai", ""),

                // PoiMetadataExtractor
                // testFile(MIMETYPE_OPENXML_PRESENTATION_TEMPLATE_MACRO, "potm", ""),
                // testFile(MIMETYPE_OPENXML_SPREADSHEET_ADDIN_MACRO, "xlam", ""),
                // testFile(MIMETYPE_OPENXML_WORD_TEMPLATE, "dotx", ""),
                // testFile(MIMETYPE_OPENXML_SPREADSHEET_BINARY_MACRO, "xlsb", ""),
                testFile(MIMETYPE_OPENXML_WORDPROCESSING, "docx", "quick.docx"),
                // testFile(MIMETYPE_OPENXML_PRESENTATION_SLIDE_MACRO, "sldm", ""),
                // testFile("application/vnd.ms-visio.drawing", "", ""),
                // testFile(MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW_MACRO, "ppsm", ""),
                // testFile(MIMETYPE_OPENXML_PRESENTATION_MACRO, "pptm", ""),
                // testFile(MIMETYPE_OPENXML_PRESENTATION_SLIDE, "sldx", ""),
                // testFile(MIMETYPE_OPENXML_SPREADSHEET_MACRO, "xlsm", ""),
                // testFile(MIMETYPE_OPENXML_WORD_TEMPLATE_MACRO, "dotm", ""),
                // testFile(MIMETYPE_OPENXML_WORDPROCESSING_MACRO, "docm", ""),
                // testFile(MIMETYPE_OPENXML_PRESENTATION_ADDIN, "ppam", ""),
                // testFile(MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE, "xltx", ""),
                // testFile("application/vnd.ms-xpsdocument", "", ""),
                // testFile("application/vnd.ms-visio.drawing.macroenabled.12", "", ""),
                // testFile("application/vnd.ms-visio.template.macroenabled.12", "", ""),
                // testFile("model/vnd.dwfx+xps", "", ""),
                // testFile(MIMETYPE_OPENXML_PRESENTATION_TEMPLATE, "potx", ""),
                testFile(MIMETYPE_OPENXML_PRESENTATION, "pptx", "quick.pptx"),
                testFile(MIMETYPE_OPENXML_SPREADSHEET, "xlsx", "quick.xlsx"),
                // testFile("application/vnd.ms-visio.stencil", "", ""),
                // testFile("application/vnd.ms-visio.template", "", ""),
                // testFile(MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW, "ppsx", ""),
                // testFile("application/vnd.ms-visio.stencil.macroenabled.12", "", ""),
                // testFile(MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE_MACRO, "xltm", ""),

                // TikaAudioMetadataExtractor
                testFile("video/x-m4v", "m4v", "quick.m4v"),
                // testFile("audio/x-oggflac", "", ""),
                // testFile("application/mp4", "", ""),
                testFile(MIMETYPE_VORBIS, "ogg", "quick.ogg"),
                testFile(MIMETYPE_VIDEO_3GP, "3gp", "quick.3gp"),
                // testFile(MIMETYPE_FLAC, "flac", ""),
                testFile(MIMETYPE_VIDEO_3GP2, "3g2", "quick.3g2"),
                testFile(MIMETYPE_VIDEO_QUICKTIME, "mov", "quick.mov"),
                testFile(MIMETYPE_AUDIO_MP4, "m4a", "quick.m4a"),
                testFile(MIMETYPE_VIDEO_MP4, "mp4", "quick.mp4"),

                // TikaAutoMetadataExtractor

                // The following <source>_metadata.json files contain null values against author and title.
                // This is not new and will be the case in the content repository, but was not tested.
                //
                // The expected ones are: txt, xml, zip, tar
                //
                // The unexpected ones are: quick.key, quick.numbers and quick.pages.
                //
                // quick.bmp, quick.gif, quick.png, quick.3g2, quick.3gp, quick.flv, quick.m4v, quick.mov & quick.mp4
                // contain one or more values, but also include nulls. Again this may be correct, a bug or just the
                // example quick file rather than a problem with the extractor.

                // testFile("application/vnd.ms-htmlhelp", "", ""),
                // testFile(MIMETYPE_ATOM, "", ""),
                // testFile("audio/midi", "", ""),
                // testFile("application/aaigrid", "", ""),
                // testFile("application/x-bag", "", ""),
                testFile(MIMETYPE_IWORK_KEYNOTE, "key", "quick.key"),
                // testFile("application/x-quattro-pro; version=9", "", ""),
                // testFile("application/x-ibooks+zip", "", ""),
                // testFile("audio/wave", "", ""),
                // testFile("application/x-midi", "", ""),
                testFile(MIMETYPE_XML, "xml", "quick.xml"),
                // testFile(MIMETYPE_RSS, "rss", ""),
                // testFile("application/x-netcdf", "cdf", ""),
                // testFile("video/x-daala", "", ""),
                // testFile("application/matlab-mat", "", ""),
                // testFile("audio/aiff", "", ""),
                // testFile("application/jaxa-pal-sar", "", ""),
                // testFile("image/x-pcraster", "", ""),
                // testFile("image/arg", "", ""),
                // testFile("application/x-kro", "", ""),
                // testFile("image/x-hdf5-image", "", ""),
                // testFile("audio/speex", "", ""),
                // testFile("image/big-gif", "", ""),
                // testFile("application/zlib", "", ""),
                // testFile("application/x-cosar", "", ""),
                // testFile("application/x-ntv2", "", ""),
                // testFile("application/x-archive", "", ""),
                // testFile("application/java-archive", "jar", ""),
                // testFile("application/x-vnd.sun.xml.writer", "", ""),
                // testFile("application/x-gmt", "", ""),
                // testFile("application/x-xml", "", ""),
                // testFile("application/gzip-compressed", "", ""),
                // testFile("image/ida", "", ""),
                // testFile("text/x-groovy", "", ""),
                // testFile("image/x-emf", "", ""),
                // testFile("application/x-rar", "", ""),
                // testFile("image/sar-ceos", "", ""),
                // testFile("application/acad", "", ""),
                testFile(MIMETYPE_ZIP, "zip", "quick.zip"),
                // testFile(MIMETYPE_IMAGE_PSD, "psd", ""),
                // testFile("application/x-sharedlib", "", ""),
                // testFile("audio/x-m4a", "", ""),
                // testFile("image/webp", "", ""),
                // testFile("application/vnd.wap.xhtml+xml", "", ""),
                // testFile("audio/x-aiff", "aiff", ""),
                // testFile("application/vnd.ms-spreadsheetml", "", ""),
                // testFile("image/x-airsar", "", ""),
                // testFile("application/x-pcidsk", "", ""),
                // testFile("application/x-java-pack200", "", ""),
                // testFile("image/x-fujibas", "", ""),
                // testFile("application/x-zmap", "", ""),
                // testFile("image/x-bmp", "", ""),
                // testFile("image/bpg", "", ""),
                // testFile(MIMETYPE_RTF, "rtf", ""),
                // testFile("application/x-xz", "", ""),
                // testFile("application/x-speex", "", ""),
                // testFile("audio/ogg; codecs=speex", "", ""),
                // testFile("application/x-l1b", "", ""),
                // testFile("application/x-gsbg", "", ""),
                // testFile("application/x-sdat", "", ""),
                // testFile("application/vnd.ms-visio", "", ""),
                // testFile("application/x-coredump", "", ""),
                // testFile("application/x-msaccess", "", ""),
                // testFile("application/x-dods", "", ""),
                testFile(MIMETYPE_IMAGE_PNG, "png", "quick.png"),
                // testFile("application/vnd.ms-outlook-pst", "", ""),
                // testFile("image/bsb", "", ""),
                // testFile("application/x-cpio", "cpio", ""),
                // testFile("audio/ogg", "oga", ""),
                testFile("application/x-tar", "tar", "quick.tar"),
                // testFile("application/x-dbf", "", ""),
                // testFile("video/x-ogm", "", ""),
                // testFile("application/x-los-las", "", ""),
                // testFile("application/autocad_dwg", "", ""),
                // testFile("application/vnd.ms-excel.workspace.3", "", ""),
                // testFile("application/vnd.ms-excel.workspace.4", "", ""),
                // testFile("image/x-bpg", "", ""),
                // testFile("gzip/document", "", ""),
                // testFile("text/x-java", "", ""),
                // testFile("application/x-brotli", "", ""),
                // testFile("application/elas", "", ""),
                // testFile("image/x-jb2", "", ""),
                // testFile("application/x-cappi", "", ""),
                // testFile("application/epub+zip", "", ""),
                // testFile("application/x-ace2", "", ""),
                // testFile("application/x-sas-data", "", ""),
                // testFile("application/x-hdf", "hdf", ""),
                // testFile("image/x-mff", "", ""),
                // testFile("image/x-srp", "", ""),
                testFile(MIMETYPE_IMAGE_BMP, "bmp", "quick.bmp"),
                // testFile("video/x-ogguvs", "", ""),
                // testFile("drawing/dwg", "", ""),
                // testFile("application/x-doq2", "", ""),
                // testFile("application/x-acad", "", ""),
                // testFile("application/x-kml", "", ""),
                // testFile("application/x-autocad", "", ""),
                // testFile("image/x-mff2", "", ""),
                // testFile("application/x-snodas", "", ""),
                // testFile("application/terragen", "", ""),
                // testFile("application/x-wcs", "", ""),
                // testFile("text/x-c++src", "", ""),
                // testFile("application/timestamped-data", "", ""),
                testFile(MIMETYPE_IMAGE_TIFF, "tiff", "quick.tiff"),
                // testFile("application/msexcel", "", ""),
                // testFile("application/x-asp", "", ""),
                // testFile("application/x-rar-compressed", "rar", ""),
                // testFile("application/x-envi-hdr", "", ""),
                // testFile("text/iso19139+xml", "", ""),
                // testFile("application/vnd.ms-tnef", "", ""),
                // testFile("application/x-ecrg-toc", "", ""),
                // testFile("application/aig", "", ""),
                // testFile("audio/x-wav", "wav", ""),
                // testFile("image/emf", "", ""),
                // testFile("application/x-bzip", "", ""),
                // testFile("application/jdem", "", ""),
                // testFile("application/x-webp", "", ""),
                // testFile("application/x-arj", "", ""),
                // testFile("application/x-lzma", "", ""),
                // testFile("application/x-java-vm", "", ""),
                // testFile("image/envisat", "", ""),
                // testFile("application/x-doq1", "", ""),
                // testFile("audio/vnd.wave", "", ""),
                // testFile("application/x-ppi", "", ""),
                // testFile("image/ilwis", "", ""),
                // testFile("application/x-gunzip", "", ""),
                // testFile("image/x-icon", "", ""),
                // testFile("application/ogg", "ogx", ""),
                // testFile(MIMETYPE_IMAGE_SVG, "svg", ""),
                // testFile("application/x-ms-owner", "", ""),
                // testFile("application/x-grib", "", ""),
                // testFile("application/ms-tnef", "", ""),
                // testFile("image/fits", "", ""),
                // testFile("audio/x-mpeg", "", ""),
                // testFile("application/x-bzip2", "", ""),
                // testFile("text/tsv", "", ""),
                // testFile("application/x-fictionbook+xml", "", ""),
                // testFile("application/x-p-aux", "", ""),
                // testFile("application/x-font-ttf", "", ""),
                // testFile("image/x-xcf", "", ""),
                // testFile("image/x-ms-bmp", "", ""),
                // testFile("image/wmf", "", ""),
                // testFile("image/eir", "", ""),
                // testFile("application/x-matlab-data", "", ""),
                // testFile("application/deflate64", "", ""),
                // testFile("audio/wav", "", ""),
                // testFile("application/x-rs2", "", ""),
                // testFile("application/vnd.ms-word", "", ""),
                // testFile("application/x-tsx", "", ""),
                // testFile("application/x-lcp", "", ""),
                // testFile("application/x-mbtiles", "", ""),
                // testFile("audio/x-oggpcm", "", ""),
                // testFile("application/x-epsilon", "", ""),
                // testFile("application/x-msgn", "", ""),
                // testFile(MIMETYPE_TEXT_CSV, "csv", ""),
                // testFile("image/x-dimap", "", ""),
                // testFile("image/vnd.microsoft.icon", "", ""),
                // testFile("application/x-envi", "", ""),
                // testFile("application/x-dwg", "", ""),
                testFile(MIMETYPE_IWORK_NUMBERS, "numbers", "quick.numbers"),
                // testFile("application/vnd.ms-word2006ml", "", ""),
                // testFile("application/x-bt", "", ""),
                // testFile("application/x-font-adobe-metric", "", ""),
                // testFile("application/x-rst", "", ""),
                // testFile("application/vrt", "", ""),
                // testFile("application/x-ctg", "", ""),
                // testFile("application/x-e00-grid", "", ""),
                // testFile("audio/x-ogg-flac", "", ""),
                // testFile("application/x-compress", "z", ""),
                // testFile("image/x-psd", "", ""),
                // testFile("text/rss", "", ""),
                // testFile("application/sdts-raster", "", ""),
                // testFile("application/oxps", "", ""),
                // testFile("application/leveller", "", ""),
                // testFile("application/x-ingr", "", ""),
                // testFile("image/sgi", "", ""),
                // testFile("application/x-pnm", "", ""),
                // testFile("image/raster", "", ""),
                // testFile("audio/x-ogg-pcm", "", ""),
                // testFile("audio/ogg; codecs=opus", "", ""),
                // testFile("application/fits", "", ""),
                // testFile("application/x-r", "", ""),
                testFile(MIMETYPE_IMAGE_GIF, "gif", "quick.gif"),
                // testFile("application/java-vm", "", ""),
                // testFile("application/mspowerpoint", "", ""),
                // testFile("application/x-http", "", ""),
                // testFile("application/x-rmf", "", ""),
                // testFile("application/x-ogg", "", ""),
                // testFile("video/ogg", "ogv", "quick.ogv"),
                // testFile(MIMETYPE_APPLEFILE, "", ""),
                // testFile("text/rtf", "", ""),
                // testFile("image/adrg", "", ""),
                // testFile("video/x-ogg-rgb", "", ""),
                // testFile("application/x-ngs-geoid", "", ""),
                // testFile("application/x-map", "", ""),
                // testFile("image/ceos", "", ""),
                // testFile("application/xpm", "", ""),
                // testFile("application/x-ers", "", ""),
                // testFile("video/x-ogg-yuv", "", ""),
                // testFile("application/x-isis2", "", ""),
                // testFile("application/x-nwt-grd", "", ""),
                // testFile("application/x-isis3", "", ""),
                // testFile("application/x-nwt-grc", "", ""),
                // testFile("video/daala", "", ""),
                // testFile("application/x-blx", "", ""),
                // testFile("application/x-tnef", "", ""),
                // testFile("video/x-dirac", "", ""),
                // testFile("application/x-ndf", "", ""),
                // testFile("image/vnd.wap.wbmp", "", ""),
                // testFile("video/theora", "", ""),
                // testFile("application/kate", "", ""),
                // testFile("application/pkcs7-mime", "", ""),
                // testFile("image/fit", "", ""),
                // testFile("application/x-ctable2", "", ""),
                // testFile("application/x-executable", "", ""),
                // testFile("application/x-isatab", "", ""),
                // testFile("application/grass-ascii-grid", "", ""),
                testFile(MIMETYPE_TEXT_PLAIN, "txt", "quick.txt"),
                // testFile("application/gzipped", "", ""),
                // testFile("application/x-gxf", "", ""),
                // testFile("application/x-cpg", "", ""),
                // testFile("application/x-lan", "", ""),
                // testFile("application/x-xyz", "", ""),
                testFile(MIMETYPE_IWORK_PAGES, "pages", "quick.pages"),
                // testFile("image/x-jbig2", "", ""),
                // testFile("image/nitf", "", ""),
                // testFile("application/mbox", "", ""),
                // testFile("application/chm", "", ""),
                // testFile("application/x-fast", "", ""),
                // testFile("application/x-gsc", "", ""),
                // testFile("application/x-deflate", "", ""),
                // testFile("application/x-grib2", "", ""),
                // testFile("image/x-ozi", "", ""),
                // testFile("application/x-pds", "", ""),
                // testFile("application/vnd.apple.iwork", "", ""),
                // testFile("application/x-usgs-dem", "", ""),
                // testFile("application/vnd.ms-excel.sheet.2", "", ""),
                // testFile("application/vnd.ms-excel.sheet.3", "", ""),
                // testFile("application/dif+xml", "", ""),
                // testFile("application/vnd.ms-excel.sheet.4", "", ""),
                // testFile("application/x-java", "", ""),
                // testFile("image/geotiff", "", ""),
                // testFile("application/x-gsag", "", ""),
                // testFile("application/x-snappy", "", ""),
                // testFile("video/x-theora", "", ""),
                // testFile("image/ntf", "", ""),
                // testFile("application/x-pdf", "", ""),
                // testFile("application/xml", "", ""),
                // testFile("application/vnd.wordperfect; version=6.x", "", ""),
                // testFile("application/pkcs7-signature", "", ""),
                // testFile("application/vnd.wordperfect; version=5.1", "", ""),
                // testFile("application/vnd.wordperfect; version=5.0", "", ""),
                // testFile("application/x-arj-compressed", "", ""),
                // testFile("application/geotopic", "", ""),
                // testFile("text/x-java-source", "java", ""),
                // testFile("audio/basic", "au", ""),
                // testFile("application/pcisdk", "", ""),
                // testFile("application/x-rik", "", ""),
                // testFile("audio/opus", "", ""),
                // testFile(MIMETYPE_IMAGE_JP2, "jp2", ""),
                // testFile("application/x-gtx", "", ""),
                // testFile("application/x-object", "", ""),
                // testFile("application/vnd.ms-wordml", "", ""),
                // testFile("image/x-wmf", "", ""),
                // testFile("application/x-rpf-toc", "", ""),
                // testFile("application/x-srtmhgt", "", ""),
                // testFile("application/x-generic-bin", "", ""),
                // testFile("text/vnd.iptc.anpa", "", ""),
                // testFile("application/x-msmetafile", "", ""),
                // testFile("application/x-wms", "", ""),
                // testFile("video/x-oggrgb", "", ""),
                // testFile("image/xcf", "", ""),
                // testFile("application/photoshop", "", ""),
                // testFile("application/x-lz4", "", ""),
                // testFile("application/x-7z-compressed", "", ""),
                // testFile("application/gff", "", ""),
                // testFile("video/x-oggyuv", "", ""),
                // testFile("application/x-msdownload", "", ""),
                // testFile("image/icns", "", ""),
                // testFile("application/x-emf", "", ""),
                // testFile("application/x-geo-pdf", "", ""),
                // testFile("video/x-ogg-uvs", "", ""),
                testFile(MIMETYPE_VIDEO_FLV, "flv", "quick.flv"),
                // testFile("application/x-zip-compressed", "", ""),
                // testFile("application/gzip", "", ""),
                // testFile("application/x-tika-unix-dump", "", ""),
                // testFile("application/x-coasp", "", ""),
                // testFile("application/x-dipex", "", ""),
                // testFile("application/x-til", "", ""),
                // testFile("application/x-gzip", "gzip", ""),
                // testFile("application/x-gs7bg", "", ""),
                // testFile("application/x-unix-archive", "", ""),
                // testFile("application/x-elf", "", ""),
                // testFile("application/dted", "", ""),
                // testFile("application/x-rasterlite", "", ""),
                // testFile("audio/x-mp4a", "", ""),
                // testFile("application/x-gzip-compressed", "", ""),
                // testFile("application/x-chm", "", ""),
                // testFile("image/hfa", "", ""),

                // Special test cases from the repo tests
                // ======================================

                // Test for MNT-577: Alfresco is running 100% CPU for over 10 minutes while extracting metadata for
                // Word office document
                // testFile(MIMETYPE_OPENXML_WORDPROCESSING, "docx", "problemFootnotes2.docx")

                // Test MNT-15219 Excel (.xlsx) containing xmls (shapes/drawings) with multi byte characters may
                // cause OutOfMemory in Tika Note - doesn't use extractFromMimetype
                testFile(MIMETYPE_OPENXML_SPREADSHEET, "xlsx", "dmsu1332-reproduced.xlsx")

        );
    }

    @ParameterizedTest
    @MethodSource("tika2_2_1_upgradeFailures")
    public void testTika_2_2_1_upgradeFailures(FileInfo fileInfo)
    {
        super.testTransformation(fileInfo);
    }

    private static Stream<FileInfo> tika2_2_1_upgradeFailures()
    {
        // When we upgraded to Tika 2.2.1 from 2.2.0:
        // - the original OfficeOpenXMLCore.SUBJECT raw metadata value started being null.
        // - the replacement TikaCoreProperties.SUBJECT raw metadata changed into a multi value
        // The following test files were the ones that failed.
        return Stream.of(
                testFile(MIMETYPE_OPENDOCUMENT_GRAPHICS_TEMPLATE, "otg", "quick.otg"),
                testFile(MIMETYPE_OPENOFFICE1_WRITER, "sxw", "quick.sxw"),
                testFile(MIMETYPE_OPENDOCUMENT_GRAPHICS, "odg", "quick.odg"),
                testFile(MIMETYPE_OPENDOCUMENT_TEXT, "odt", "quick.odt"),
                testFile(MIMETYPE_OPENDOCUMENT_TEXT_TEMPLATE, "ott", "quick.ott"),
                testFile(MIMETYPE_OPENDOCUMENT_FORMULA, "odf", "quick.odf"),
                testFile(MIMETYPE_PDF, "pdf", "quick.pdf"));
    }
}
