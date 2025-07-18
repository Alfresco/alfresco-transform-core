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
package org.alfresco.transformer;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.alfresco.transform.common.RequestParamMap.DIRECT_ACCESS_URL;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM_CONFIG;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM_CONFIG_LATEST;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import org.alfresco.transform.client.model.InternalContext;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.config.SupportedSourceAndTarget;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.TransformOption;
import org.alfresco.transform.config.TransformOptionGroup;
import org.alfresco.transform.config.TransformOptionValue;
import org.alfresco.transform.config.Transformer;
import org.alfresco.transform.messages.TransformStack;
import org.alfresco.transform.registry.TransformServiceRegistry;
import org.alfresco.transformer.clients.AlfrescoSharedFileStoreClient;
import org.alfresco.transformer.probes.ProbeTestTransform;

/**
 * @deprecated will be removed in a future release. Replaced by alfresco-base-t-engine.
 *
 *             Super class for testing controllers without a server. Includes tests for the AbstractTransformerController itself.
 */
@Deprecated
public abstract class AbstractTransformerControllerTest
{
    @TempDir // added as part of ATS-702 to allow test resources to be read from the imported jar files to prevent test resource duplication
    public File tempDir;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected AlfrescoSharedFileStoreClient alfrescoSharedFileStoreClient;

    @SpyBean
    protected TransformServiceRegistry transformRegistry;

    @Value("${transform.core.version}")
    private String coreVersion;

    protected String sourceExtension;
    protected String targetExtension;
    protected String sourceMimetype;
    protected String targetMimetype;
    protected HashMap<String, String> options = new HashMap<>();

    protected MockMultipartFile sourceFile;
    protected String expectedOptions;
    protected String expectedSourceSuffix;
    protected Long expectedTimeout = 0L;
    protected byte[] expectedSourceFileBytes;

    /**
     * The expected result. Taken resting target quick file's bytes.
     *
     * Note: These checks generally don't work on Windows (Mac and Linux are okay). Possibly to do with byte order loading.
     */
    protected byte[] expectedTargetFileBytes;

    // Called by sub class
    protected abstract void mockTransformCommand(String sourceExtension,
            String targetExtension, String sourceMimetype,
            boolean readTargetFileBytes) throws IOException;

    protected abstract AbstractTransformerController getController();

    protected abstract void updateTransformRequestWithSpecificOptions(
            TransformRequest transformRequest);

    /**
     * This method ends up being the core of the mock. It copies content from an existing file in the resources folder to the desired location in order to simulate a successful transformation.
     *
     * @param actualTargetExtension
     *            Requested extension.
     * @param testFile
     *            The test file (transformed) - basically the result.
     * @param targetFile
     *            The location where the content from the testFile should be copied
     * @throws IOException
     *             in case of any errors.
     */
    void generateTargetFileFromResourceFile(String actualTargetExtension, File testFile,
            File targetFile) throws IOException
    {
        if (testFile != null)
        {
            try (var inputStream = new FileInputStream(testFile);
                    var outputStream = new FileOutputStream(targetFile))
            {
                FileChannel source = inputStream.getChannel();
                FileChannel target = outputStream.getChannel();
                target.transferFrom(source, 0, source.size());

            }
            catch (Exception e)
            {
                throw e;
            }
        }
        else
        {
            testFile = getTestFile("quick." + actualTargetExtension, false);
            if (testFile != null)
            {
                try (var inputStream = new FileInputStream(testFile);
                        var outputStream = new FileOutputStream(targetFile))
                {
                    FileChannel source = inputStream.getChannel();
                    FileChannel target = outputStream.getChannel();
                    target.transferFrom(source, 0, source.size());

                }
                catch (Exception e)
                {
                    throw e;
                }
            }
        }
    }

    protected byte[] readTestFile(String extension) throws IOException
    {
        return Files.readAllBytes(getTestFile("quick." + extension, true).toPath());
    }

    protected File getTestFile(String testFilename, boolean required) throws IOException
    {
        File testFile = null;
        ClassLoader classLoader = getClass().getClassLoader();
        URL testFileUrl = classLoader.getResource(testFilename);
        if (required && testFileUrl == null)
        {
            throw new IOException("The test file " + testFilename +
                    " does not exist in the resources directory");
        }
        // added as part of ATS-702 to allow test resources to be read from the imported jar files to prevent test resource duplication
        if (testFileUrl != null)
        {
            // Each use of the tempDir should result in a unique directory being used
            testFile = new File(tempDir, testFilename);
            Files.copy(classLoader.getResourceAsStream(testFilename), testFile.toPath(), REPLACE_EXISTING);
        }

        return testFileUrl == null ? null : testFile;
    }

    protected MockHttpServletRequestBuilder mockMvcRequest(String url, MockMultipartFile sourceFile, String... params)
    {
        if (sourceFile == null)
        {
            return mockMvcRequestWithoutMockMultipartFile(url, params);
        }
        else
        {
            return mockMvcRequestWithMockMultipartFile(url, sourceFile, params);
        }
    }

    private MockHttpServletRequestBuilder mockMvcRequestWithoutMockMultipartFile(String url,
            String... params)
    {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(ENDPOINT_TRANSFORM);

        if (params.length % 2 != 0)
        {
            throw new IllegalArgumentException("each param should have a name and value.");
        }
        for (int i = 0; i < params.length; i += 2)
        {
            builder = builder.param(params[i], params[i + 1]);
        }

        return builder;
    }

    private MockHttpServletRequestBuilder mockMvcRequestWithMockMultipartFile(String url, MockMultipartFile sourceFile,
            String... params)
    {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(ENDPOINT_TRANSFORM).file(
                sourceFile);

        if (params.length % 2 != 0)
        {
            throw new IllegalArgumentException("each param should have a name and value.");
        }
        for (int i = 0; i < params.length; i += 2)
        {
            builder = builder.param(params[i], params[i + 1]);
        }

        return builder;
    }

    protected TransformRequest createTransformRequest(String sourceFileRef, File sourceFile)
    {
        TransformRequest transformRequest = new TransformRequest();
        transformRequest.setRequestId("1");
        transformRequest.setSchema(1);
        transformRequest.setClientData("Alfresco Digital Business Platform");
        transformRequest.setTransformRequestOptions(options);
        transformRequest.setSourceReference(sourceFileRef);
        transformRequest.setSourceExtension(sourceExtension);
        transformRequest.setSourceMediaType(sourceMimetype);
        transformRequest.setSourceSize(sourceFile.length());
        transformRequest.setTargetExtension(targetExtension);
        transformRequest.setTargetMediaType(targetMimetype);
        transformRequest.setInternalContext(InternalContext.initialise(null));
        transformRequest.getInternalContext().getMultiStep().setInitialRequestId("123");
        transformRequest.getInternalContext().getMultiStep().setInitialSourceMediaType(sourceMimetype);
        TransformStack.setInitialTransformRequestOptions(transformRequest.getInternalContext(), options);
        TransformStack.setInitialSourceReference(transformRequest.getInternalContext(), sourceFileRef);
        TransformStack.addTransformLevel(transformRequest.getInternalContext(),
                TransformStack.levelBuilder(TransformStack.PIPELINE_FLAG)
                        .withStep("transformerName", sourceMimetype, targetMimetype));
        return transformRequest;
    }

    @Test
    public void simpleTransformTest() throws Exception
    {
        mockMvc.perform(
                mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile, "targetExtension", targetExtension))
                .andExpect(status().is(OK.value()))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*= UTF-8''quick." + targetExtension));
    }

    @Test
    public void testDelayTest() throws Exception
    {
        long start = System.currentTimeMillis();
        mockMvc.perform(mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile, "targetExtension", targetExtension,
                "testDelay", "400"))
                .andExpect(status().is(OK.value()))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*= UTF-8''quick." + targetExtension));
        long ms = System.currentTimeMillis() - start;
        System.out.println("Transform incluing test delay was " + ms);
        assertTrue(ms >= 400, "Delay sending the result back was too small " + ms);
        assertTrue(ms <= 500, "Delay sending the result back was too big " + ms);
    }

    @Test
    public void noTargetFileTest() throws Exception
    {
        mockMvc.perform(mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile, "targetExtension", "xxx"))
                .andExpect(status().is(INTERNAL_SERVER_ERROR.value()));
    }

    @Test
    // Looks dangerous but is okay as we only use the final filename
    public void dotDotSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "../quick." + sourceExtension, sourceMimetype,
                expectedSourceFileBytes);

        mockMvc.perform(
                mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile, "targetExtension", targetExtension))
                .andExpect(status().is(OK.value()))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*= UTF-8''quick." + targetExtension));
    }

    @Test
    // Is okay, as the target filename is built up from the whole source filename and the targetExtension
    public void noExtensionSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "../quick", sourceMimetype,
                expectedSourceFileBytes);

        mockMvc.perform(
                mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile, "targetExtension", targetExtension))
                .andExpect(status().is(OK.value()))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*= UTF-8''quick." + targetExtension));
    }

    @Test
    // Invalid file name that ends in /
    public void badSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "abc/", sourceMimetype, expectedSourceFileBytes);

        mockMvc.perform(
                mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile, "targetExtension", targetExtension))
                .andExpect(status().is(BAD_REQUEST.value()))
                .andExpect(status().reason(containsString("The source filename was not supplied")));
    }

    @Test
    public void blankSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "", sourceMimetype, expectedSourceFileBytes);

        mockMvc.perform(
                mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile, "targetExtension", targetExtension))
                .andExpect(status().is(BAD_REQUEST.value()));
    }

    @Test
    public void noTargetExtensionTest() throws Exception
    {
        mockMvc.perform(mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile))
                .andExpect(status().is(BAD_REQUEST.value()))
                .andExpect(status().reason(
                        containsString("Request parameter 'targetExtension' is missing")));
    }

    @Test
    public void calculateMaxTime() throws Exception
    {
        ProbeTestTransform probeTestTransform = getController().getProbeTestTransform();
        probeTestTransform.setLivenessPercent(110);

        long[][] values = new long[][]{
                {5000, 0, Long.MAX_VALUE}, // 1st transform is ignored
                {1000, 1000, 2100}, // 1000 + 1000*1.1
                {3000, 2000, 4200}, // 2000 + 2000*1.1
                {2000, 2000, 4200},
                {6000, 3000, 6300},
                {8000, 4000, 8400},
                {4444, 4000, 8400}, // no longer in the first few, so normal and max times don't change
                {5555, 4000, 8400}
        };

        for (long[] v : values)
        {
            long time = v[0];
            long expectedNormalTime = v[1];
            long expectedMaxTime = v[2];

            probeTestTransform.calculateMaxTime(time, true);
            assertEquals(expectedNormalTime, probeTestTransform.getNormalTime());
            assertEquals(expectedMaxTime, probeTestTransform.getMaxTime());
        }
    }

    @Test
    public void testEmptyPojoTransform() throws Exception
    {
        // Transformation Request POJO
        TransformRequest transformRequest = new TransformRequest();

        // Serialize and call the transformer
        String tr = objectMapper.writeValueAsString(transformRequest);
        String transformationReplyAsString = mockMvc
                .perform(MockMvcRequestBuilders
                        .post(ENDPOINT_TRANSFORM)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .content(tr))
                .andExpect(status().is(BAD_REQUEST.value()))
                .andReturn().getResponse().getContentAsString();

        TransformReply transformReply = objectMapper.readValue(transformationReplyAsString,
                TransformReply.class);

        // Assert the reply
        assertEquals(BAD_REQUEST.value(), transformReply.getStatus());
    }

    /**
     *
     * @return transformer specific engine config name
     */
    public String getEngineConfigName()
    {
        return "engine_config.json";
    }

    @Test
    public void testGetTransformConfigInfo() throws Exception
    {
        TransformConfig expectedTransformConfig = objectMapper
                .readValue(getTestFile(getEngineConfigName(), true),
                        TransformConfig.class);
        expectedTransformConfig.getTransformers().forEach(transformer -> {
            transformer.setCoreVersion(coreVersion);
            transformer.getTransformOptions().add(DIRECT_ACCESS_URL);
        });
        expectedTransformConfig.getTransformOptions().put(DIRECT_ACCESS_URL, Set.of(new TransformOptionValue(false, DIRECT_ACCESS_URL)));

        ReflectionTestUtils.setField(transformRegistry, "engineConfig",
                new ClassPathResource(getEngineConfigName()));

        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(ENDPOINT_TRANSFORM_CONFIG_LATEST))
                .andExpect(status().is(OK.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andReturn().getResponse().getContentAsString();

        TransformConfig transformConfig = objectMapper.readValue(response, TransformConfig.class);
        assertEquals(expectedTransformConfig, transformConfig);
    }

    @Test
    // Test for case when T-Router or Repository is a version that does not expect it
    public void testGetTransformConfigInfoExcludingCoreVersion() throws Exception
    {
        TransformConfig expectedTransformConfig = objectMapper
                .readValue(getTestFile(getEngineConfigName(), true),
                        TransformConfig.class);

        ReflectionTestUtils.setField(transformRegistry, "engineConfig",
                new ClassPathResource(getEngineConfigName()));

        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(ENDPOINT_TRANSFORM_CONFIG))
                .andExpect(status().is(OK.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andReturn().getResponse().getContentAsString();

        TransformConfig transformConfig = objectMapper.readValue(response, TransformConfig.class);
        assertEquals(expectedTransformConfig, transformConfig);
    }

    @Test
    public void testGetInfoFromConfigWithDuplicates() throws Exception
    {
        TransformConfig expectedResult = buildCompleteTransformConfig();

        ReflectionTestUtils.setField(transformRegistry, "engineConfig",
                new ClassPathResource("engine_config_with_duplicates.json"));

        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(ENDPOINT_TRANSFORM_CONFIG))
                .andExpect(status().is(OK.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andReturn().getResponse().getContentAsString();

        TransformConfig transformConfig = objectMapper.readValue(response, TransformConfig.class);

        assertNotNull(transformConfig);
        assertEquals(expectedResult, transformConfig);
        assertEquals(3, transformConfig.getTransformOptions().get("engineXOptions").size());
        assertEquals(1,
                transformConfig.getTransformers().get(0).getSupportedSourceAndTargetList().size());
        assertEquals(1,
                transformConfig.getTransformers().get(0).getTransformOptions().size());
    }

    @Test
    public void testGetInfoFromConfigWithEmptyTransformOptions() throws Exception
    {
        Transformer transformer = buildTransformer("application/pdf", "image/png");
        TransformConfig expectedResult = new TransformConfig();
        expectedResult.setTransformers(ImmutableList.of(transformer));

        ReflectionTestUtils.setField(transformRegistry, "engineConfig",
                new ClassPathResource("engine_config_incomplete.json"));

        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(ENDPOINT_TRANSFORM_CONFIG))
                .andExpect(status().is(OK.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andReturn().getResponse().getContentAsString();

        TransformConfig transformConfig = objectMapper.readValue(response, TransformConfig.class);

        assertNotNull(transformConfig);
        assertEquals(expectedResult, transformConfig);
    }

    @Test
    public void testGetInfoFromConfigWithNoTransformOptions() throws Exception
    {
        Transformer transformer = buildTransformer("application/pdf", "image/png");
        transformer.setTransformerName("engineX");
        TransformConfig expectedResult = new TransformConfig();
        expectedResult.setTransformers(ImmutableList.of(transformer));

        ReflectionTestUtils.setField(transformRegistry, "engineConfig",
                new ClassPathResource("engine_config_no_transform_options.json"));

        String response = mockMvc
                .perform(MockMvcRequestBuilders.get(ENDPOINT_TRANSFORM_CONFIG))
                .andExpect(status().is(OK.value()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andReturn().getResponse().getContentAsString();

        TransformConfig transformConfig = objectMapper.readValue(response, TransformConfig.class);

        assertNotNull(transformConfig);
        assertEquals(expectedResult, transformConfig);
    }

    private TransformConfig buildCompleteTransformConfig()
    {
        TransformConfig expectedResult = new TransformConfig();

        Set<TransformOption> transformOptionGroup = ImmutableSet.of(
                new TransformOptionValue(false, "cropGravity"));
        Set<TransformOption> transformOptions = ImmutableSet.of(
                new TransformOptionValue(false, "page"),
                new TransformOptionValue(false, "width"),
                new TransformOptionGroup(false, transformOptionGroup));
        Map<String, Set<TransformOption>> transformOptionsMap = ImmutableMap.of("engineXOptions",
                transformOptions);

        Transformer transformer = buildTransformer("application/pdf", "image/png", "engineXOptions",
                "engineX");
        List<Transformer> transformers = ImmutableList.of(transformer);

        expectedResult.setTransformOptions(transformOptionsMap);
        expectedResult.setTransformers(transformers);

        return expectedResult;
    }

    private Transformer buildTransformer(String sourceMediaType, String targetMediaType,
            String transformOptions, String transformerName)
    {
        Transformer transformer = buildTransformer(sourceMediaType, targetMediaType);
        transformer.setTransformerName(transformerName);
        transformer.setTransformOptions(ImmutableSet.of(transformOptions));

        return transformer;
    }

    private Transformer buildTransformer(String sourceMediaType, String targetMediaType)
    {
        Set<SupportedSourceAndTarget> supportedSourceAndTargetList = ImmutableSet.of(
                SupportedSourceAndTarget.builder()
                        .withSourceMediaType(sourceMediaType)
                        .withTargetMediaType(targetMediaType)
                        .build());

        Transformer transformer = new Transformer();
        transformer.setSupportedSourceAndTargetList(supportedSourceAndTargetList);
        return transformer;
    }
}
