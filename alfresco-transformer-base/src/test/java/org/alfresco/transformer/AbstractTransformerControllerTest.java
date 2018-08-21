/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transformer;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transformer.model.FileRefEntity;
import org.alfresco.transformer.model.FileRefResponse;
import org.alfresco.util.exec.RuntimeExec;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Super class for testing controllers without a server. Includes tests for the AbstractTransformerController itself.
 */
public abstract class AbstractTransformerControllerTest
{
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Mock
    private RuntimeExec mockTransformCommand;

    @Mock
    private RuntimeExec mockCheckCommand;

    @Mock
    protected AlfrescoSharedFileStoreClient alfrescoSharedFileStoreClient;

    @Mock
    private RuntimeExec.ExecutionResult mockExecutionResult;

    protected String sourceExtension;
    protected String targetExtension;
    protected String sourceMimetype;

    protected MockMultipartFile sourceFile;
    protected String expectedOptions;
    protected String expectedSourceSuffix;
    protected Long expectedTimeout = 0L;
    protected byte[] expectedSourceFileBytes;
    protected byte[] expectedTargetFileBytes;

    protected AbstractTransformerController controller;

    @Before
    public void before() throws Exception
    {
    }

    // Called by sub class
    public void mockTransformCommand(AbstractTransformerController controller, String sourceExtension,
                                     String targetExtension, String sourceMimetype,
                                     boolean readTargetFileBytes) throws IOException
    {
        this.controller = controller;
        this.sourceExtension = sourceExtension;
        this.targetExtension = targetExtension;
        this.sourceMimetype = sourceMimetype;

        expectedOptions = null;
        expectedSourceSuffix = null;
        expectedSourceFileBytes = readTestFile(sourceExtension);
        expectedTargetFileBytes = readTargetFileBytes ? readTestFile(targetExtension) : null;
        sourceFile = new MockMultipartFile("file", "quick."+sourceExtension, sourceMimetype, expectedSourceFileBytes);

        controller.setTransformCommand(mockTransformCommand);
        controller.setCheckCommand(mockCheckCommand);

        when(mockTransformCommand.execute(anyObject(), anyLong())).thenAnswer(new Answer<RuntimeExec.ExecutionResult>()
        {
            public RuntimeExec.ExecutionResult answer(InvocationOnMock invocation) throws Throwable
            {
                Map<String, String> actualProperties = invocation.getArgumentAt(0, Map.class);
                assertEquals("There should be 3 properties", 3, actualProperties.size());

                String actualOptions = actualProperties.get("options");
                String actualSource = actualProperties.get("source");
                String actualTarget = actualProperties.get("target");
                String actualTargetExtension = StringUtils.getFilenameExtension(actualTarget);

                assertNotNull(actualSource);
                assertNotNull(actualTarget);
                if (expectedSourceSuffix != null)
                {
                    assertTrue("The source file \""+actualSource+"\" should have ended in \""+expectedSourceSuffix+"\"", actualSource.endsWith(expectedSourceSuffix));
                    actualSource = actualSource.substring(0, actualSource.length()-expectedSourceSuffix.length());
                }

                assertNotNull(actualOptions);
                if (expectedOptions != null)
                {
                    assertEquals("expectedOptions", expectedOptions, actualOptions);
                }

                Long actualTimeout = invocation.getArgumentAt(1, Long.class);
                assertNotNull(actualTimeout);
                if (expectedTimeout != null)
                {
                    assertEquals("expectedTimeout", expectedTimeout, actualTimeout);
                }

                // Copy a test file into the target file location if it exists
                int i = actualTarget.lastIndexOf('_');
                if (i >= 0)
                {
                    String testFilename = actualTarget.substring(i+1);
                    File testFile = getTestFile(testFilename, false);
                    File targetFile = new File(actualTarget);
                    generateTargetFileFromResourceFile(actualTargetExtension, testFile,
                        targetFile);
                }

                // Check the supplied source file has not been changed.
                byte[] actualSourceFileBytes = Files.readAllBytes(new File(actualSource).toPath());
                assertTrue("Source file is not the same", Arrays.equals(expectedSourceFileBytes, actualSourceFileBytes));

                return mockExecutionResult;
            }
        });

        when(mockExecutionResult.getExitValue()).thenReturn(0);
        when(mockExecutionResult.getStdErr()).thenReturn("STDERROR");
        when(mockExecutionResult.getStdOut()).thenReturn("STDOUT");
    }

    /**
     * This method ends up being the core of the mock.
     * It copies content from an existing file in the resources folder to the desired location
     * in order to simulate a successful transformation.
     *
     * @param actualTargetExtension Requested extension.
     * @param testFile The test file (transformed) - basically the result.
     * @param targetFile The location where the content from the testFile should be copied
     * @throws IOException in case of any errors.
     */
    void generateTargetFileFromResourceFile(String actualTargetExtension, File testFile,
        File targetFile) throws IOException
    {
        if (testFile != null)
        {
            FileChannel source = new FileInputStream(testFile).getChannel();
            FileChannel target = new FileOutputStream(targetFile).getChannel();
            target.transferFrom(source, 0, source.size());
        }
        else
        {
            testFile = getTestFile("quick." + actualTargetExtension, false);
            if (testFile != null)
            {
                FileChannel source = new FileInputStream(testFile).getChannel();
                FileChannel target = new FileOutputStream(targetFile).getChannel();
                target.transferFrom(source, 0, source.size());
            }
        }
    }

    protected byte[] readTestFile(String extension) throws IOException
    {
        return Files.readAllBytes(getTestFile("quick."+extension, true).toPath());
    }

    protected File getTestFile(String testFilename, boolean required) throws IOException
    {
        ClassLoader classLoader = getClass().getClassLoader();
        URL testFileUrl = classLoader.getResource(testFilename);
        if (required && testFileUrl == null)
        {
            throw new IOException("The test file "+testFilename+" does not exist in the resources directory");
        }
        return testFileUrl == null ? null : new File(testFileUrl.getFile());
    }

    protected MockHttpServletRequestBuilder mockMvcRequest(String url, MockMultipartFile sourceFile, String... params)
    {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.fileUpload("/transform").file(sourceFile);

        if (params.length % 2 != 0)
        {
            throw new IllegalArgumentException("each param should have a name and value.");
        }
        for (int i=0; i<params.length; i+=2)
        {
            builder = builder.param(params[i], params[i+1]);
        }

        return builder;
    }

    @Test
    public void simpleTransformTest() throws Exception
    {
        mockMvc.perform(mockMvcRequest("/transform", sourceFile, "targetExtension", targetExtension))
                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
    }

    @Test
    public void testDelayTest() throws Exception
    {
        long start = System.currentTimeMillis();
        mockMvc.perform(mockMvcRequest("/transform", sourceFile, "targetExtension", targetExtension, "testDelay", "400"))
                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
        long ms = System.currentTimeMillis()- start;
        System.out.println("Transform incluing test delay was "+ms);
        assertTrue("Delay sending the result back was too small "+ms, ms >= 400);
        assertTrue("Delay sending the result back was too big "+ms, ms <= 500);
    }

    @Test
    public void noTargetFileTest() throws Exception
    {
        mockMvc.perform(mockMvcRequest("/transform", sourceFile, "targetExtension", "xxx"))
                .andExpect(status().is(500));
    }

    @Test
    public void badExitCodeTest() throws Exception
    {
        when(mockExecutionResult.getExitValue()).thenReturn(1);

        mockMvc.perform(mockMvcRequest("/transform", sourceFile, "targetExtension", "xxx"))
                .andExpect(status().is(400))
                .andExpect(status().reason(containsString("Transformer exit code was not 0: \nSTDERR")));
    }

    @Test
    // Looks dangerous but is okay as we only use the final filename
    public void dotDotSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "../quick."+sourceExtension, sourceMimetype, expectedSourceFileBytes);

        mockMvc.perform(mockMvcRequest("/transform", sourceFile, "targetExtension", targetExtension))
                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
    }

    @Test
    // Is okay, as the target filename is built up from the whole source filename and the targetExtenstion
    public void noExtensionSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "../quick", sourceMimetype, expectedSourceFileBytes);

        mockMvc.perform(mockMvcRequest("/transform", sourceFile, "targetExtension", targetExtension))
                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
    }

    @Test
    // Invalid file name that ends in /
    public void badSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "abc/", sourceMimetype, expectedSourceFileBytes);

        mockMvc.perform(mockMvcRequest("/transform", sourceFile, "targetExtension", targetExtension))
                .andExpect(status().is(400))
                .andExpect(status().reason(containsString("The source filename was not supplied")));
    }

    @Test
    public void blankSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "", sourceMimetype, expectedSourceFileBytes);

        mockMvc.perform(mockMvcRequest("/transform", sourceFile, "targetExtension", targetExtension))
                .andExpect(status().is(400))
                .andExpect(status().reason(containsString("The source filename was not supplied")));
    }

    @Test
    public void noTargetExtensionTest() throws Exception
    {
        mockMvc.perform(mockMvcRequest("/transform", sourceFile))
                .andExpect(status().is(400))
                .andExpect(status().reason(containsString("Request parameter targetExtension is missing")));
    }

//    @Test
//    // Not a real test, but helpful for trying out the duration times in log code.
//    public void testTimes() throws InterruptedException
//    {
//        LogEntry.start();
//        Thread.sleep(50);
//        LogEntry.setSource("test File", 1234);
//        Thread.sleep(200);
//        LogEntry.setStatusCodeAndMessage(200, "Success");
//        LogEntry.addDelay(2000L);
//        for (LogEntry logEntry: LogEntry.getLog())
//        {
//            String str = logEntry.getDuration();
//            System.out.println(str);
//        }
//    }

    @Test
    public void calculateMaxTime() throws Exception
    {
        ProbeTestTransform probeTestTransform = controller.getProbeTestTransform();
        probeTestTransform.livenessPercent = 110;

        long [][] values = new long[][] {
                {5000, 0, Long.MAX_VALUE}, // 1st transform is ignored
                {1000, 1000, 2100},        // 1000 + 1000*1.1
                {3000, 2000, 4200},        // 2000 + 2000*1.1
                {2000, 2000, 4200},
                {6000, 3000, 6300},
                {8000, 4000, 8400},
                {4444, 4000, 8400},        // no longer in the first few, so normal and max times don't change
                {5555, 4000, 8400}
        };

        for (long[] v: values)
        {
            long time = v[0];
            long expectedNormalTime = v[1];
            long expectedMaxTime = v[2];

            probeTestTransform.calculateMaxTime(time, true);
            assertEquals("", expectedNormalTime, probeTestTransform.normalTime);
            assertEquals("", expectedMaxTime, probeTestTransform.maxTime);
        }
    }

    @Test
    public void testPojoTransform() throws Exception
    {
        // Files
        String sourceFileRef = UUID.randomUUID().toString();
        File sourceFile = getTestFile("quick." + sourceExtension, true);
        String targetFileRef = UUID.randomUUID().toString();


        // Transformation Request POJO
        TransformRequest transformRequest = new TransformRequest();
        transformRequest.setRequestId("1");
        transformRequest.setSchema(1);
        transformRequest.setClientData("Alfresco Digital Business Platform");
        transformRequest.setTransformationRequestOptions(new HashMap<>());
        transformRequest.setSourceReference(sourceFileRef);
        transformRequest.setSourceExtension(sourceExtension);
        transformRequest.setSourceSize(sourceFile.length());
        transformRequest.setTargetExtension(targetExtension);

        // HTTP Request
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quick." + sourceExtension);
        ResponseEntity<Resource> response = new ResponseEntity<>(new FileSystemResource(
            sourceFile), headers, HttpStatus.OK);

        when(alfrescoSharedFileStoreClient.retrieveFile(sourceFileRef)).thenReturn(response);
        when(alfrescoSharedFileStoreClient.saveFile(any())).thenReturn(new FileRefResponse(new FileRefEntity(targetFileRef)));
        when(mockExecutionResult.getExitValue()).thenReturn(0);

        // Update the Transformation Request with any specific params before sending it
        updateTransformRequestWithSpecificOptions(transformRequest);

        // Serialize and call the transformer
        String tr = objectMapper.writeValueAsString(transformRequest);
        String transformationReplyAsString = mockMvc.perform(MockMvcRequestBuilders.post("/transform")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).content(tr))
            .andExpect(status().is(HttpStatus.CREATED.value()))
            .andReturn().getResponse().getContentAsString();

        TransformReply transformReply = objectMapper.readValue(transformationReplyAsString, TransformReply.class);

        // Assert the reply
        assertEquals(transformRequest.getRequestId(), transformReply.getRequestId());
        assertEquals(transformRequest.getClientData(), transformReply.getClientData());
        assertEquals(transformRequest.getSchema(), transformReply.getSchema());
    }

    @Test
    public void testEmptyPojoTransform() throws Exception
    {
        // Transformation Request POJO
        TransformRequest transformRequest = new TransformRequest();

        // Serialize and call the transformer
        String tr = objectMapper.writeValueAsString(transformRequest);
        String transformationReplyAsString = mockMvc.perform(MockMvcRequestBuilders.post("/transform")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).content(tr))
            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
            .andReturn().getResponse().getContentAsString();

        TransformReply transformReply = objectMapper.readValue(transformationReplyAsString, TransformReply.class);

        // Assert the reply
        assertEquals(HttpStatus.BAD_REQUEST.value(), transformReply.getStatus());
    }

    protected abstract void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest);
}
