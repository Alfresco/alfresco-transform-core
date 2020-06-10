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

import static java.text.MessageFormat.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;
import static org.alfresco.transformer.EngineClient.sendTRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.http.HttpStatus.OK;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.ImmutableMap;

/**
 * @author Cezar Leahu
 */
@RunWith(Parameterized.class)
public class TikaTransformationIT
{
    private static final Logger logger = LoggerFactory.getLogger(TikaTransformationIT.class);
    private static final String ENGINE_URL = "http://localhost:8090";
    private static final Map<String, String> extensionMimetype = ImmutableMap.of(
        "html", "text/html",
        "txt", "text/plain",
        "xhtml", "application/xhtml+xml",
        "xml", "text/xml");

    private final String sourceFile;
    private final String targetExtension;
    private final String targetMimetype;
    private final String sourceMimetype;

    public TikaTransformationIT(final Triple<String, String, String> entry)
    {
        sourceFile = entry.getLeft();
        targetExtension = entry.getMiddle();
        //Single test to cover csv-->pdf
        if (sourceFile.contains("pdf") && targetExtension.contains("csv"))
        {
            targetMimetype = "text/csv";
        }
        else
        {
            targetMimetype = extensionMimetype.get(entry.getMiddle());
        }
        sourceMimetype = entry.getRight();
    }

    // TODO unit tests for the following file types (for which is difficult to find file samples):
    //  *.ogx (application/ogg)
    //  *.cpio (application/x-cpio)
    //  *.cdf (application/x-netcdf) 
    //  *.hdf (application/x-hdf)

    @Parameterized.Parameters
    public static Set<Triple<String, String, String>> engineTransformations()
    {
        return Stream
            .of(
                allTargets("quick.doc", "application/msword"),
                allTargets("quick.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                allTargets("quick.html", "text/html"),
                allTargets("quick.jar", "application/java-archive"),
                allTargets("quick.java", "text/x-java-source"),
                Stream.of(
                    Triple.of("quick.key", "html", "application/vnd.apple.keynote"),
                    // Does not work, alfresco-docker-sourceMimetype-misc can handle this target mimetype, removed from engine_config.json
                    // Triple.of("quick.key", "txt", "TikaAuto"),
                    Triple.of("quick.key", "xhtml", "application/vnd.apple.keynote"),
                    Triple.of("quick.key", "xml", "application/vnd.apple.keynote")
                ),
                allTargets("quick.msg", "application/vnd.ms-outlook"),
                Stream.of(
                    Triple.of("quick.numbers", "html", "application/vnd.apple.numbers"),
                    // Does not work, alfresco-docker-sourceMimetype-misc can handle this target mimetype, removed from engine_config.json
                    // Triple.of("quick.numbers", "txt", "TikaAuto"),
                    Triple.of("quick.numbers", "xhtml", "application/vnd.apple.numbers"),
                    Triple.of("quick.numbers", "xml", "application/vnd.apple.numbers")
                ),
                Stream.of(
                    Triple.of("quick.pdf", "csv", "application/pdf")
                ),
                allTargets("quick.odp", "application/vnd.oasis.opendocument.presentation"),
                allTargets("quick.ods", "application/vnd.oasis.opendocument.spreadsheet"),
                allTargets("quick.odt", "application/vnd.oasis.opendocument.text"),
                allTargets("quick.otp", "application/vnd.oasis.opendocument.presentation-template"),
                allTargets("quick.ots", "application/vnd.oasis.opendocument.spreadsheet-template"),
                allTargets("quick.ott", "application/vnd.oasis.opendocument.text-template"),
                Stream.of(
                    Triple.of("quick.pages", "html", "application/vnd.apple.pages"),
                    // Does not work, alfresco-docker-sourceMimetype-misc can handle this target mimetype, removed from engine_config.json
                    // Triple.of("quick.pages", "txt", "TikaAuto"),
                    Triple.of("quick.pages", "xhtml", "application/vnd.apple.pages"),
                    Triple.of("quick.pages", "xml", "application/vnd.apple.pages")
                ),
                allTargets("quick.pdf", "application/pdf"),
                allTargets("quick.ppt", "application/vnd.ms-powerpoint"),
                allTargets("quick.pptx",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
                allTargets("quick.sxw", "application/vnd.sun.xml.writer"),
                allTargets("quick.txt", "text/plain"),
                allTargets("quick.vsd", "application/vnd.visio"),
                allTargets("quick.xls", "application/vnd.ms-excel"),
                allTargets("quick.xslx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                allTargets("quick.zip", "application/zip"),
                allTargets("quick.tar", "application/x-tar"),
                allTargets("sample.rtf", "application/rtf"),
                allTargets("quick.xml", "text/xml"),
                allTargets("sample.xhtml.txt", "application/xhtml+xml"),
                allTargets("sample.rss", "application/rss+xml"),
                //allTargets("quick.rar", "application/x-rar-compressed"),
                allTargets("quick.z", "application/x-compress"),
                allTargets("quick.csv", "text/csv"),
                allTargets("quick.tar.gz", "application/x-gzip"))
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
            final ResponseEntity<Resource> response = sendTRequest(ENGINE_URL, sourceFile, null,
                targetMimetype, targetExtension, ImmutableMap.of(
                    "targetEncoding", "UTF-8",
                    "sourceMimetype", sourceMimetype));
            assertEquals(descriptor, OK, response.getStatusCode());
        }
        catch (Exception e)
        {
            fail(descriptor + " exception: " + e.getMessage());
        }
    }

    private static Stream<Triple<String, String, String>> allTargets(final String sourceFile,
        final String sourceMimetype)
    {
        return extensionMimetype
            .keySet()
            .stream()
            .map(k -> Triple.of(sourceFile, k, sourceMimetype));
    }
}
