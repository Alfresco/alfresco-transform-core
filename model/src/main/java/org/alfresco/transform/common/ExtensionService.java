/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package org.alfresco.transform.common;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_DITA;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_EXCEL;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_SVG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_GRAPHICS;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_PRESENTATION;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_PRESENTATION_TEMPLATE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_SPREADSHEET;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_SPREADSHEET_TEMPLATE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_TEXT;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENDOCUMENT_TEXT_TEMPLATE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENOFFICE1_CALC;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENOFFICE1_IMPRESS;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENOFFICE1_WRITER;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_PRESENTATION;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_PRESENTATION_ADDIN;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_PRESENTATION_MACRO;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_PRESENTATION_SLIDE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_PRESENTATION_SLIDE_MACRO;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_PRESENTATION_TEMPLATE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_PRESENTATION_TEMPLATE_MACRO;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_SPREADSHEET;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_SPREADSHEET_BINARY_MACRO;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_SPREADSHEET_MACRO;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE_MACRO;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_WORDPROCESSING;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_WORDPROCESSING_MACRO;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_WORD_TEMPLATE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_WORD_TEMPLATE_MACRO;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OUTLOOK_MSG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PPT;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_VISIO;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_VISIO_2013;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_WORD;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_WORDPERFECT;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_XHTML;
import static org.alfresco.transform.common.TransformerDebug.MIMETYPE_METADATA_EMBED;
import static org.alfresco.transform.common.TransformerDebug.MIMETYPE_METADATA_EXTRACT;

import java.util.Map;

/**
 * Provides mapping between mimtypes and file extensions, static and not configurable.
 * The correct extension is required for a small subset of transforms in pipelines which go through the
 * libreoffice transformer
 */
public class ExtensionService
{
    private final static String MIMETYPE_TAB_SEPARATED_VALUES="text/tab-separated-values";
    private final static String MIMETYPE_CALC_TEMPLATE="application/vnd.sun.xml.calc.template";
    private final static String MIMETYPE_IMPRESS_TEMPLATE="application/vnd.sun.xml.impress.template";
    private final static String MIMETYPE_WRITER_TEMPLATE="application/vnd.sun.xml.writer.template";

    private static final Map<String,String> mimetpeExtensions = Map.ofEntries(       
            Map.entry(MIMETYPE_WORD,                                "doc"), 
            Map.entry(MIMETYPE_OPENXML_WORDPROCESSING_MACRO,        "docm"),
            Map.entry(MIMETYPE_OPENXML_WORDPROCESSING,              "docx"),
            Map.entry(MIMETYPE_OPENXML_WORD_TEMPLATE_MACRO,         "dotm"),
            Map.entry(MIMETYPE_OPENXML_WORD_TEMPLATE,               "dotx"),
            Map.entry(MIMETYPE_OPENDOCUMENT_GRAPHICS,               "odg"),
            Map.entry(MIMETYPE_OPENDOCUMENT_PRESENTATION,           "odp"),
            Map.entry(MIMETYPE_OPENDOCUMENT_PRESENTATION_TEMPLATE,  "otp"),
            Map.entry(MIMETYPE_OPENDOCUMENT_SPREADSHEET,            "ods"), 
            Map.entry(MIMETYPE_OPENDOCUMENT_SPREADSHEET_TEMPLATE,   "ots"), 
            Map.entry(MIMETYPE_OPENDOCUMENT_TEXT,                   "odt"), 
            Map.entry(MIMETYPE_OPENDOCUMENT_TEXT_TEMPLATE,          "ott"),
            Map.entry(MIMETYPE_OPENXML_PRESENTATION_TEMPLATE_MACRO, "potm"),
            Map.entry(MIMETYPE_OPENXML_PRESENTATION_TEMPLATE,       "potx"),
            Map.entry(MIMETYPE_OPENXML_PRESENTATION_ADDIN,          "ppam"),
            Map.entry(MIMETYPE_PPT,                                 "ppt"), 
            Map.entry(MIMETYPE_OPENXML_PRESENTATION_MACRO,          "pptm"),
            Map.entry(MIMETYPE_OPENXML_PRESENTATION,                "pptx"),
            Map.entry(MIMETYPE_OPENXML_PRESENTATION_SLIDE_MACRO,    "sldm"),
            Map.entry(MIMETYPE_OPENXML_PRESENTATION_SLIDE,          "sldx"),
            Map.entry(MIMETYPE_CALC_TEMPLATE,                       "stc"),
            Map.entry(MIMETYPE_IMPRESS_TEMPLATE,                    "sti"),
            Map.entry(MIMETYPE_WRITER_TEMPLATE,                     "stw"),
            Map.entry(MIMETYPE_TAB_SEPARATED_VALUES,                "tsv"), 
            Map.entry(MIMETYPE_OPENOFFICE1_CALC,                    "sxc"),
            Map.entry(MIMETYPE_OPENOFFICE1_IMPRESS,                 "sxi"),
            Map.entry(MIMETYPE_OPENOFFICE1_WRITER,                  "sxw"),
            Map.entry(MIMETYPE_VISIO,                               "vsd"),
            Map.entry(MIMETYPE_VISIO_2013,                          "vsdx"),
            Map.entry(MIMETYPE_WORDPERFECT,                         "wp"),
            Map.entry(MIMETYPE_EXCEL,                               "xls"), 
            Map.entry(MIMETYPE_OPENXML_SPREADSHEET_BINARY_MACRO,    "xlsb"),
            Map.entry(MIMETYPE_OPENXML_SPREADSHEET_MACRO,           "xlsm"),
            Map.entry(MIMETYPE_OPENXML_SPREADSHEET,                 "xlsx"),
            Map.entry(MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE_MACRO,  "xltm"),
            Map.entry(MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW,      "ppsx"),
            Map.entry(MIMETYPE_OUTLOOK_MSG,                         "msg"),
            Map.entry(MIMETYPE_DITA,                                "dita"),
            Map.entry(MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE,        "xltx"),
            Map.entry(MIMETYPE_IMAGE_SVG,                           "svg"),
            Map.entry(MIMETYPE_TEXT_PLAIN,                          "txt"),
            Map.entry(MIMETYPE_XHTML,                               "xhtml")
    );

    public static String getExtensionForTargetMimetype(String targetMimetype, String sourceMimetype)
    {
        if (targetMimetype == null)
        {
            return null;
        }
        return getExtensionForMimetype(MIMETYPE_METADATA_EMBED.equals(targetMimetype) ? sourceMimetype : targetMimetype);
    }

    public static String getExtensionForMimetype(String mimetype)
    {
        if (mimetype == null)
        {
            return null;
        }
        if (mimetpeExtensions.containsKey(mimetype))
        {
            return mimetpeExtensions.get(mimetype);
        }
        if (MIMETYPE_METADATA_EXTRACT.equals(mimetype))
        {
            return "json";
        }
        // else fall back to the original implementation
        return splitMimetype(mimetype);
    }

    // Moved from Dispatcher. This fails to work in many cases, but we need to be backward compatible.
    private static String splitMimetype(final String mimetype)
    {
        final String[] parts = mimetype.split("[.\\-_|/]");
        return parts[parts.length - 1];
    }
}
