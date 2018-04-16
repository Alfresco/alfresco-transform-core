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

import org.alfresco.transformer.base.AbstractTransformerController;
import org.alfresco.util.exec.RuntimeExec;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Super class for testing controllers without a server. Includes tests for the AbstractTransformerController itself.
 */
public abstract class AbstractTransformerControllerTest
{
    @Autowired
    protected MockMvc mockMvc;

    @Mock
    private RuntimeExec mockTransformCommand;

    @Mock
    private RuntimeExec mockCheckCommand;

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

    private AbstractTransformerController controller;

    // Called by sub class
    public void mockTransformCommand(AbstractTransformerController controller, String sourceExtension, String targetExtension, String sourceMimetype) throws IOException
    {
        this.controller = controller;
        this.sourceExtension = sourceExtension;
        this.targetExtension = targetExtension;
        this.sourceMimetype = sourceMimetype;

        expectedOptions = null;
        expectedSourceSuffix = null;
        expectedSourceFileBytes = Files.readAllBytes(getTestFile("quick."+sourceExtension, true).toPath());
        expectedTargetFileBytes = Files.readAllBytes(getTestFile("quick."+targetExtension, true).toPath());
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
                    if (testFile != null)
                    {
                        File targetFile = new File(actualTarget);
                        FileChannel source = new FileInputStream(testFile).getChannel();
                        FileChannel target = new FileOutputStream(targetFile).getChannel();
                        target.transferFrom(source, 0, source.size());
                    }
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

    @Test
    public void simpleTransformTest() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension))
                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
    }

    @Test
    public void testDelayTest() throws Exception
    {
        long start = System.currentTimeMillis();
        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension)
                .param("testDelay", "400"))
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
        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", "xxx"))
                .andExpect(status().is(500));
    }

    @Test
    public void badExitCodeTest() throws Exception
    {
        when(mockExecutionResult.getExitValue()).thenReturn(1);

        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", "xxx"))
                .andExpect(status().is(400))
                .andExpect(status().reason(containsString("Transformer exit code was not 0: \nSTDERR")));
    }

    @Test
    // Looks dangerous but is okay as we only use the final filename
    public void dotDotSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "../quick."+sourceExtension, sourceMimetype, expectedSourceFileBytes);

        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension))
                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
    }

    @Test
    // Is okay, as the target filename is built up from the whole source filename and the targetExtenstion
    public void noExtensionSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "../quick", sourceMimetype, expectedSourceFileBytes);

        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension))
                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
    }

    @Test
    // Invalid file name that ends in /
    public void badSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "abc/", sourceMimetype, expectedSourceFileBytes);

        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension))
                .andExpect(status().is(400))
                .andExpect(status().reason(containsString("The source filename was not supplied")));
    }

    @Test
    public void blankSourceFilenameTest() throws Exception
    {
        sourceFile = new MockMultipartFile("file", "", sourceMimetype, expectedSourceFileBytes);

        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension))
                .andExpect(status().is(400))
                .andExpect(status().reason(containsString("The source filename was not supplied")));
    }

    @Test
    public void noTargetExtensionTest() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile))
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
}
