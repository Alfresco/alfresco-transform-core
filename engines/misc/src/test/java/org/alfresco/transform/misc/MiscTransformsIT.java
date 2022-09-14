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
package org.alfresco.transform.misc;

import org.alfresco.transform.base.clients.FileInfo;
import org.alfresco.transform.base.clients.SourceTarget;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.stream.Stream;

import static java.text.MessageFormat.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.alfresco.transform.base.clients.HttpClient.sendTRequest;
import static org.alfresco.transform.base.clients.FileInfo.testFile;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_DITA;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_EXCEL;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_GIF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_TIFF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IWORK_KEYNOTE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IWORK_NUMBERS;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IWORK_PAGES;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_JAVASCRIPT;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_PRESENTATION;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_SPREADSHEET;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OPENXML_WORDPROCESSING;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_OUTLOOK_MSG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PPT;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_RFC822;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_CSS;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_CSV;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_JAVASCRIPT;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_MEDIAWIKI;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_WORD;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_XML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.HttpStatus.OK;

/**
 * @author Cezar Leahu
 */
public class MiscTransformsIT
{
    private static final String ENGINE_URL = "http://localhost:8090";

    private static final Map<String, FileInfo> TEST_FILES = Stream.of(
        testFile(MIMETYPE_IMAGE_GIF, "gif", "quick.gif"),
        testFile(MIMETYPE_IMAGE_JPEG, "jpg", "quick.jpg"),
        testFile(MIMETYPE_IMAGE_PNG, "png", "quick.png"),
        testFile(MIMETYPE_IMAGE_TIFF, "tiff", "quick.tiff"),
        testFile(MIMETYPE_WORD, "doc", "quick.doc"),
        testFile(MIMETYPE_OPENXML_WORDPROCESSING, "docx", "quick.docx"),
        testFile(MIMETYPE_EXCEL, "xls", "quick.xls"),
        testFile(MIMETYPE_OPENXML_SPREADSHEET, "xlsx", "quick.xlsx"),
        testFile(MIMETYPE_PPT, "ppt", "quick.ppt"),
        testFile(MIMETYPE_OPENXML_PRESENTATION, "pptx", "quick.pptx"),
        testFile(MIMETYPE_OUTLOOK_MSG, "msg", "quick.msg"),
        testFile(MIMETYPE_PDF, "pdf", "quick.pdf"),
        testFile(MIMETYPE_TEXT_PLAIN, "txt", "quick2.txt"),

        testFile("text/richtext", "rtf", "sample.rtf"),
        testFile("text/sgml", "sgml", "sample.sgml"),
        testFile("text/tab-separated-values", "tsv", "sample.tsv"),
        testFile("text/x-setext", "etx", "sample.etx"),
        testFile("text/x-java-source", "java", "Sample.java.txt"),
        testFile("text/x-jsp", "jsp", "sample.jsp.txt"),
        testFile("text/x-markdown", "md", "sample.md"),
        testFile("text/calendar", "ics", "sample.ics"),

        testFile(MIMETYPE_TEXT_MEDIAWIKI, "mw", "sample.mw"),
        testFile(MIMETYPE_TEXT_CSS, "css", "style.css"),
        testFile(MIMETYPE_TEXT_CSV, "csv", "people.csv"),
        testFile(MIMETYPE_TEXT_JAVASCRIPT, "js", "script.js"),
        testFile(MIMETYPE_XML, "xml", "quick.xml"),
        testFile(MIMETYPE_HTML, "html", "quick.html"),
        testFile(MIMETYPE_JAVASCRIPT, "js", "script.js"),
        testFile(MIMETYPE_DITA, "dita", "quickConcept.dita"),
        testFile(MIMETYPE_IWORK_KEYNOTE, "key", "quick.key"),
        testFile(MIMETYPE_IWORK_NUMBERS, "number", "quick.numbers"),
        testFile(MIMETYPE_IWORK_PAGES, "pages", "quick.pages"),
        testFile(MIMETYPE_RFC822, "eml", "quick.eml")
    ).collect(toMap(FileInfo::getMimeType, identity()));
   
    public static Stream<SourceTarget> engineTransformations()
    {
        return Stream.of(
            SourceTarget.of("text/html", "text/plain"), //duplicate

            SourceTarget.of("text/plain", "text/plain"),
            SourceTarget.of("text/mediawiki", "text/plain"),
            SourceTarget.of("text/css", "text/plain"),
            SourceTarget.of("text/csv", "text/plain"),
            SourceTarget.of("text/xml", "text/plain"),
            SourceTarget.of("text/html", "text/plain"),
            SourceTarget.of("text/richtext", "text/plain"),
            SourceTarget.of("text/sgml", "text/plain"),
            SourceTarget.of("text/tab-separated-values", "text/plain"),
            SourceTarget.of("text/x-setext", "text/plain"),
            SourceTarget.of("text/x-java-source", "text/plain"),
            SourceTarget.of("text/x-jsp", "text/plain"),
            SourceTarget.of("text/x-markdown", "text/plain"),
            SourceTarget.of("text/calendar", "text/plain"),
            SourceTarget.of("application/x-javascript", "text/plain"),
            SourceTarget.of("application/dita+xml", "text/plain"),

            SourceTarget.of("application/vnd.apple.keynote", "image/jpeg"),
            SourceTarget.of("application/vnd.apple.numbers", "image/jpeg"),
            SourceTarget.of("application/vnd.apple.pages", "image/jpeg"),

            SourceTarget.of("text/plain", "application/pdf"),
            SourceTarget.of("text/csv", "application/pdf"),
            SourceTarget.of("application/dita+xml", "application/pdf"),
            SourceTarget.of("text/xml", "application/pdf"),

            SourceTarget.of("message/rfc822", "text/plain")
        );
    }

    @ParameterizedTest
    @MethodSource("engineTransformations")
    public void testTransformation(SourceTarget sourceTarget)
    {
        final String sourceMimetype = sourceTarget.source;
        final String targetMimetype = sourceTarget.target;
        final String sourceFile = TEST_FILES.get(sourceMimetype).getPath();
        final String targetExtension = TEST_FILES.get(targetMimetype).getExtension();

        final String descriptor = format("Transform ({0}, {1} -> {2}, {3})",
            sourceFile, sourceMimetype, targetMimetype, targetExtension);

        try
        {
            final ResponseEntity<Resource> response = sendTRequest(ENGINE_URL, sourceFile,
                sourceMimetype, targetMimetype, targetExtension);
            assertEquals(OK, response.getStatusCode(), descriptor);
        }
        catch (Exception e)
        {
            fail(descriptor + " exception: " + e.getMessage());
        }
    }
}
