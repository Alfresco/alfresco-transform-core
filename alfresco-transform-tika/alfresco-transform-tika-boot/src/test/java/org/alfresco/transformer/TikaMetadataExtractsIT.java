/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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
package org.alfresco.transformer;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_APP_DWG;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OUTLOOK_MSG;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_RFC822;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_XHTML;
import static org.alfresco.transformer.TestFileInfo.testFile;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_EXCEL;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_IMG_DWG;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_MP3;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_CHART;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_CHART_TEMPLATE;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_DATABASE;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_FORMULA;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_FORMULA_TEMPLATE;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_GRAPHICS;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_GRAPHICS_TEMPLATE;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_IMAGE;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_IMAGE_TEMPLATE;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_PRESENTATION;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_PRESENTATION_TEMPLATE;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_SPREADSHEET;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_SPREADSHEET_TEMPLATE;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_TEXT;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_TEXT_MASTER;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_TEXT_TEMPLATE;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_TEXT_WEB;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_PPT;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_VISIO;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_VISIO_2013;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_WORD;

/**
 * Metadata integration tests in the Tika T-Engine.
 *
 * @author adavis
 */
@RunWith(Parameterized.class)
public class TikaMetadataExtractsIT extends AbstractMetadataExtractsIT
{
    public TikaMetadataExtractsIT(TestFileInfo testFileInfo)
    {
        super(testFileInfo);
    }

    @Parameterized.Parameters
    public static Set<TestFileInfo> engineTransformations()
    {
        // The following files are the ones tested in the content repository.
        // There are many more mimetypes supported by these extractors.
        // There a line has been commented out, it is referenced in the repository code but is ignored because there is
        // either no quick file or the target extension has not been registered.
        return Stream.of(
                // DWGMetadataExtractor
                testFile(MIMETYPE_APP_DWG, "dwg", "quick2010CustomProps.dwg"),

                // MailMetadataExtractor
                testFile(MIMETYPE_OUTLOOK_MSG, "msg", "quick.msg"),

                // MP3MetadataExtractor
                testFile(MIMETYPE_MP3, "mp3", "quick.mp3"),

                // OfficeMetadataExtractor
                testFile(MIMETYPE_WORD, "doc", "quick.doc"),
                testFile(MIMETYPE_EXCEL, "xls", "quick.xls"),
                testFile(MIMETYPE_PPT, "ppt", "quick.ppt"),
                testFile(MIMETYPE_VISIO, "vsd", "quick.vsd"),
//                testFile(MIMETYPE_VISIO_2013, "vsdx", "quick.vsdx"),

                // OpenDocumentMetadataExtractor
                testFile(MIMETYPE_OPENDOCUMENT_TEXT, "odt", "quick.odt"),
                testFile(MIMETYPE_OPENDOCUMENT_TEXT_TEMPLATE, "ott", "quick.ott"),
                testFile(MIMETYPE_OPENDOCUMENT_GRAPHICS, "odg", "quick.odg"),
                testFile(MIMETYPE_OPENDOCUMENT_GRAPHICS_TEMPLATE, "otg", "quick.otg"),
                testFile(MIMETYPE_OPENDOCUMENT_PRESENTATION, "odp", "quick.odp"),
                testFile(MIMETYPE_OPENDOCUMENT_PRESENTATION_TEMPLATE, "otp", "quick.otp"),
                testFile(MIMETYPE_OPENDOCUMENT_SPREADSHEET, "ods", "quick.ods"),
                testFile(MIMETYPE_OPENDOCUMENT_SPREADSHEET_TEMPLATE, "ots", "quick.ots"),
//                testFile(MIMETYPE_OPENDOCUMENT_CHART, "odc", "quick.odc"),
//                testFile(MIMETYPE_OPENDOCUMENT_CHART_TEMPLATE, "???", "quick.???"),
//                testFile(MIMETYPE_OPENDOCUMENT_IMAGE, "odi", "quick.odi"),
//                testFile(MIMETYPE_OPENDOCUMENT_IMAGE_TEMPLATE, "???", "quick.???"),
                testFile(MIMETYPE_OPENDOCUMENT_FORMULA, "odf", "quick.odf"),
//                testFile(MIMETYPE_OPENDOCUMENT_FORMULA_TEMPLATE, "???", "quick.???"),
//                testFile(MIMETYPE_OPENDOCUMENT_TEXT_MASTER, "odm", "quick.odm"),
//                testFile(MIMETYPE_OPENDOCUMENT_TEXT_WEB, "oth", "quick.oth"),
//                testFile(MIMETYPE_OPENDOCUMENT_DATABASE, "odb", "quick.odb"),

                // PdfBoxMetadataExtractor
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),


                // PoiMetadataExtractor
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),


                // TikaAudioMetadataExtractor
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),


                // TikaAutoMetadataExtractor
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),
//                testFile(MIMETYPE_, "", "quick."),

                testFile(MIMETYPE_IMAGE_JPEG, "jpg", "quick.jpg")

        ).collect(toSet());


    }
}
