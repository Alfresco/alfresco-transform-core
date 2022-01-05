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
package org.alfresco.transformer;

import static java.text.MessageFormat.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.alfresco.transformer.EngineClient.sendTRequest;
import static org.alfresco.transformer.TestFileInfo.testFile;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_EXCEL;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_HTML;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_IMAGE_SVG;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_GRAPHICS;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_PRESENTATION;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_SPREADSHEET;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_TEXT;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENXML_PRESENTATION;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENXML_SPREADSHEET;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENXML_WORDPROCESSING;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_PDF;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_PPT;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_RTF;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_TEXT_CSV;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_TSV;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_VISIO;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_VISIO_2013;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_WORD;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_WORDPERFECT;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_XML;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE_MACRO;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW_MACRO;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OUTLOOK_MSG;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_DITA;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_SXI;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_SXC;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_STW;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_STI;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_STC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.HttpStatus.OK;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

/**
 * @author Cezar Leahu
 */
public class LibreOfficeTransformationIT
{
    private static final Logger logger = LoggerFactory.getLogger(LibreOfficeTransformationIT.class);
    private static final String ENGINE_URL = "http://localhost:8090";
    private static final Set<TestFileInfo> spreadsheetTargets = ImmutableSet.of(
        testFile(MIMETYPE_TEXT_CSV, "csv",null),
        testFile(MIMETYPE_HTML,"html",null),
        testFile(MIMETYPE_OPENDOCUMENT_SPREADSHEET,"ods",null),
        testFile(MIMETYPE_PDF,"pdf",null),
        testFile(MIMETYPE_TSV,"tsv",null),
        testFile(MIMETYPE_EXCEL,"xls",null)
    );

    private static final Set<TestFileInfo> documentsTargets = ImmutableSet.of(
        testFile(MIMETYPE_WORD,"doc",null),
        testFile(MIMETYPE_HTML,"html",null),
        testFile(MIMETYPE_OPENDOCUMENT_TEXT,"odt",null),
        testFile(MIMETYPE_PDF,"pdf",null),
        testFile(MIMETYPE_RTF,"rtf",null)
    );

    private static final Set<TestFileInfo> graphicTargets = ImmutableSet.of(
        testFile(MIMETYPE_PDF,"pdf",null),
        testFile(MIMETYPE_IMAGE_SVG,"svg",null)
    );

    private static final Set<TestFileInfo> presentationTargets = ImmutableSet.of(
        testFile(MIMETYPE_HTML,"html",null),
        testFile(MIMETYPE_OPENDOCUMENT_PRESENTATION,"odp",null),
        testFile(MIMETYPE_PPT,"ppt",null),
        testFile(MIMETYPE_PDF,"pdf",null)
    );

    private static final Set<TestFileInfo> pdfTarget = ImmutableSet.of(
        testFile(MIMETYPE_PDF,"pdf",null)
    );

    private static final Set<TestFileInfo> txtTarget = ImmutableSet.of(
            testFile(MIMETYPE_TEXT_PLAIN,"txt",null)
    );

    private static final Map<String,TestFileInfo> TEST_FILES = Stream.of(
        testFile(MIMETYPE_WORD                                 ,"doc"  ,"quick.doc"),
        testFile(MIMETYPE_OPENXML_WORDPROCESSING               ,"docx" ,"quick.docx"),
        testFile(MIMETYPE_OPENDOCUMENT_GRAPHICS                ,"odg"  ,"quick.odg"),
        testFile(MIMETYPE_OPENDOCUMENT_PRESENTATION            ,"odp"  ,"quick.odp"),
        testFile(MIMETYPE_OPENDOCUMENT_SPREADSHEET             ,"ods"  ,"quick.ods"),
        testFile(MIMETYPE_OPENDOCUMENT_TEXT                    ,"odt"  ,"quick.odt"),
        testFile(MIMETYPE_PPT                                  ,"ppt"  ,"quick.ppt"),
        testFile(MIMETYPE_OPENXML_PRESENTATION                 ,"pptx" ,"quick.pptx"),
        testFile(MIMETYPE_VISIO                                ,"vdx"  ,"quick.vdx"),
        testFile(MIMETYPE_VISIO_2013                           ,"vsd"  ,"quick.vsd"),
        testFile(MIMETYPE_WORDPERFECT                          ,"wpd"  ,"quick.wpd"),
        testFile(MIMETYPE_EXCEL                                ,"xls"  ,"quick.xls" ),
        testFile(MIMETYPE_OPENXML_SPREADSHEET                  ,"xlsx" ,"quick.xlsx"),
        testFile(MIMETYPE_TEXT_CSV                             ,"csv"  ,"people.csv"),
        testFile(MIMETYPE_RTF                                  ,"rtf"  ,"sample.rtf"),
        testFile(MIMETYPE_HTML                                 ,"html" ,"quick.html"),
        testFile(MIMETYPE_XML                                  ,"xml" ,"quick.xml"),
        testFile(MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE_MACRO   ,"xltm" ,"quick.xltm"),
        testFile(MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW       ,"ppsx" ,"quick.ppsx"),
        testFile(MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW_MACRO ,"ppsm" ,"quick.ppsm"),
        testFile(MIMETYPE_DITA                                 ,"dita" ,"quick.dita"),
        testFile(MIMETYPE_TEXT_PLAIN                           ,"txt" ,"quick.txt"),
        testFile(MIMETYPE_STC                                  ,"stc" ,"quick.stc"),
        testFile(MIMETYPE_STI                                  ,"sti" ,"quick.sti"),
        testFile(MIMETYPE_STW                                  ,"stw" ,"quick.stw"),
        testFile(MIMETYPE_SXC                                  ,"sxc" ,"quick.sxc"),
        testFile(MIMETYPE_SXI                                  ,"sxi" ,"quick.sxi"),
        testFile(MIMETYPE_TSV                                  ,"tsv"  ,"sample.tsv")
    ).collect(toMap(TestFileInfo::getPath, identity()));

    public static Stream<Pair<TestFileInfo, TestFileInfo>> engineTransformations()
    {
        return Stream
            .of(
                allTargets("quick.doc", documentsTargets),
                allTargets("quick.docx", documentsTargets),
                allTargets("quick.html", documentsTargets),
                allTargets("quick.odt", documentsTargets),
                allTargets("quick.wpd", documentsTargets),
                allTargets("quick.txt", documentsTargets),
                allTargets("sample.rtf", documentsTargets),

                allTargets("quick.odp", presentationTargets),
                allTargets("quick.ppt", presentationTargets),
                allTargets("quick.pptx", presentationTargets),

                allTargets("quick.odg", graphicTargets),
                allTargets("quick.vdx", graphicTargets),
                allTargets("quick.vsd", graphicTargets),

                allTargets("quick.ods", spreadsheetTargets),
                allTargets("quick.xls", spreadsheetTargets),
                allTargets("quick.xlsx", spreadsheetTargets),
                allTargets("people.csv", spreadsheetTargets),
                allTargets("sample.tsv", spreadsheetTargets),

                allTargets("quick.xml", pdfTarget),
                allTargets("quick.xltm", pdfTarget),
                allTargets("quick.dita", pdfTarget),
                allTargets("quick.ppsm", pdfTarget),
                allTargets("quick.ppsx", pdfTarget),
                allTargets("quick.stc", pdfTarget),
                allTargets("quick.sti", pdfTarget),
                allTargets("quick.stw", pdfTarget),
                allTargets("quick.sxc", pdfTarget),
                allTargets("quick.sxi", pdfTarget)

                )
            .flatMap(identity());
    }

    @ParameterizedTest
    @MethodSource("engineTransformations")
    public void testTransformation(final Pair<TestFileInfo, TestFileInfo> entry)
    {
        final String sourceFile = entry.getLeft().getPath();
        final String targetExtension = entry.getRight().getExtension();
        final String sourceMimetype = entry.getLeft().getMimeType();
        final String targetMimetype = entry.getRight().getMimeType();
        final String descriptor = format("Transform ({0}, {1} -> {2}, {3})",
            sourceFile, sourceMimetype, targetMimetype, targetExtension);
        try
        {
            final ResponseEntity<Resource> response = sendTRequest(ENGINE_URL, sourceFile, sourceMimetype,
                targetMimetype, targetExtension);
            assertEquals(OK, response.getStatusCode(), descriptor);
        }
        catch (Exception e)
        {
            fail(descriptor + " exception: " + e.getMessage());
        }
    }

    private static Stream<Pair<TestFileInfo, TestFileInfo>> allTargets(final String sourceFile,
        final Set<TestFileInfo> mimetypes)
    {
        return mimetypes
            .stream()
            //Filter out duplicate mimetypes. eg. We do not want "Transform (quick.doc, application/msword -> application/msword, doc)" as these are not contained in the engine_config
            .filter(type -> !type.getMimeType().equals(TEST_FILES.get(sourceFile).getMimeType()))
            // Edge case: Transform (quick.ods, application/vnd.oasis.opendocument.spreadsheet -> text/csv, csv) not in engine_config
            .filter(type -> !(TEST_FILES.get(sourceFile).getMimeType().equals(MIMETYPE_OPENDOCUMENT_SPREADSHEET) && type.getMimeType().equals(MIMETYPE_TEXT_CSV)))
            .map(k -> Pair.of(TEST_FILES.get(sourceFile), k));
    }
}
