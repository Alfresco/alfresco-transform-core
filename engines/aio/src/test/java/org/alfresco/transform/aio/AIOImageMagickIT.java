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
package org.alfresco.transform.aio;

import static java.text.MessageFormat.format;

import static org.alfresco.transform.base.clients.HttpClient.sendTRequest;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_TIFF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.alfresco.transform.imagemagick.ImageMagickTransformationIT;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public class AIOImageMagickIT extends ImageMagickTransformationIT
{

    @Test
    void testTransformTiffToPdf() throws IOException
    {
        final String sourceFile = "quick.tiff";
        final String targetExtension = "pdf";
        final String sourceMimetype = MIMETYPE_IMAGE_TIFF;
        final String targetMimetype = MIMETYPE_PDF;
        final Map<String, String> tOptions = ImmutableMap.of("startPage", "0", "endPage", "0");

        // when
        final ResponseEntity<Resource> response = sendTRequest("http://localhost:8090", sourceFile, sourceMimetype,
            targetMimetype, targetExtension, tOptions);

        assertEquals(OK, response.getStatusCode());
        final PDDocument pdfFile = PDDocument.load(response.getBody().getInputStream());
        assertNotNull(pdfFile);
    }
}
