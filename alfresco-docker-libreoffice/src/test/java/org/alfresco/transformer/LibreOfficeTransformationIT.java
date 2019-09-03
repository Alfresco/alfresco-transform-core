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

import java.util.Set;
import java.util.stream.Stream;

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
    private static final Set<String> spreadsheetTargetMimetypes = ImmutableSet.of(
        "csv", "html", "ods", "pdf", "tsv", "xls");
    private static final Set<String> documentsTargetMimetypes = ImmutableSet.of(
        "doc", "html", "odt", "pdf", "rtf");
    private static final Set<String> graphicTargetMimetypes = ImmutableSet.of(
        "pdf", "svg");
    private static final Set<String> presentationTargetMimetypes = ImmutableSet.of(
        "html", "odp", "ppt", "pdf");

    private final String sourceFile;
    private final String targetExtension;

    public LibreOfficeTransformationIT(final Pair<String, String> entry)
    {
        sourceFile = entry.getKey();
        targetExtension = entry.getRight();
    }

    @Parameterized.Parameters
    public static Set<Pair<String, String>> engineTransformations()
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
        final String descriptor = format("Transform ({0} -> {1})", sourceFile, targetExtension);

        try
        {
            final ResponseEntity<Resource> response = sendTRequest(ENGINE_URL, sourceFile, null,
                null, targetExtension);
            assertEquals(descriptor, OK, response.getStatusCode());
        }
        catch (Exception e)
        {
            fail(descriptor + " exception: " + e.getMessage());
        }
    }

    private static Stream<Pair<String, String>> allTargets(final String sourceFile,
        final Set<String> mimetypes)
    {
        return mimetypes
            .stream()
            .map(k -> Pair.of(sourceFile, k));
    }
}
