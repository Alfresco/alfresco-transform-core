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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.transform.base.clients.AlfrescoSharedFileStoreClient;
import org.alfresco.transform.base.model.FileRefEntity;
import org.alfresco.transform.base.model.FileRefResponse;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.client.model.InternalContext;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.messages.TransformStack;
import org.alfresco.transform.registry.TransformServiceRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.alfresco.transform.common.RequestParamMap.DIRECT_ACCESS_URL;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Super class for testing.
 */
@SpringBootTest(classes={org.alfresco.transform.base.Application.class})
@AutoConfigureMockMvc
public abstract class AbstractBaseTest
{
    // Added as part of ATS-702 to allow test resources to be read from the imported jar files to prevent test
    // resource duplication
    @TempDir
    public File tempDir;

    @Autowired
    protected TransformController controller;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected AlfrescoSharedFileStoreClient alfrescoSharedFileStoreClient;

    @SpyBean
    protected TransformServiceRegistry transformRegistry;

    @Autowired
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
    protected byte[] sourceFileBytes;

    /**
     * The expected result. Taken resting target quick file's bytes.
     *
     * Note: These checks generally don't work on Windows (Mac and Linux are okay). Possibly to do with byte order
     * loading.
     */
    protected byte[] expectedTargetFileBytes;

    // Called by sub class
    protected abstract void mockTransformCommand(String sourceExtension,
        String targetExtension, String sourceMimetype,
        boolean readTargetFileBytes) throws IOException;

    protected ProbeTransform getProbeTestTransform()
    {
        return controller.probeTransform;
    }

    protected abstract void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest);

//    static void assertConfig(String url, String expectedTransformers, String expectedOptions,
//        MockMvc mockMvc, ObjectMapper objectMapper) throws Exception
//    {
//        TransformConfig config = objectMapper.readValue(
//            mockMvc.perform(MockMvcRequestBuilders.get(url))
//                   .andExpect(status().isOk())
//                   .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
//                   .andReturn()
//                   .getResponse()
//                   .getContentAsString(), TransformConfig.class);
//
//        // Gets a list of transformerNames,coreVersion,optionNames
//        assertEquals(expectedTransformers,
//            config.getTransformers().stream()
//                  .map(t -> t.getTransformerName()+","
//                                +t.getCoreVersion()+","
//                                +t.getTransformOptions().stream().sorted().collect(Collectors.joining(",")))
//                  .sorted()
//                  .collect(Collectors.joining("\n")));
//
//        assertEquals(expectedOptions,
//            config.getTransformOptions().keySet().stream()
//                  .sorted()
//                  .collect(Collectors.joining(",")));
//    }
//
    /**
     * This method ends up being the core of the mock.
     * It copies content from an existing file in the resources folder to the desired location
     * in order to simulate a successful transformation.
     *
     * @param actualTargetExtension Requested extension.
     * @param testFile              The test file (transformed) - basically the result.
     * @param targetFile            The location where the content from the testFile should be copied
     * @throws IOException in case of any errors.
     */
    public void generateTargetFileFromResourceFile(String actualTargetExtension, File testFile,
        File targetFile) throws IOException
    {
        if (testFile == null)
        {
            testFile = getTestFile("quick." + actualTargetExtension, false);
        }
        if (testFile != null)
        {
            try (var inputStream = new FileInputStream(testFile);
                 var outputStream = new FileOutputStream(targetFile))
            {
                FileChannel source = inputStream.getChannel();
                FileChannel target = outputStream.getChannel();
                target.transferFrom(source, 0, source.size());

            } catch (Exception e) 
            {
                throw e;
            }
        }
    }

    protected byte[] readTestFile(String extension) throws IOException
    {
        return Files.readAllBytes(getTestFile("quick." + extension, true).toPath());
    }

    protected File getTestFile(String testFilename, boolean required) throws IOException
    {
        return getTestFile(testFilename, required, tempDir);
    }

    public static File getTestFile(String testFilename, boolean required, File tempDir) throws IOException
    {
        File testFile = null;
        ClassLoader classLoader = AbstractBaseTest.class.getClassLoader();
        URL testFileUrl = classLoader.getResource(testFilename);
        if (required && testFileUrl == null)
        {
            throw new IOException("The test file " + testFilename +
                    " does not exist in the resources directory");
        }
        // Added as part of ATS-702 to allow test resources to be read from the imported jar files to prevent test
        // resource duplication
        if (testFileUrl != null)
        {
            // Each use of the tempDir should result in a unique directory being used
            testFile = new File(tempDir, testFilename);
            Files.copy(classLoader.getResourceAsStream(testFilename), testFile.toPath(),REPLACE_EXISTING);
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

    private MockHttpServletRequestBuilder mockMvcRequestWithoutMockMultipartFile(String url, String... params)
    {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(url);

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
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(ENDPOINT_TRANSFORM).file(sourceFile);

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
        return TransformRequest.builder()
        .withRequestId("1")
        .withSchema(1)
        .withClientData("Alfresco Digital Business Platform")
        .withTransformRequestOptions(options)
        .withSourceReference(sourceFileRef)
        .withSourceExtension(sourceExtension)
        .withSourceMediaType(sourceMimetype)
        .withSourceSize(sourceFile.length())
        .withTargetExtension(targetExtension)
        .withTargetMediaType(targetMimetype)
        .withInternalContextForTransformEngineTests()
        .build();
    }

    @Test
    public void simpleTransformTest() throws Exception
    {
        mockMvc.perform(
            mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile))
               .andExpect(request().asyncStarted())
               .andDo(MvcResult::getAsyncResult)
               .andExpect(status().isOk())
               .andExpect(content().bytes(expectedTargetFileBytes))
               .andExpect(header().string("Content-Disposition",
                   "attachment; filename*=UTF-8''transform." + targetExtension));
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
        sourceFile = new MockMultipartFile("file", "../quick." + sourceExtension, sourceMimetype, sourceFileBytes);

        mockMvc.perform(
            mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile))
               .andExpect(request().asyncStarted())
               .andDo(MvcResult::getAsyncResult)
               .andExpect(status().isOk())
               .andExpect(content().bytes(expectedTargetFileBytes))
               .andExpect(header().string("Content-Disposition",
                   "attachment; filename*=UTF-8''transform." + targetExtension));
    }

    @Test
    // Is okay, as the target filename is built up from the whole source filename and the targetExtension
    public void noExtensionSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "../quick", sourceMimetype, sourceFileBytes);

        mockMvc.perform(
            mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile))
               .andExpect(request().asyncStarted())
               .andDo(MvcResult::getAsyncResult)
               .andExpect(status().isOk())
               .andExpect(content().bytes(expectedTargetFileBytes))
               .andExpect(header().string("Content-Disposition",
                   "attachment; filename*=UTF-8''transform." + targetExtension));
    }

    @Test
    public void calculateMaxTime() throws Exception
    {
        ProbeTransform probeTransform = controller.probeTransform;
        probeTransform.setLivenessPercent(110);

        long[][] values = new long[][]{
            {5000, 0, Long.MAX_VALUE}, // 1st transform is ignored
            {1000, 1000, 2100},        // 1000 + 1000*1.1
            {3000, 2000, 4200},        // 2000 + 2000*1.1
            {2000, 2000, 4200},
            {6000, 3000, 6300},
            {8000, 4000, 8400},
            {4444, 4000, 8400},        // no longer in the first few, so normal and max times don't change
            {5555, 4000, 8400}
        };

        for (long[] v : values)
        {
            long time = v[0];
            long expectedNormalTime = v[1];
            long expectedMaxTime = v[2];

            probeTransform.calculateMaxTime(time, true);
            assertEquals(expectedNormalTime, probeTransform.getNormalTime());
            assertEquals(expectedMaxTime, probeTransform.getMaxTime());
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

    @Test
    public void queueTransformRequestUsingDirectAccessUrlTest() throws Exception
    {
        // Files
        String sourceFileRef = UUID.randomUUID().toString();
        File sourceFile = getTestFile("quick." + sourceExtension, true);
        String targetFileRef = UUID.randomUUID().toString();

        TransformRequest transformRequest = createTransformRequest(sourceFileRef, sourceFile);
        Map<String, String> transformRequestOptions = transformRequest.getTransformRequestOptions();

        String directUrl = "file://" + sourceFile.toPath();

        transformRequestOptions.put(DIRECT_ACCESS_URL, directUrl);
//        transformRequest.setTransformRequestOptions(transformRequestOptions);

        when(alfrescoSharedFileStoreClient.saveFile(any()))
                .thenReturn(new FileRefResponse(new FileRefEntity(targetFileRef)));

        // Update the Transformation Request with any specific params before sending it
        updateTransformRequestWithSpecificOptions(transformRequest);

        // Serialize and call the transformer
        String tr = objectMapper.writeValueAsString(transformRequest);
        String transformationReplyAsString = mockMvc
                .perform(MockMvcRequestBuilders
                        .post(ENDPOINT_TRANSFORM)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .content(tr))
                .andExpect(status().is(CREATED.value()))
                .andReturn().getResponse().getContentAsString();
        TransformReply transformReply = objectMapper.readValue(transformationReplyAsString, TransformReply.class);

        // Assert the reply
        assertEquals(transformRequest.getRequestId(), transformReply.getRequestId());
        assertEquals(transformRequest.getClientData(), transformReply.getClientData());
        assertEquals(transformRequest.getSchema(), transformReply.getSchema());
    }

    @Test
    public void httpTransformRequestUsingDirectAccessUrlTest() throws Exception
    {
        File dauSourceFile = getTestFile("quick." + sourceExtension, true);
        String directUrl = "file://" + dauSourceFile.toPath();

        MvcResult mvcResult = mockMvc.perform(
             mockMvcRequest(ENDPOINT_TRANSFORM, null)
            .param(DIRECT_ACCESS_URL, directUrl))
            .andExpect(request().asyncStarted())
            .andReturn();

        ResultActions resultActions = mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                "attachment; filename*=UTF-8''transform."+targetExtension));

        if (expectedTargetFileBytes != null)
        {
            resultActions.andExpect(content().bytes(expectedTargetFileBytes));
        }
    }
}
