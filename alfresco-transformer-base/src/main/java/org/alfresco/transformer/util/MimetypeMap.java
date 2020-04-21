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
package org.alfresco.transformer.util;

/**
 * Partially duplicated from *alfresco-data-model*.
 */
public interface MimetypeMap
{
    String PREFIX_APPLICATION = "application/";
    String PREFIX_AUDIO = "audio/";
    String PREFIX_IMAGE = "image/";
    String PREFIX_MESSAGE = "message/";
    String PREFIX_MODEL = "model/";
    String PREFIX_MULTIPART = "multipart/";
    String PREFIX_TEXT = "text/";
    String PREFIX_VIDEO = "video/";
    String EXTENSION_BINARY = "bin";
    String MACOS_RESOURCE_FORK_FILE_NAME_PREFIX = "._";
    String MIMETYPE_MULTIPART_ALTERNATIVE = "multipart/alternative";
    String MIMETYPE_TEXT_PLAIN = "text/plain";
    String MIMETYPE_TEXT_MEDIAWIKI = "text/mediawiki";
    String MIMETYPE_TEXT_CSS = "text/css";
    String MIMETYPE_TEXT_CSV = "text/csv";
    String MIMETYPE_TEXT_JAVASCRIPT = "text/javascript";
    String MIMETYPE_XML = "text/xml";
    String MIMETYPE_HTML = "text/html";
    String MIMETYPE_XHTML = "application/xhtml+xml";
    String MIMETYPE_PDF = "application/pdf";
    String MIMETYPE_JSON = "application/json";
    String MIMETYPE_WORD = "application/msword";
    String MIMETYPE_EXCEL = "application/vnd.ms-excel";
    String MIMETYPE_BINARY = "application/octet-stream";
    String MIMETYPE_PPT = "application/vnd.ms-powerpoint";
    String MIMETYPE_APP_DWG = "application/dwg";
    String MIMETYPE_IMG_DWG = "image/vnd.dwg";
    String MIMETYPE_VIDEO_AVI = "video/x-msvideo";
    String MIMETYPE_VIDEO_QUICKTIME = "video/quicktime";
    String MIMETYPE_VIDEO_WMV = "video/x-ms-wmv";
    String MIMETYPE_VIDEO_3GP = "video/3gpp";
    String MIMETYPE_VIDEO_3GP2 = "video/3gpp2";
    String MIMETYPE_DITA = "application/dita+xml";
    String MIMETYPE_FLASH = "application/x-shockwave-flash";
    String MIMETYPE_VIDEO_FLV = "video/x-flv";
    String MIMETYPE_APPLICATION_FLA = "application/x-fla";
    String MIMETYPE_VIDEO_MPG = "video/mpeg";
    String MIMETYPE_VIDEO_MP4 = "video/mp4";
    String MIMETYPE_IMAGE_BMP = "image/bmp";
    String MIMETYPE_IMAGE_CGM = "image/cgm";
    String MIMETYPE_IMAGE_GIF = "image/gif";
    String MIMETYPE_IMAGE_IEF = "image/ief";
    String MIMETYPE_IMAGE_JPEG = "image/jpeg";
    String MIMETYPE_IMAGE_JP2 = "image/jp2";
    String MIMETYPE_IMAGE_RGB = "image/x-rgb";
    String MIMETYPE_IMAGE_SVG = "image/svg+xml";
    String MIMETYPE_IMAGE_PBM = "image/x-portable-bitmap";
    String MIMETYPE_IMAGE_PGM = "image/x-portable-graymap";
    String MIMETYPE_IMAGE_PNM = "image/x-portable-anymap";
    String MIMETYPE_IMAGE_PNG = "image/png";
    String MIMETYPE_IMAGE_PPM = "image/x-portable-pixmap";
    String MIMETYPE_IMAGE_PPJ = "image/vnd.adobe.premiere";
    String MIMETYPE_IMAGE_PSD = "image/vnd.adobe.photoshop";
    String MIMETYPE_IMAGE_RAS = "image/x-cmu-raster";
    String MIMETYPE_IMAGE_TIFF = "image/tiff";
    String MIMETYPE_IMAGE_RAW_DNG = "image/x-raw-adobe";
    String MIMETYPE_IMAGE_RAW_3FR = "image/x-raw-hasselblad";
    String MIMETYPE_IMAGE_RAW_RAF = "image/x-raw-fuji";
    String MIMETYPE_IMAGE_RAW_CR2 = "image/x-raw-canon";
    String MIMETYPE_IMAGE_RAW_K25 = "image/x-raw-kodak";
    String MIMETYPE_IMAGE_RAW_MRW = "image/x-raw-minolta";
    String MIMETYPE_IMAGE_RAW_NEF = "image/x-raw-nikon";
    String MIMETYPE_IMAGE_RAW_ORF = "image/x-raw-olympus";
    String MIMETYPE_IMAGE_RAW_PEF = "image/x-raw-pentax";
    String MIMETYPE_IMAGE_RAW_ARW = "image/x-raw-sony";
    String MIMETYPE_IMAGE_RAW_X3F = "image/x-raw-sigma";
    String MIMETYPE_IMAGE_RAW_RW2 = "image/x-raw-panasonic";
    String MIMETYPE_IMAGE_RAW_RWL = "image/x-raw-leica";
    String MIMETYPE_IMAGE_RAW_R3D = "image/x-raw-red";
    String MIMETYPE_IMAGE_DWT = "image/x-dwt";
    String MIMETYPE_IMAGE_XBM = "image/x-xbitmap";
    String MIMETYPE_IMAGE_XPM = "image/x-xpixmap";
    String MIMETYPE_IMAGE_XWD = "image/x-xwindowdump";
    String MIMETYPE_APPLICATION_EPS = "application/eps";
    String MIMETYPE_APPLICATION_PS = "application/postscript";
    String MIMETYPE_JAVASCRIPT = "application/x-javascript";
    String MIMETYPE_ZIP = "application/zip";
    String MIMETYPE_OPENSEARCH_DESCRIPTION = "application/opensearchdescription+xml";
    String MIMETYPE_ATOM = "application/atom+xml";
    String MIMETYPE_RSS = "application/rss+xml";
    String MIMETYPE_RFC822 = "message/rfc822";
    String MIMETYPE_OUTLOOK_MSG = "application/vnd.ms-outlook";
    String MIMETYPE_VISIO = "application/vnd.visio";
    String MIMETYPE_VISIO_2013 = "application/vnd.visio2013";
    String MIMETYPE_APPLICATION_ILLUSTRATOR = "application/illustrator";
    String MIMETYPE_APPLICATION_PHOTOSHOP = "image/vnd.adobe.photoshop";
    String MIMETYPE_ENCRYPTED_OFFICE = "application/x-tika-ooxml-protected";
    String MIMETYPE_OPENDOCUMENT_TEXT = "application/vnd.oasis.opendocument.text";
    String MIMETYPE_OPENDOCUMENT_TEXT_TEMPLATE = "application/vnd.oasis.opendocument.text-template";
    String MIMETYPE_OPENDOCUMENT_GRAPHICS = "application/vnd.oasis.opendocument.graphics";
    String MIMETYPE_OPENDOCUMENT_GRAPHICS_TEMPLATE = "application/vnd.oasis.opendocument.graphics-template";
    String MIMETYPE_OPENDOCUMENT_PRESENTATION = "application/vnd.oasis.opendocument.presentation";
    String MIMETYPE_OPENDOCUMENT_PRESENTATION_TEMPLATE = "application/vnd.oasis.opendocument.presentation-template";
    String MIMETYPE_OPENDOCUMENT_SPREADSHEET = "application/vnd.oasis.opendocument.spreadsheet";
    String MIMETYPE_OPENDOCUMENT_SPREADSHEET_TEMPLATE = "application/vnd.oasis.opendocument.spreadsheet-template";
    String MIMETYPE_OPENDOCUMENT_CHART = "application/vnd.oasis.opendocument.chart";
    String MIMETYPE_OPENDOCUMENT_CHART_TEMPLATE = "applicationvnd.oasis.opendocument.chart-template";
    String MIMETYPE_OPENDOCUMENT_IMAGE = "application/vnd.oasis.opendocument.image";
    String MIMETYPE_OPENDOCUMENT_IMAGE_TEMPLATE = "applicationvnd.oasis.opendocument.image-template";
    String MIMETYPE_OPENDOCUMENT_FORMULA = "application/vnd.oasis.opendocument.formula";
    String MIMETYPE_OPENDOCUMENT_FORMULA_TEMPLATE = "applicationvnd.oasis.opendocument.formula-template";
    String MIMETYPE_OPENDOCUMENT_TEXT_MASTER = "application/vnd.oasis.opendocument.text-master";
    String MIMETYPE_OPENDOCUMENT_TEXT_WEB = "application/vnd.oasis.opendocument.text-web";
    String MIMETYPE_OPENDOCUMENT_DATABASE = "application/vnd.oasis.opendocument.database";
    String MIMETYPE_OPENOFFICE1_WRITER = "application/vnd.sun.xml.writer";
    String MIMETYPE_OPENOFFICE1_CALC = "application/vnd.sun.xml.calc";
    String MIMETYPE_OPENOFFICE1_DRAW = "application/vnd.sun.xml.draw";
    String MIMETYPE_OPENOFFICE1_IMPRESS = "application/vnd.sun.xml.impress";
    String MIMETYPE_OPENXML_WORDPROCESSING = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    String MIMETYPE_OPENXML_WORDPROCESSING_MACRO = "application/vnd.ms-word.document.macroenabled.12";
    String MIMETYPE_OPENXML_WORD_TEMPLATE = "application/vnd.openxmlformats-officedocument.wordprocessingml.template";
    String MIMETYPE_OPENXML_WORD_TEMPLATE_MACRO = "application/vnd.ms-word.template.macroenabled.12";
    String MIMETYPE_OPENXML_SPREADSHEET = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    String MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE = "application/vnd.openxmlformats-officedocument.spreadsheetml.template";
    String MIMETYPE_OPENXML_SPREADSHEET_MACRO = "application/vnd.ms-excel.sheet.macroenabled.12";
    String MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE_MACRO = "application/vnd.ms-excel.template.macroenabled.12";
    String MIMETYPE_OPENXML_SPREADSHEET_ADDIN_MACRO = "application/vnd.ms-excel.addin.macroenabled.12";
    String MIMETYPE_OPENXML_SPREADSHEET_BINARY_MACRO = "application/vnd.ms-excel.sheet.binary.macroenabled.12";
    String MIMETYPE_OPENXML_PRESENTATION = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    String MIMETYPE_OPENXML_PRESENTATION_MACRO = "application/vnd.ms-powerpoint.presentation.macroenabled.12";
    String MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW = "application/vnd.openxmlformats-officedocument.presentationml.slideshow";
    String MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW_MACRO = "application/vnd.ms-powerpoint.slideshow.macroenabled.12";
    String MIMETYPE_OPENXML_PRESENTATION_TEMPLATE = "application/vnd.openxmlformats-officedocument.presentationml.template";
    String MIMETYPE_OPENXML_PRESENTATION_TEMPLATE_MACRO = "application/vnd.ms-powerpoint.template.macroenabled.12";
    String MIMETYPE_OPENXML_PRESENTATION_ADDIN = "application/vnd.ms-powerpoint.addin.macroenabled.12";
    String MIMETYPE_OPENXML_PRESENTATION_SLIDE = "application/vnd.openxmlformats-officedocument.presentationml.slide";
    String MIMETYPE_OPENXML_PRESENTATION_SLIDE_MACRO = "application/vnd.ms-powerpoint.slide.macroenabled.12";
    String MIMETYPE_STAROFFICE5_DRAW = "application/vnd.stardivision.draw";
    String MIMETYPE_STAROFFICE5_CALC = "application/vnd.stardivision.calc";
    String MIMETYPE_STAROFFICE5_IMPRESS = "application/vnd.stardivision.impress";
    String MIMETYPE_STAROFFICE5_IMPRESS_PACKED = "application/vnd.stardivision.impress-packed";
    String MIMETYPE_STAROFFICE5_CHART = "application/vnd.stardivision.chart";
    String MIMETYPE_STAROFFICE5_WRITER = "application/vnd.stardivision.writer";
    String MIMETYPE_STAROFFICE5_WRITER_GLOBAL = "application/vnd.stardivision.writer-global";
    String MIMETYPE_STAROFFICE5_MATH = "application/vnd.stardivision.math";
    String MIMETYPE_IWORK_KEYNOTE = "application/vnd.apple.keynote";
    String MIMETYPE_IWORK_NUMBERS = "application/vnd.apple.numbers";
    String MIMETYPE_IWORK_PAGES = "application/vnd.apple.pages";
    String MIMETYPE_APPLEFILE = "application/applefile";
    String MIMETYPE_WORDPERFECT = "application/wordperfect";
    String MIMETYPE_MP3 = "audio/mpeg";
    String MIMETYPE_AUDIO_MP4 = "audio/mp4";
    String MIMETYPE_VORBIS = "audio/vorbis";
    String MIMETYPE_FLAC = "audio/x-flac";
    String MIMETYPE_ACP = "application/acp";
}
