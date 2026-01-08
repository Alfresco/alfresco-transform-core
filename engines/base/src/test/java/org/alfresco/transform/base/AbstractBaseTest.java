/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2026 Alfresco Software Limited
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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.alfresco.transform.common.RequestParamMap.DIRECT_ACCESS_URL;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import org.alfresco.transform.base.executors.CommandExecutor;
import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.model.FileRefEntity;
import org.alfresco.transform.base.model.FileRefResponse;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.base.sfs.SharedFileStoreClient;
import org.alfresco.transform.base.transform.TransformHandler;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.registry.TransformServiceRegistry;

/**
 * Super class for unit testing.
 */
@SpringBootTest(classes = {org.alfresco.transform.base.Application.class})
@AutoConfigureMockMvc
public abstract class AbstractBaseTest
{
    // Added as part of ATS-702 to allow test resources to be read from the imported jar files to prevent test
    // resource duplication
    @TempDir
    public File tempDir;

    @Autowired
    protected TransformHandler transformHandler;
    @Autowired
    protected TransformController controller;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected SharedFileStoreClient sharedFileStoreClient;

    @Autowired
    protected TransformServiceRegistry transformRegistry;

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
     * Note: These checks generally don't work on Windows (Mac and Linux are okay). Possibly to do with byte order loading.
     */
    protected byte[] expectedTargetFileBytes;

    // Called by sub class
    private CommandExecutor commandExecutor;
    private RuntimeExec origTransformCommand;
    private RuntimeExec origCheckCommand;

    protected void setMockExternalCommandsOnTransformer(CommandExecutor commandExecutor, RuntimeExec mockTransformCommand,
            RuntimeExec mockCheckCommand)
    {
        this.commandExecutor = commandExecutor;
        origTransformCommand = (RuntimeExec) ReflectionTestUtils.getField(commandExecutor, "transformCommand");
        origCheckCommand = (RuntimeExec) ReflectionTestUtils.getField(commandExecutor, "transformCommand");
        ReflectionTestUtils.setField(commandExecutor, "transformCommand", mockTransformCommand);
        ReflectionTestUtils.setField(commandExecutor, "checkCommand", mockCheckCommand);
    }

    protected void resetExternalCommandsOnTransformer()
    {
        ReflectionTestUtils.setField(commandExecutor, "transformCommand", origTransformCommand);
        ReflectionTestUtils.setField(commandExecutor, "checkCommand", origCheckCommand);
    }

    protected void mockTransformCommand(String sourceExtension,
            String targetExtension, String sourceMimetype,
            boolean readTargetFileBytes) throws IOException
    {}

    protected void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest)
    {}

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

            }
            catch (Exception e)
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
            Files.copy(classLoader.getResourceAsStream(testFilename), testFile.toPath(), REPLACE_EXISTING);
        }

        return testFileUrl == null ? null : testFile;
    }

    protected MockMultipartHttpServletRequestBuilder mockMvcRequest(String url, MockMultipartFile sourceFile, String... params)
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

    private MockMultipartHttpServletRequestBuilder mockMvcRequestWithoutMockMultipartFile(String url, String... params)
    {
        MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(url);

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

    private MockMultipartHttpServletRequestBuilder mockMvcRequestWithMockMultipartFile(String url, MockMultipartFile sourceFile,
            String... params)
    {
        MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(url).file(sourceFile);

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

    public static void resetProbeForTesting(ProbeTransform probe)
    {
        ReflectionTestUtils.setField(probe, "probeCount", 0);
        ReflectionTestUtils.setField(probe, "transCount", 0);
        ReflectionTestUtils.setField(probe, "normalTime", 0);
        ReflectionTestUtils.setField(probe, "maxTime", Long.MAX_VALUE);
        ReflectionTestUtils.setField(probe, "nextTransformTime", 0);

        ((AtomicBoolean) ReflectionTestUtils.getField(probe, "initialised")).set(false);
        ((AtomicBoolean) ReflectionTestUtils.getField(probe, "readySent")).set(false);
        ((AtomicLong) ReflectionTestUtils.getField(probe, "transformCount")).set(0);
        ((AtomicBoolean) ReflectionTestUtils.getField(probe, "die")).set(false);
    }

    @Test
    public void simpleTransformTest() throws Exception
    {
        mockMvc.perform(
                mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile))
                .andExpect(status().isOk())
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''transform." + targetExtension));
    }

    @Test
    public void dotDotSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "../quick." + sourceExtension, sourceMimetype, sourceFileBytes);

        mockMvc.perform(
                mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile))
                .andExpect(status().isOk())
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''transform." + targetExtension));
    }

    @Test
    public void noExtensionSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "../quick", sourceMimetype, sourceFileBytes);

        mockMvc.perform(
                mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile))
                .andExpect(status().isOk())
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''transform." + targetExtension));
    }

    @Test
    public void calculateMaxTime() throws Exception
    {
        ProbeTransform probeTransform = controller.getProbeTransform();
        resetProbeForTesting(probeTransform);
        probeTransform.setLivenessPercent(110);

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

        when(sharedFileStoreClient.saveFile(any()))
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

        ResultActions resultActions = mockMvc.perform(
                mockMvcRequest(ENDPOINT_TRANSFORM, null)
                        .param(DIRECT_ACCESS_URL, directUrl))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''transform." + targetExtension));

        if (expectedTargetFileBytes != null)
        {
            resultActions.andExpect(content().bytes(expectedTargetFileBytes));
        }
    }
}
