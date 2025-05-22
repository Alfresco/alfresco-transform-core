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

import org.alfresco.transform.base.AbstractBaseTest;
import org.alfresco.transform.base.TransformController;
import org.alfresco.transform.config.TransformConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Files;
import java.util.StringJoiner;

import static org.alfresco.transform.base.TransformControllerTest.getLogMessagesFor;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.RequestParamMap.CONFIG_VERSION_DEFAULT;
import static org.alfresco.transform.common.RequestParamMap.CONFIG_VERSION_LATEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test All-In-One.
 */
public class AIOTest extends AbstractBaseTest
{
    @Autowired
    private String coreVersion;

    @BeforeEach
    public void before() throws Exception
    {
        sourceMimetype = MIMETYPE_HTML;
        targetMimetype = MIMETYPE_TEXT_PLAIN;
        sourceExtension = "html";
        targetExtension = "txt";
        expectedOptions = null;
        expectedSourceSuffix = null;
        sourceFileBytes = readTestFile(sourceExtension);
        expectedTargetFileBytes = Files.readAllBytes(getTestFile("quick3." + targetExtension, true).toPath());
        sourceFile = new MockMultipartFile("file", "quick." + sourceExtension, sourceMimetype, sourceFileBytes);
    }

    @Override
    // Add extra required parameters to the request.
    protected MockHttpServletRequestBuilder mockMvcRequest(String url, MockMultipartFile sourceFile, String... params)
    {
        return super.mockMvcRequest(url, sourceFile, params)
                    .param("targetMimetype", targetMimetype)
                    .param("sourceMimetype", sourceMimetype);
    }

    @Test
    public void coreVersionNotSetInOriginalConfigTest()
    {
        ResponseEntity<TransformConfig> responseEntity = controller.transformConfig(Integer.valueOf(CONFIG_VERSION_DEFAULT));
        responseEntity.getBody().getTransformers().forEach(transformer -> {
            assertNull(transformer.getCoreVersion(), transformer.getTransformerName() +
                    " should have had a null coreValue but was " + transformer.getCoreVersion());
        });
    }

    @Test
    public void coreVersionSetInLatestConfigTest()
    {
        ResponseEntity<TransformConfig> responseEntity = controller.transformConfig(CONFIG_VERSION_LATEST);
        responseEntity.getBody().getTransformers().forEach(transformer -> {
            assertNotNull(transformer.getCoreVersion(), transformer.getTransformerName() +
                    " should have had a coreValue but was null. Should have been " + coreVersion);
        });
    }

    @Test
    public void testStartupLogsIncludeEngineMessages()
    {
        StringJoiner controllerLogMessages = getLogMessagesFor(TransformController.class);

        controller.startup();

        assertEquals(
            "--------------------------------------------------------------------------------------------------------------------------------------------------------------\n"
                + "If the Alfresco software was purchased under a paid Alfresco license, the terms of the paid license agreement \n"
                + "will prevail. Otherwise, the software is provided under terms of the GNU LGPL v3 license. \n"
                + "See the license at http://www.gnu.org/licenses/lgpl-3.0.txt. or in /LICENSE.txt \n"
                + "\n"
                + "This transformer uses ImageMagick from ImageMagick Studio LLC. See the license at http://www.imagemagick.org/script/license.php or in /ImageMagick-license.txt\n"
                + "This transformer uses LibreOffice from The Document Foundation. See the license at https://www.libreoffice.org/download/license/ or in /libreoffice.txt\n"
                + "This transformer uses libraries from Apache. See the license at http://www.apache.org/licenses/LICENSE-2.0. or in /Apache\\\\ 2.0.txt\n"
                + "This transformer uses htmlparser. See the license at http://htmlparser.sourceforge.net/license.html\n"
                + "This transformer uses alfresco-pdf-renderer which uses the PDFium library from Google Inc. See the license at https://pdfium.googlesource.com/pdfium/+/master/LICENSE or in /pdfium.txt\n"
                + "This transformer uses Tika from Apache. See the license at http://www.apache.org/licenses/LICENSE-2.0. or in /Apache\\ 2.0.txt\n"
                + "This transformer uses ExifTool by Phil Harvey. See license at https://exiftool.org/#license. or in /Perl-Artistic-License.txt\n"
                + "--------------------------------------------------------------------------------------------------------------------------------------------------------------\n"
                + "Starting application components... Done",
            controllerLogMessages.toString());
    }
}