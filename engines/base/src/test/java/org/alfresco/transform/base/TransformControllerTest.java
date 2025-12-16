/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2022 Alfresco Software Limited
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.alfresco.transform.base.AbstractBaseTest.getTestFile;
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import org.alfresco.transform.base.fakes.FakeTransformEngineWithTwoCustomTransformers;
import org.alfresco.transform.base.fakes.FakeTransformerPdf2Png;
import org.alfresco.transform.base.fakes.FakeTransformerTxT2Pdf;
import org.alfresco.transform.base.model.FileRefEntity;
import org.alfresco.transform.base.model.FileRefResponse;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.base.sfs.SharedFileStoreClient;
import org.alfresco.transform.base.transform.TransformHandler;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.config.TransformConfig;

/**
 * Tests the endpoints of the TransformController.
 *
 * Also see {@link TransformControllerAllInOneTest}.
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = {org.alfresco.transform.base.Application.class})
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
    @TempDir
    public File tempDir;
    @MockitoBean
    protected SharedFileStoreClient fakeSfsClient;

    static void resetProbeForTesting(TransformController transformController)
    {
        AbstractBaseTest.resetProbeForTesting(transformController.getProbeTransform());
    }

    @Test
    public void testInitEngine() throws Exception
    {
        assertEquals(FakeTransformEngineWithTwoCustomTransformers.class.getSimpleName(),
                transformController.transformEngine.getClass().getSimpleName());
    }

    @Test
    public void testStartupLogsIncludeEngineMessages()
    {
        StringJoiner controllerLogMessages = getLogMessagesFor(TransformController.class);

        transformController.startup();

        assertEquals(
                "--------------------------------------------------------------------------------------------------------------------------------------------------------------\n"
                        + "Startup 0000 TwoCustomTransformers\n"
                        + "Line 2 0000 TwoCustomTransformers\n"
                        + "Line 3\n"
                        + "--------------------------------------------------------------------------------------------------------------------------------------------------------------\n"
                        + "Starting application components... Done",
                controllerLogMessages.toString());
    }

    public static StringJoiner getLogMessagesFor(Class classBeingLogged)
    {
        StringJoiner logMessages = new StringJoiner("\n");
        Logger logger = (Logger) LoggerFactory.getLogger(classBeingLogged);
        AppenderBase<ILoggingEvent> logAppender = new AppenderBase<>() {
            @Override
            protected void append(ILoggingEvent iLoggingEvent)
            {
                logMessages.add(iLoggingEvent.getMessage());
            }
        };
        logAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.DEBUG);
        logger.addAppender(logAppender);
        logAppender.start();

        return logMessages;
    }

    private void testPageWithOrWithoutIngresPrefix(String url, boolean behindIngres, String... expected) throws Exception
    {
        boolean origBehindIngres = (boolean) ReflectionTestUtils.getField(transformController, "behindIngres");
        try
        {
            ReflectionTestUtils.setField(transformController, "behindIngres", behindIngres);

            mockMvc.perform(MockMvcRequestBuilders.get(url))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(expected[0])))
                    .andExpect(content().string(containsString(expected[1])))
                    .andExpect(content().string(containsString(expected[2])))
                    .andExpect(content().string(containsString(expected[3])))
                    .andExpect(content().string(containsString(expected[4])))
                    .andExpect(content().string(containsString(expected[5])));
        }
        finally
        {
            ReflectionTestUtils.setField(transformController, "behindIngres", origBehindIngres);
        }
    }

    @Test
    public void testVersionEndpointIncludesAvailable() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_VERSION))
                .andExpect(status().isOk())
                .andExpect(content().string("TwoCustomTransformers " + coreVersion));
    }

    @Test
    public void testRootEndpointReturnsTestPage() throws Exception
    {
        testPageWithOrWithoutIngresPrefix(ENDPOINT_ROOT, false,
                "TwoCustomTransformers Test Page",
                "action=\"/test\"",
                "<a href=\"/log\">Log</a>",
                "<a href=\"/ready\">Ready</a>",
                "<a href=\"/live\">Live</a>",
                "<a href=\"/transform/config?configVersion=9999\">Config</a>");
    }

    @Test
    public void testRootEndpointReturnsTestPageWithIngres() throws Exception
    {
        testPageWithOrWithoutIngresPrefix(ENDPOINT_ROOT, true,
                "TwoCustomTransformers Test Page",
                "action=\"/twocustomtransformers/test\"",
                "href=\"/twocustomtransformers/log\"",
                "<a href=\"/twocustomtransformers/ready\">Ready</a>",
                "<a href=\"/twocustomtransformers/live\">Live</a>",
                "<a href=\"/twocustomtransformers/transform/config?configVersion=9999\">Config</a>");
    }

    @Test
    public void testErrorEndpointReturnsErrorPage() throws Exception
    {
        testPageWithOrWithoutIngresPrefix(ENDPOINT_ERROR, false,
                "TwoCustomTransformers Error Page",
                "<a href=\"/\">Test</a>",
                "<a href=\"/log\">Log</a>",
                "<a href=\"/ready\">Ready</a>",
                "<a href=\"/live\">Live</a>",
                "<a href=\"/transform/config?configVersion=9999\">Config</a>");
    }

    @Test
    public void testErrorEndpointReturnsErrorPageWithIngres() throws Exception
    {
        testPageWithOrWithoutIngresPrefix(ENDPOINT_ERROR, true,
                "TwoCustomTransformers Error Page",
                "href=\"/twocustomtransformers/\"",
                "href=\"/twocustomtransformers/log\"",
                "<a href=\"/twocustomtransformers/ready\">Ready</a>",
                "<a href=\"/twocustomtransformers/live\">Live</a>",
                "<a href=\"/twocustomtransformers/transform/config?configVersion=9999\">Config</a>");
    }

    @Test
    public void testLogEndpointReturnsLogPage() throws Exception
    {
        testPageWithOrWithoutIngresPrefix(ENDPOINT_LOG, false,
                "TwoCustomTransformers Log Entries",
                "<a href=\"/\">Test</a>",
                "Log",
                "<a href=\"/ready\">Ready</a>",
                "<a href=\"/live\">Live</a>",
                "<a href=\"/transform/config?configVersion=9999\">Config</a>");
    }

    @Test
    public void testLogEndpointReturnsLogPageWithIngres() throws Exception
    {
        testPageWithOrWithoutIngresPrefix(ENDPOINT_LOG, true,
                "TwoCustomTransformers Log Entries",
                "href=\"/twocustomtransformers/\"",
                "Log",
                "<a href=\"/twocustomtransformers/ready\">Ready</a>",
                "<a href=\"/twocustomtransformers/live\">Live</a>",
                "<a href=\"/twocustomtransformers/transform/config?configVersion=9999\">Config</a>");
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
    public void testLiveEndpointReturnsErrorAfterTooManyTransforms() throws Exception
    {
        resetProbeForTesting(transformController);
        ProbeTransform probeTransform = transformController.getProbeTransform();
        IntStream.range(0, 1024 + 1).forEach(i -> probeTransform.incrementTransformerCount());
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_LIVE))
                .andExpect(status().is(TOO_MANY_REQUESTS.value()))
                .andExpect(content().string(containsString("Transformer requested to die. It has performed more than 1024 transformations")));
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
                "Pdf2Png," + coreVersion + ",directAccessUrl,imageOptions,sourceFilename\n"
                        + "TxT2Pdf," + coreVersion + ",directAccessUrl,docOptions,sourceFilename\n"
                        + "Txt2JpgViaPdf,null,imageOptions\n"
                        + "Txt2PngViaPdf," + coreVersion + ",directAccessUrl,imageOptions,sourceFilename",
                "directAccessUrl,docOptions,imageOptions,sourceFilename", mockMvc, objectMapper);
    }

    static void assertConfig(String url, String expectedTransformers, String expectedOptions,
            MockMvc mockMvc, ObjectMapper objectMapper) throws Exception
    {
        TransformConfig config = objectMapper.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get(url))
                        .andExpect(status().isOk())
                        .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                TransformConfig.class);

        // Gets a list of transformerNames,coreVersion,optionNames
        assertEquals(expectedTransformers,
                config.getTransformers().stream()
                        .map(t -> t.getTransformerName() + ","
                                + t.getCoreVersion() + ","
                                + t.getTransformOptions().stream().sorted().collect(Collectors.joining(",")))
                        .sorted()
                        .collect(Collectors.joining("\n")));

        assertEquals(expectedOptions,
                config.getTransformOptions().keySet().stream()
                        .sorted()
                        .collect(Collectors.joining(",")));
    }

    @Test
    public void testTransformEndpointThatUsesTransformRequests() throws Exception
    {
        final Map<String, File> sfsRef2File = new HashMap<>();
        when(fakeSfsClient.saveFile(any())).thenAnswer((Answer) invocation -> {
            File originalFile = (File) invocation.getArguments()[0];

            // Make a copy as the original might get deleted
            File fileCopy = new File(tempDir, originalFile.getName() + "copy");
            FileUtils.copyFile(originalFile, fileCopy);

            String fileRef = UUID.randomUUID().toString();
            sfsRef2File.put(fileRef, fileCopy);
            return new FileRefResponse(new FileRefEntity(fileRef));
        });

        when(fakeSfsClient.retrieveFile(any())).thenAnswer((Answer) invocation -> ResponseEntity.ok().header(CONTENT_DISPOSITION, "attachment; filename*=UTF-8''transform.tmp")
                .body((Resource) new UrlResource(sfsRef2File.get(invocation.getArguments()[0]).toURI())));

        File sourceFile = getTestFile("original.txt", true, tempDir);
        String sourceFileRef = fakeSfsClient.saveFile(sourceFile).getEntry().getFileRef();

        TransformRequest transformRequest = TransformRequest.builder()
                .withRequestId("1")
                .withSchema(1)
                .withClientData("Alfresco Digital Business Platform")
                // .withTransformRequestOptions(ImmutableMap.of(DIRECT_ACCESS_URL, "file://"+sourceFile.toPath()))
                .withSourceReference(sourceFileRef)
                .withSourceMediaType(MIMETYPE_TEXT_PLAIN)
                .withSourceSize(sourceFile.length())
                .withTargetMediaType(MIMETYPE_PDF)
                .withInternalContextForTransformEngineTests()
                .build();

        String transformRequestJson = objectMapper.writeValueAsString(transformRequest);
        String transformReplyJson = mockMvc
                .perform(MockMvcRequestBuilders
                        .post(ENDPOINT_TRANSFORM)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .content(transformRequestJson))
                .andExpect(status().is(CREATED.value()))
                .andReturn().getResponse().getContentAsString();
        TransformReply transformReply = objectMapper.readValue(transformReplyJson, TransformReply.class);
        String newValue = new String(fakeSfsClient.retrieveFile(transformReply.getTargetReference()).getBody()
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(transformRequest.getRequestId(), transformReply.getRequestId());
        assertEquals(transformRequest.getClientData(), transformReply.getClientData());
        assertEquals(transformRequest.getSchema(), transformReply.getSchema());
        assertEquals("Original Text -> TxT2Pdf()", newValue);
    }

    @Test
    public void testTransformEndpointThatUploadsAndDownloadsContent() throws Exception
    {
        mockMvc.perform(
                MockMvcRequestBuilders.multipart(ENDPOINT_TRANSFORM)
                        .file(new MockMultipartFile("file", null, MIMETYPE_TEXT_PLAIN,
                                "Start".getBytes(StandardCharsets.UTF_8)))
                        .param(SOURCE_MIMETYPE, MIMETYPE_TEXT_PLAIN)
                        .param(TARGET_MIMETYPE, MIMETYPE_PDF)
                        .param(PAGE_REQUEST_PARAM, "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''transform.pdf"))
                .andExpect(content().string("Start -> TxT2Pdf(page=1)"));
    }

    @Test
    public void testTestTransformEndpointWhichConvertsRequestParameters() throws Exception
    {
        TransformHandler transformHandlerOrig = transformController.transformHandler;
        try
        {
            TransformHandler transformHandlerSpy = spy(transformHandlerOrig);
            transformController.transformHandler = transformHandlerSpy;

            mockMvc.perform(
                    MockMvcRequestBuilders.multipart(ENDPOINT_TEST)
                            .file(new MockMultipartFile("file", null, MIMETYPE_TEXT_PLAIN,
                                    "Start".getBytes(StandardCharsets.UTF_8)))
                            .param(SOURCE_MIMETYPE, MIMETYPE_IMAGE_BMP)
                            .param("_" + SOURCE_MIMETYPE, MIMETYPE_TEXT_PLAIN)
                            .param(TARGET_MIMETYPE, MIMETYPE_PDF)
                            .param("_" + TARGET_MIMETYPE, "")
                            .param(PAGE_REQUEST_PARAM, "replaced")
                            .param("name1", "hasNoValueSoRemoved").param("value1", "")
                            .param("name2", PAGE_REQUEST_PARAM).param("value2", "1")
                            .param("name3", SOURCE_ENCODING).param("value3", "UTF-8"));

            verify(transformHandlerSpy).handleHttpRequest(any(), any(), eq(MIMETYPE_TEXT_PLAIN), eq(MIMETYPE_PDF),
                    eq(ImmutableMap.of(
                            SOURCE_MIMETYPE, MIMETYPE_TEXT_PLAIN,
                            TARGET_MIMETYPE, MIMETYPE_PDF,
                            PAGE_REQUEST_PARAM, "1",
                            SOURCE_ENCODING, "UTF-8")),
                    any());
        }
        finally
        {
            transformController.transformHandler = transformHandlerOrig;
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
                .andExpect(status().reason(containsString("Request parameter '" + SOURCE_MIMETYPE + "' is missing")));
    }

    @Test
    public void testInterceptOfTransformException_noTransformers() throws Exception
    {
        mockMvc.perform(
                MockMvcRequestBuilders.multipart(ENDPOINT_TRANSFORM)
                        .file(new MockMultipartFile("file", null, MIMETYPE_TEXT_PLAIN,
                                "Start".getBytes(StandardCharsets.UTF_8)))
                        .param(SOURCE_MIMETYPE, MIMETYPE_TEXT_PLAIN)
                        .param(TARGET_MIMETYPE, MIMETYPE_PDF)
                        .param("unknown", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("TwoCustomTransformers Error Page")))
                .andExpect(content().string(containsString("No transforms for: text/plain (5 bytes) -&gt; application/pdf unknown=1")));
    }
}
