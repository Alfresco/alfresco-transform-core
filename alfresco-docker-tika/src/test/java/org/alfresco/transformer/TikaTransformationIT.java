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
    private final String transform;

    public TikaTransformationIT(final Triple<String, String, String> entry)
    {
        sourceFile = entry.getLeft();
        targetExtension = entry.getMiddle();
        targetMimetype = extensionMimetype.get(entry.getMiddle());
        transform = entry.getRight();
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
                allTargets("quick.doc", "Office"),
                allTargets("quick.docx", "TikaAuto"),
                allTargets("quick.html", "TikaAuto"),
                allTargets("quick.jar", "TikaAuto"),
                allTargets("quick.java", "TikaAuto"),
                Stream.of(
                    Triple.of("quick.key", "html", "TikaAuto"),
                    // Does not work, alfresco-docker-transform-misc can handle this target mimetype, removed from engine_config.json
                    // Triple.of("quick.key", "txt", "TikaAuto"),
                    Triple.of("quick.key", "xhtml", "TikaAuto"),
                    Triple.of("quick.key", "xml", "TikaAuto")
                ),
                allTargets("quick.msg", "OutlookMsg"),
                Stream.of(
                    Triple.of("quick.numbers", "html", "TikaAuto"),
                    // Does not work, alfresco-docker-transform-misc can handle this target mimetype, removed from engine_config.json
                    // Triple.of("quick.numbers", "txt", "TikaAuto"),
                    Triple.of("quick.numbers", "xhtml", "TikaAuto"),
                    Triple.of("quick.numbers", "xml", "TikaAuto")
                ),
                allTargets("quick.odp", "TikaAuto"),
                allTargets("quick.ods", "TikaAuto"),
                allTargets("quick.odt", "TikaAuto"),
                allTargets("quick.otp", "TikaAuto"),
                allTargets("quick.ots", "TikaAuto"),
                allTargets("quick.ott", "TikaAuto"),
                Stream.of(
                    Triple.of("quick.pages", "html", "TikaAuto"),
                    // Does not work, alfresco-docker-transform-misc can handle this target mimetype, removed from engine_config.json
                    // Triple.of("quick.pages", "txt", "TikaAuto"),
                    Triple.of("quick.pages", "xhtml", "TikaAuto"),
                    Triple.of("quick.pages", "xml", "TikaAuto")
                ),
                allTargets("quick.pdf", "TikaAuto"),
                allTargets("quick.ppt", "TikaAuto"),
                allTargets("quick.pptx", "TikaAuto"),
                allTargets("quick.sxw", "TikaAuto"),
                allTargets("quick.txt", "TikaAuto"),
                allTargets("quick.vsd", "TikaAuto"),
                allTargets("quick.xls", "TikaAuto"),
                allTargets("quick.xslx", "TikaAuto"),
                allTargets("quick.zip", "TikaAuto"),
                allTargets("quick.zip", "Archive"),
                allTargets("quick.jar", "Archive"),
                allTargets("quick.tar", "Archive"),
                allTargets("sample.rtf", "TikaAuto"),
                allTargets("quick.xml", "TikaAuto"),
                allTargets("sample.xhtml.txt", "TikaAuto"),
                allTargets("sample.rss", "TikaAuto"),
                //allTargets("quick.rar", "TikaAuto"),
                allTargets("quick.tar.gz", "TikaAuto"))
            .flatMap(identity())
            .collect(toSet());
    }

    @Test
    public void testTransformation()
    {
        final String descriptor = format("Transform ({0} -> {1}, {2}, transform={3})",
            sourceFile, targetMimetype, targetExtension, transform);

        try
        {
            final ResponseEntity<Resource> response = sendTRequest(ENGINE_URL, sourceFile, null,
                targetMimetype, targetExtension, ImmutableMap.of(
                    "targetEncoding", "UTF-8",
                    "transform", transform));
            assertEquals(descriptor, OK, response.getStatusCode());
        }
        catch (Exception e)
        {
            fail(descriptor + " exception: " + e.getMessage());
        }
    }

    private static Stream<Triple<String, String, String>> allTargets(final String sourceFile,
        final String transform)
    {
        return extensionMimetype
            .keySet()
            .stream()
            .map(k -> Triple.of(sourceFile, k, transform));
    }
}
