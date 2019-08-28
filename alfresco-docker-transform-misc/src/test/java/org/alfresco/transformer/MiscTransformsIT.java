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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_DITA;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_EXCEL;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IMAGE_GIF;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IMAGE_TIFF;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IWORK_KEYNOTE;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IWORK_NUMBERS;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IWORK_PAGES;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_JAVASCRIPT;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_PRESENTATION;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_SPREADSHEET;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_WORDPROCESSING;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OUTLOOK_MSG;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_PPT;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_RFC822;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_TEXT_CSS;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_TEXT_CSV;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_TEXT_JAVASCRIPT;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_TEXT_MEDIAWIKI;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_WORD;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_XML;
import static org.alfresco.transformer.MiscTransformsIT.TestFileInfo.testFile;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * @author Cezar Leahu
 */
@RunWith(Parameterized.class)
public class MiscTransformsIT
{
    private static final Logger logger = LoggerFactory.getLogger(MiscTransformsIT.class);
    private static final String ENGINE_URL = "http://localhost:8090";

    private static final Map<String, TestFileInfo> TEST_FILES = Stream.of(
        testFile(MIMETYPE_IMAGE_GIF, "gif", "quick.gif", true),
        testFile(MIMETYPE_IMAGE_JPEG, "jpg", "quick.jpg", true),
        testFile(MIMETYPE_IMAGE_PNG, "png", "quick.png", true),
        testFile(MIMETYPE_IMAGE_TIFF, "tiff", "quick.tiff", true),
        testFile(MIMETYPE_WORD, "doc", "quick.doc", true),
        testFile(MIMETYPE_OPENXML_WORDPROCESSING, "docx", "quick.docx", true),
        testFile(MIMETYPE_EXCEL, "xls", "quick.xls", true),
        testFile(MIMETYPE_OPENXML_SPREADSHEET, "xlsx", "quick.xlsx", true),
        testFile(MIMETYPE_PPT, "ppt", "quick.ppt", true),
        testFile(MIMETYPE_OPENXML_PRESENTATION, "pptx", "quick.pptx", true),
        testFile(MIMETYPE_OUTLOOK_MSG, "msg", "quick.msg", true),
        testFile(MIMETYPE_PDF, "pdf", "quick.pdf", true),
        testFile(MIMETYPE_TEXT_PLAIN, "txt", "quick.txt", true),

        testFile(MIMETYPE_TEXT_MEDIAWIKI, "mw", "sample.mw", false),
        testFile(MIMETYPE_TEXT_CSS, "css", "style.css", false),
        testFile(MIMETYPE_TEXT_CSV, "csv", "people.csv", false),
        testFile(MIMETYPE_TEXT_JAVASCRIPT, "js", "script.js", false),
        testFile(MIMETYPE_XML, "xml", "quick.xml", true),
        testFile(MIMETYPE_HTML, "html", "quick.html", true),
        testFile(MIMETYPE_JAVASCRIPT, "js", "script.js", false),
        testFile(MIMETYPE_DITA, "dita", "quickConcept.dita", false),
        testFile(MIMETYPE_IWORK_KEYNOTE, "key", "quick.key", false),
        testFile(MIMETYPE_IWORK_NUMBERS, "number", "quick.numbers", false),
        testFile(MIMETYPE_IWORK_PAGES, "pages", "quick.pages", false),
        testFile(MIMETYPE_RFC822, "eml", "quick.eml", false)
    ).collect(toMap(TestFileInfo::getMimeType, identity()));

    private final String sourceMimetype;
    private final String targetMimetype;

    public MiscTransformsIT(final SourceTarget sourceTarget)
    {
        sourceMimetype = sourceTarget.source;
        targetMimetype = sourceTarget.target;
    }

    @Parameterized.Parameters
    public static Set<SourceTarget> engineTransformations()
    {
        return Stream.of(
            SourceTarget.of("text/html", "text/plain"), //duplicate
            SourceTarget.of("text/plain", "text/plain"),
            SourceTarget.of("text/mediawiki", "text/plain"),
            SourceTarget.of("text/css", "text/plain"),
            SourceTarget.of("text/csv", "text/plain"),
            SourceTarget.of("text/javascript", "text/plain"),
            SourceTarget.of("text/xml", "text/plain"),
            SourceTarget.of("text/html", "text/plain"),
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
        ).collect(toSet());
    }

    @Test
    public void testTransformation()
    {
        final TestFileInfo sourceFile = TEST_FILES.get(sourceMimetype);
        final TestFileInfo targetFile = TEST_FILES.get(targetMimetype);
        assertNotNull(sourceFile);
        assertNotNull(targetFile);

        final ResponseEntity<Resource> response = sendTRequest(sourceFile.path,
            sourceMimetype, targetMimetype, targetFile.extension);

        logger.info("Response: {}", response);

        final int status = response.getStatusCode().value();
        assertTrue("Transformation failed", status >= 200 && status < 300);
    }

    private static ResponseEntity<Resource> sendTRequest(final String sourceFile,
        final String sourceMimetype, final String targetMimetype, final String targetExtension)
    {
        final RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MULTIPART_FORM_DATA);
        //headers.setAccept(ImmutableList.of(MULTIPART_FORM_DATA));

        final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ClassPathResource(sourceFile));
        body.add("targetExtension", targetExtension);
        body.add("targetMimetype", targetMimetype);
        body.add("sourceMimetype", sourceMimetype);

        final HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        return restTemplate.postForEntity(ENGINE_URL + "/transform", entity, Resource.class);
    }

    static class TestFileInfo
    {
        private final String mimeType;
        private final String extension;
        private final String path;
        private final boolean exactMimeType;

        public TestFileInfo(final String mimeType, final String extension, final String path,
            final boolean exactMimeType)
        {
            this.mimeType = mimeType;
            this.extension = extension;
            this.path = path;
            this.exactMimeType = exactMimeType;
        }

        public String getMimeType()
        {
            return mimeType;
        }

        public String getExtension()
        {
            return extension;
        }

        public String getPath()
        {
            return path;
        }

        public boolean isExactMimeType()
        {
            return exactMimeType;
        }

        public static TestFileInfo testFile(final String mimeType, final String extension,
            final String path, final boolean exactMimeType)
        {
            return new TestFileInfo(mimeType, extension, path, exactMimeType);
        }
    }

    public static class SourceTarget
    {
        final String source;
        final String target;

        private SourceTarget(final String source, final String target)
        {
            this.source = source;
            this.target = target;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SourceTarget that = (SourceTarget) o;
            return Objects.equals(source, that.source) &&
                   Objects.equals(target, that.target);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(source, target);
        }

        @Override
        public String toString()
        {
            return source + '|' + target;
        }

        public static SourceTarget of(final String source, final String target)
        {
            return new SourceTarget(source, target);
        }
    }
}
