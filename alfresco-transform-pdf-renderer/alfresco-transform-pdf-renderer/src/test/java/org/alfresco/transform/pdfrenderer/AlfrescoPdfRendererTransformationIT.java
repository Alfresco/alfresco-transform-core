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
package org.alfresco.transform.pdfrenderer;

import static java.text.MessageFormat.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.alfresco.transformer.EngineClient.sendTRequest;
import static org.alfresco.transformer.TestFileInfo.testFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.HttpStatus.OK;

import java.util.Map;
import java.util.stream.Stream;

import org.alfresco.transformer.TestFileInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

/**
 * @author Cezar Leahu
 */
public class AlfrescoPdfRendererTransformationIT
{
    private static final Logger logger = LoggerFactory.getLogger(
        AlfrescoPdfRendererTransformationIT.class);
    private static final String ENGINE_URL = "http://localhost:8090";

    private static final Map<String, TestFileInfo> TEST_FILES = Stream.of(
        testFile("application/pdf","pdf","quick.pdf"),
        testFile("application/illustrator","ai","quickCS3.ai")  ,      
        testFile("application/illustrator","ai","quickCS5.ai")
    ).collect(toMap(TestFileInfo::getPath, identity()));

    public static Stream<String> engineTransformations()
    {
        return Stream.of(
            "quick.pdf",
            "quickCS3.ai",
            "quickCS5.ai"
        );
    }

    @ParameterizedTest
    @MethodSource("engineTransformations")
    public void testTransformation(String sourceFile)
    {
        final String sourceMimetype = TEST_FILES.get(sourceFile).getMimeType();

        final String descriptor = format("Transform ({0}, {1} -> {2}, {3})",
            sourceFile, sourceMimetype, "image/png", "png");

        try
        {
            final ResponseEntity<Resource> response = sendTRequest(ENGINE_URL, sourceFile, sourceMimetype,
                "image/png", "png");
            assertEquals(OK, response.getStatusCode(),descriptor);
        }
        catch (Exception e)
        {
            fail(descriptor + " exception: " + e.getMessage());
        }
    }
}
