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
package org.alfresco.transform.base;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.alfresco.transform.base.fakes.FakeTransformEngineWithTwoCustomTransformers;
import org.alfresco.transform.base.fakes.FakeTransformerPdf2Png;
import org.alfresco.transform.base.fakes.FakeTransformerTxT2Pdf;
import org.alfresco.transform.config.TransformConfig;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_BMP;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_ERROR;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_LIVE;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_LOG;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_READY;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_ROOT;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TEST;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM_CONFIG;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM_CONFIG_LATEST;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_VERSION;
import static org.alfresco.transform.common.RequestParamMap.PAGE_REQUEST_PARAM;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_ENCODING;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transform.common.RequestParamMap.TARGET_MIMETYPE;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testing base t-engine TransformController functionality.
 *
 * Also see {@link TransformControllerAllInOneTest}.
 */
@AutoConfigureMockMvc
@SpringBootTest(classes={org.alfresco.transform.base.Application.class})
@ContextConfiguration(classes = {
        FakeTransformEngineWithTwoCustomTransformers.class,
        FakeTransformerTxT2Pdf.class,
        FakeTransformerPdf2Png.class})
public class TransformControllerTest
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
    public void testInitEngine() throws Exception
    {
        assertEquals(FakeTransformEngineWithTwoCustomTransformers.class.getSimpleName(),
                transformController.transformEngine.getClass().getSimpleName());
        assertNotNull(transformController.probeTransform);
    }

    @Test
    public void testStartupLogsIncludeEngineMessages()
    {
        StringJoiner controllerLogMessages = getLogMessagesFor(TransformController.class);

        transformController.startup();

        assertEquals(
            "--------------------------------------------------------------------------------------------------------------------------------------------------------------\n"
             + "Startup TwoCustomTransformers\n"
             + "Line 2 TwoCustomTransformers\n"
             + "Line 3\n"
             + "--------------------------------------------------------------------------------------------------------------------------------------------------------------\n"
             + "Starting application components... Done",
            controllerLogMessages.toString());
    }

    static StringJoiner getLogMessagesFor(Class classBeingLogged)
    {
        StringJoiner logMessages = new StringJoiner("\n");
        Logger logger = (Logger) LoggerFactory.getLogger(classBeingLogged);
        AppenderBase<ILoggingEvent> logAppender = new AppenderBase<>()
        {
            @Override
            protected void append(ILoggingEvent iLoggingEvent)
            {
                logMessages.add(iLoggingEvent.getMessage());
            }
        };
        logAppender.setContext((LoggerContext)LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.DEBUG);
        logger.addAppender(logAppender);
        logAppender.start();

        return logMessages;
    }

    @Test
    public void testVersionEndpointIncludesAvailable() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_VERSION))
               .andExpect(status().isOk())
               .andExpect(content().string("TwoCustomTransformers "+coreVersion+" available"));
    }

    @Test
    public void testRootEndpointReturnsTestPage() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_ROOT))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("TwoCustomTransformers Test Page")));
    }

    @Test
    public void testErrorEndpointReturnsErrorPage() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_ERROR))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("TwoCustomTransformers Error Page")));
    }

    @Test
    public void testLogEndpointReturnsLogPage() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_LOG))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("TwoCustomTransformers Log Entries")));
    }

    @Test
    public void testReadyEndpointReturnsSuccessful() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_READY))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Success - ")));
    }

    @Test
    public void testLiveEndpointReturnsSuccessful() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_LIVE))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Success - ")));
    }

    @Test
    public void testConfigEndpointReturnsOriginalConfigFormat() throws Exception
    {
        // Includes Txt2PngViaPdf as Pdf2Jpg might exist in another t-engine
        // coreValue is not set as this is the default version of config
        // The transformer's options should not include directAccessUrl as this is the default version of config
        assertConfig(ENDPOINT_TRANSFORM_CONFIG,
            "Pdf2Png,null,imageOptions\n"
                + "TxT2Pdf,null,docOptions\n"
                + "Txt2JpgViaPdf,null,imageOptions\n"
                + "Txt2PngViaPdf,null,imageOptions",
            "docOptions,imageOptions", mockMvc, objectMapper);
    }

    @Test
    public void testConfigLatestEndpointReturnsCoreVersionAndDirectAccessUrlOption() throws Exception
    {
        assertConfig(ENDPOINT_TRANSFORM_CONFIG_LATEST,
            "Pdf2Png,"+coreVersion+",directAccessUrl,imageOptions\n"
                + "TxT2Pdf,"+coreVersion+",directAccessUrl,docOptions\n"
                + "Txt2JpgViaPdf,null,imageOptions\n"
                + "Txt2PngViaPdf,"+coreVersion+",directAccessUrl,imageOptions",
            "directAccessUrl,docOptions,imageOptions", mockMvc, objectMapper);
    }

    static void assertConfig(String url, String expectedTransformers, String expectedOptions,
        MockMvc mockMvc, ObjectMapper objectMapper) throws Exception
    {
        TransformConfig config = objectMapper.readValue(
            mockMvc.perform(MockMvcRequestBuilders.get(url))
                   .andExpect(status().isOk())
                   .andReturn()
                   .getResponse()
                   .getContentAsString(), TransformConfig.class);

        // Gets a list of transformerNames,coreVersion,optionNames
        assertEquals(expectedTransformers,
            config.getTransformers().stream()
                  .map(t -> t.getTransformerName()+","
                            +t.getCoreVersion()+","
                            +t.getTransformOptions().stream().sorted().collect(Collectors.joining(",")))
                  .sorted()
                  .collect(Collectors.joining("\n")));

        assertEquals(expectedOptions,
            config.getTransformOptions().keySet().stream()
                  .sorted()
                  .collect(Collectors.joining(",")));
    }

    @Test
    public void testTransformEndpoint() throws Exception
    {
        MvcResult mvcResult = mockMvc.perform(
            MockMvcRequestBuilders.multipart(ENDPOINT_TRANSFORM)
                .file(new MockMultipartFile("file", null, MIMETYPE_TEXT_PLAIN,
                    "Start".getBytes(StandardCharsets.UTF_8)))
                .param(SOURCE_MIMETYPE, MIMETYPE_TEXT_PLAIN)
                .param(TARGET_MIMETYPE, MIMETYPE_PDF)
                .param(PAGE_REQUEST_PARAM, "1"))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
            "attachment; filename*=UTF-8''transform.pdf"))
            .andExpect(content().string("Start -> TxT2Pdf(page=1)"));
    }

    @Test
    public void testTestTransformEndpointConvertsRequestParameters() throws Exception
    {
        TransformHandler orig = transformController.transformHandler;
        try
        {
            TransformHandler spy = spy(orig);
            transformController.transformHandler = spy;

            MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.multipart(ENDPOINT_TEST)
                    .file(new MockMultipartFile("file", null, MIMETYPE_TEXT_PLAIN,
                        "Start".getBytes(StandardCharsets.UTF_8)))
                    .param(SOURCE_MIMETYPE, MIMETYPE_IMAGE_BMP)
                    .param("_"+SOURCE_MIMETYPE, MIMETYPE_TEXT_PLAIN)
                    .param(TARGET_MIMETYPE, MIMETYPE_PDF)
                    .param("_"+TARGET_MIMETYPE, "")
                    .param(PAGE_REQUEST_PARAM, "replaced")
                    .param("name1", "hasNoValueSoRemoved").param("value1", "")
                    .param("name2", PAGE_REQUEST_PARAM).param("value2", "1")
                    .param("name3", SOURCE_ENCODING).param("value3", "UTF-8"))
                .andExpect(request().asyncStarted())
                .andReturn();

            // Do the dispatch, just in case not doing it leaves it in a strange state.
            mockMvc.perform(asyncDispatch(mvcResult));

            verify(spy).handleHttpRequest(any(), any(), eq(MIMETYPE_TEXT_PLAIN), eq(MIMETYPE_PDF),
                eq(ImmutableMap.of(
                    SOURCE_MIMETYPE, MIMETYPE_TEXT_PLAIN,
                    TARGET_MIMETYPE, MIMETYPE_PDF,
                    PAGE_REQUEST_PARAM, "1",
                    SOURCE_ENCODING, "UTF-8")));
        }
        finally
        {
            transformController.transformHandler = orig;
        }
    }

    @Test
    public void testInterceptOfMissingServletRequestParameterException() throws Exception
    {
        mockMvc.perform(
               MockMvcRequestBuilders.multipart(ENDPOINT_TRANSFORM)
                   .file(new MockMultipartFile("file", null, MIMETYPE_TEXT_PLAIN,
                       "Start".getBytes(StandardCharsets.UTF_8))))
               .andExpect(status().isBadRequest())
               .andExpect(status().reason(containsString("Request parameter '"+SOURCE_MIMETYPE+"' is missing")));
    }

    @Test
    public void testInterceptOfTransformException_noTransformers() throws Exception
    {
        MvcResult mvcResult = mockMvc.perform(
            MockMvcRequestBuilders.multipart(ENDPOINT_TRANSFORM)
               .file(new MockMultipartFile("file", null, MIMETYPE_TEXT_PLAIN,
                    "Start".getBytes(StandardCharsets.UTF_8)))
               .param(SOURCE_MIMETYPE, MIMETYPE_TEXT_PLAIN)
               .param(TARGET_MIMETYPE, MIMETYPE_PDF)
               .param("unknown", "1"))
               .andExpect(request().asyncStarted())
               .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
               .andExpect(status().isBadRequest())
               .andExpect(content().string(containsString("TwoCustomTransformers Error Page")))
               .andExpect(content().string(containsString("No transforms were able to handle the request")));
    }
}
