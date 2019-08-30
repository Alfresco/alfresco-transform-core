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

import static org.alfresco.transformer.EngineClient.sendTRequest;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.OK;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

/**
 * @author Cezar Leahu
 */
public class TransformationIT
{
    private static final Logger logger = LoggerFactory.getLogger(TransformationIT.class);
    private static final String ENGINE_URL = "http://localhost:8090";

    @Test
    public void testPdfToPng()
    {
        checkTRequest("quick.pdf", "png");
    }

    @Test
    public void testAiToPng()
    {
        checkTRequest("quickCS3.ai", "png");

        checkTRequest("quickCS5.ai", "png");
    }

    private static void checkTRequest(final String sourceFile, final String targetExtension)
    {
        final ResponseEntity<Resource> response = sendTRequest(ENGINE_URL,
            sourceFile, null, null, targetExtension);

        logger.info("Response: {}", response);
        assertEquals(OK, response.getStatusCode());
    }
}
