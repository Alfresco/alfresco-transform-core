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
package org.alfresco.transformer;

import static org.alfresco.transformer.TestFileInfo.testFile;
import static java.text.MessageFormat.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.alfresco.transformer.EngineClient.sendTRequest;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_WORD;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_GRAPHICS;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_PRESENTATION;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_SPREADSHEET;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_OPENDOCUMENT_TEXT;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_PPT;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_VISIO;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_VISIO_2013;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_WORDPERFECT;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_EXCEL;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_TEXT_CSV;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_RTF;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_HTML;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_TSV;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_PDF;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_IMAGE_SVG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.http.HttpStatus.OK;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.alfresco.transform.client.model.Mimetype;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.ImmutableSet;

/**
 * @author Cezar Leahu
 */
@RunWith(Parameterized.class)
public class LibreOfficeTransformationIT
{
    private static final Logger logger = LoggerFactory.getLogger(LibreOfficeTransformationIT.class);
    private static final String ENGINE_URL = "http://localhost:8090";
    private static final Set<TestFileInfo> spreadsheetTargetMimetypes = ImmutableSet.of(
        testFile(MIMETYPE_TEXT_CSV, "csv",null),
        testFile(MIMETYPE_HTML,"html",null),
        testFile(MIMETYPE_OPENDOCUMENT_SPREADSHEET,"ods",null),
        testFile(MIMETYPE_PDF,"pdf",null),
        testFile(MIMETYPE_TSV,"tsv",null),
        testFile(MIMETYPE_EXCEL,"xls",null)
    );

    private static final Set<TestFileInfo> documentsTargetMimetypes = ImmutableSet.of(
        testFile(MIMETYPE_WORD,"doc",null),
        testFile(MIMETYPE_HTML,"html",null),
        testFile(MIMETYPE_OPENDOCUMENT_TEXT,"odt",null),
        testFile(MIMETYPE_PDF,"pdf",null),
        testFile(MIMETYPE_RTF,"rtf",null)
    );

    private static final Set<TestFileInfo> graphicTargetMimetypes = ImmutableSet.of(
        testFile(MIMETYPE_PDF,"pdf",null),
        testFile(MIMETYPE_IMAGE_SVG,"svg",null)
    );

    private static final Set<TestFileInfo> presentationTargetMimetypes = ImmutableSet.of(
        testFile(MIMETYPE_HTML,"html",null),
        testFile(MIMETYPE_OPENDOCUMENT_PRESENTATION,"odp",null),
        testFile(MIMETYPE_PPT,"ppt",null),
        testFile(MIMETYPE_PDF,"pdf",null)
    );

    private final String sourceFile;
    private final String targetExtension;
    private final String sourceMimetype;
    private final String targetMimetype;

    private static final Map<String,TestFileInfo> TEST_FILES = Stream.of(
        testFile(MIMETYPE_WORD                      ,"doc"  ,"quick.doc"), 
        testFile(MIMETYPE_WORD                      ,"docx" ,"quick.docx"),
        testFile(MIMETYPE_OPENDOCUMENT_GRAPHICS     ,"odg"  ,"quick.odg"), 
        testFile(MIMETYPE_OPENDOCUMENT_PRESENTATION ,"odp"  ,"quick.odp"), 
        testFile(MIMETYPE_OPENDOCUMENT_SPREADSHEET  ,"ods"  ,"quick.ods"), 
        testFile(MIMETYPE_OPENDOCUMENT_TEXT         ,"odt"  ,"quick.odt"), 
        testFile(MIMETYPE_PPT                       ,"ppt"  ,"quick.ppt"), 
        testFile(MIMETYPE_PPT                       ,"pptx" ,"quick.pptx"),
        testFile(MIMETYPE_VISIO                     ,"vdx"  ,"quick.vdx"), 
        testFile(MIMETYPE_VISIO_2013                ,"vsd"  ,"quick.vsd"), 
        testFile(MIMETYPE_WORDPERFECT               ,"wpd"  ,"quick.wpd"), 
        testFile(MIMETYPE_EXCEL                     ,"xls"  ,"quick.xls" ), 
        testFile(MIMETYPE_EXCEL                     ,"xlsx" ,"quick.xlsx"),
        testFile(MIMETYPE_TEXT_CSV                  ,"csv"  ,"people.csv"),
        testFile(MIMETYPE_RTF                       ,"rtf"  ,"sample.rtf"),
        testFile(MIMETYPE_HTML                      ,"html" ,"quick.html"),
        testFile(MIMETYPE_TSV                       ,"tsv"  ,"sample.tsv")
    ).collect(toMap(TestFileInfo::getPath, identity()));

    public LibreOfficeTransformationIT(final Pair<TestFileInfo, TestFileInfo> entry)
    {
        sourceFile = entry.getLeft().getPath();
        targetExtension = entry.getRight().getExtension();
        sourceMimetype = entry.getLeft().getMimeType();
        targetMimetype = entry.getRight().getMimeType();
    }

    @Parameterized.Parameters
    public static Set<Pair<TestFileInfo, TestFileInfo>> engineTransformations()
    {
        return Stream
            .of(
                allTargets("quick.doc", documentsTargetMimetypes),
                allTargets("quick.docx", documentsTargetMimetypes),
                allTargets("quick.odg", graphicTargetMimetypes),
                allTargets("quick.odp", presentationTargetMimetypes),
                allTargets("quick.ods", spreadsheetTargetMimetypes),
                allTargets("quick.odt", documentsTargetMimetypes),
                allTargets("quick.ppt", presentationTargetMimetypes),
                allTargets("quick.pptx", presentationTargetMimetypes),
                allTargets("quick.vdx", graphicTargetMimetypes),
                allTargets("quick.vsd", graphicTargetMimetypes),
                allTargets("quick.wpd", documentsTargetMimetypes),
                allTargets("quick.xls", spreadsheetTargetMimetypes),
                allTargets("quick.xlsx", spreadsheetTargetMimetypes),

                allTargets("people.csv", spreadsheetTargetMimetypes),
                allTargets("sample.rtf", documentsTargetMimetypes),
                allTargets("quick.html", documentsTargetMimetypes),
                allTargets("sample.tsv", spreadsheetTargetMimetypes)
            )
            .flatMap(identity())
            .collect(toSet());
    }

    @Test
    public void testTransformation()
    {
        final String descriptor = format("Transform ({0}, {1} -> {2}, {3})",
            sourceFile, sourceMimetype, targetMimetype, targetExtension);
        try
        {
            final ResponseEntity<Resource> response = sendTRequest(ENGINE_URL, sourceFile, sourceMimetype,
                targetMimetype, targetExtension);
            assertEquals(descriptor, OK, response.getStatusCode());
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
            .map(k -> Pair.of(TEST_FILES.get(sourceFile), k));
    }
}
