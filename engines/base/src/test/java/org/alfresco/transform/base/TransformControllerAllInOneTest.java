/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2023 Alfresco Software Limited
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
package org.alfresco.transform.base;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.alfresco.transform.base.TransformControllerTest.assertConfig;
import static org.alfresco.transform.base.TransformControllerTest.getLogMessagesFor;
import static org.alfresco.transform.base.TransformControllerTest.resetProbeForTesting;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_ERROR;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_LIVE;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_LOG;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_READY;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_ROOT;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM_CONFIG;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM_CONFIG_LATEST;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_VERSION;
import static org.alfresco.transform.common.RequestParamMap.PAGE_REQUEST_PARAM;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transform.common.RequestParamMap.TARGET_MIMETYPE;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import org.alfresco.transform.base.fakes.FakeTransformEngineWithAllInOne;
import org.alfresco.transform.base.fakes.FakeTransformEngineWithOneCustomTransformer;
import org.alfresco.transform.base.fakes.FakeTransformEngineWithTwoCustomTransformers;
import org.alfresco.transform.base.fakes.FakeTransformerPdf2Jpg;
import org.alfresco.transform.base.fakes.FakeTransformerPdf2Png;
import org.alfresco.transform.base.fakes.FakeTransformerTxT2Pdf;

/**
 * Testing TransformController functionality where there are multiple TransformEngines brought together in a single t-engine.
 *
 * Repeats a set of tests from {@link TransformControllerTest}, which tests the single TransformEngine case.
 */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = {org.alfresco.transform.base.Application.class})
@ContextConfiguration(classes = {
        FakeTransformEngineWithAllInOne.class,
        FakeTransformEngineWithTwoCustomTransformers.class,
        FakeTransformerTxT2Pdf.class,
        FakeTransformerPdf2Png.class,
        FakeTransformEngineWithOneCustomTransformer.class,
        FakeTransformerPdf2Jpg.class})
public class TransformControllerAllInOneTest
{
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TransformController transformController;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    private String coreVersion;

    @Test
    public void testInitEngine()
    {
        assertEquals(FakeTransformEngineWithAllInOne.class.getSimpleName(),
                transformController.transformEngine.getClass().getSimpleName());
    }

    @Test
    public void testStartupLogsIncludeEngineMessages()
    {
        StringJoiner controllerLogMessages = getLogMessagesFor(TransformController.class);

        transformController.startup();

        assertEquals(
                "--------------------------------------------------------------------------------------------------------------------------------------------------------------\n"
                        + "Startup 0000 AllInOne\n"
                        + "Line 2 0000 AllInOne\n"
                        + "Line 3\n"
                        + "--------------------------------------------------------------------------------------------------------------------------------------------------------------\n"
                        + "Starting application components... Done",
                controllerLogMessages.toString());
    }

    @Test
    public void testVersionEndpointIncludesAvailable() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_VERSION))
                .andExpect(status().isOk())
                .andExpect(content().string("AllInOne " + coreVersion));
    }

    @Test
    public void testRootEndpointReturnsTestPage() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_ROOT))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AllInOne Test Page")));
    }

    @Test
    public void testErrorEndpointReturnsErrorPage() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_ERROR))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AllInOne Error Page")));
    }

    @Test
    public void testLogEndpointReturnsLogPage() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_LOG))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AllInOne Log Entries")));
    }

    @Test
    public void testReadyEndpointReturnsSuccessful() throws Exception
    {
        resetProbeForTesting(transformController);
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_READY))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Success - ")));
    }

    @Test
    public void testLiveEndpointReturnsSuccessful() throws Exception
    {
        resetProbeForTesting(transformController);
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_LIVE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Success - ")));
    }

    @Test
    public void testConfigEndpointReturnsOriginalConfigFormat() throws Exception
    {
        // The transformer's options should not include directAccessUrl as this is the default version of config
        assertConfig(ENDPOINT_TRANSFORM_CONFIG,
                "Pdf2Jpg,null,imageOptions\n"
                        + "Pdf2Png,null,imageOptions\n"
                        + "TxT2Pdf,null,docOptions\n"
                        + "Txt2JpgViaPdf,null,imageOptions\n"
                        + "Txt2PngViaPdf,null,imageOptions",
                "docOptions,imageOptions", mockMvc, objectMapper);
    }

    @Test
    public void testConfigLatestEndpointReturnsCoreVersionAndDirectAccessUrlOption() throws Exception
    {
        assertConfig(ENDPOINT_TRANSFORM_CONFIG_LATEST,
                "Pdf2Jpg," + coreVersion + ",directAccessUrl,imageOptions,sourceFilename\n"
                        + "Pdf2Png," + coreVersion + ",directAccessUrl,imageOptions,sourceFilename\n"
                        + "TxT2Pdf," + coreVersion + ",directAccessUrl,docOptions,sourceFilename\n"
                        + "Txt2JpgViaPdf," + coreVersion + ",directAccessUrl,imageOptions,sourceFilename\n"
                        + "Txt2PngViaPdf," + coreVersion + ",directAccessUrl,imageOptions,sourceFilename",
                "directAccessUrl,docOptions,imageOptions,sourceFilename", mockMvc, objectMapper);
    }

    @Test
    public void testTransformEndpointUsingTransformEngineWithTwoCustomTransformers() throws Exception
    {
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> mockMvc.perform(
                        MockMvcRequestBuilders.multipart(ENDPOINT_TRANSFORM)
                                .file(new MockMultipartFile("file", null, MIMETYPE_TEXT_PLAIN,
                                        "Start".getBytes(StandardCharsets.UTF_8)))
                                .param(SOURCE_MIMETYPE, MIMETYPE_TEXT_PLAIN)
                                .param(TARGET_MIMETYPE, MIMETYPE_PDF)
                                .param(PAGE_REQUEST_PARAM, "1"))
                        .andExpect(status().isOk())
                        .andExpect(header().string("Content-Disposition",
                                "attachment; filename*=UTF-8''transform.pdf"))
                        .andExpect(content().string("Start -> TxT2Pdf(page=1)")));
    }

    @Test
    public void testTransformEndpointUsingTransformEngineWithOneCustomTransformer() throws Exception
    {
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> mockMvc.perform(
                        MockMvcRequestBuilders.multipart(ENDPOINT_TRANSFORM)
                                .file(new MockMultipartFile("file", null, MIMETYPE_PDF,
                                        "Start".getBytes(StandardCharsets.UTF_8)))
                                .param(SOURCE_MIMETYPE, MIMETYPE_PDF)
                                .param(TARGET_MIMETYPE, MIMETYPE_IMAGE_JPEG))
                        .andExpect(status().isOk())
                        .andExpect(header().string("Content-Disposition",
                                "attachment; filename*=UTF-8''transform.jpeg"))
                        .andExpect(content().string("Start -> Pdf2Jpg()")));
    }
}
