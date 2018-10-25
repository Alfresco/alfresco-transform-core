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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transformer.executors.ImageMagickCommandExecutor;
import org.alfresco.transformer.model.FileRefEntity;
import org.alfresco.transformer.model.FileRefResponse;
import org.alfresco.util.exec.RuntimeExec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.StringUtils;

/**
 * Test the ImageMagickController without a server.
 * Super class includes tests for the AbstractTransformerController.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(ImageMagickController.class)
public class ImageMagickControllerTest extends AbstractTransformerControllerTest
{
    @Mock
    private RuntimeExec.ExecutionResult mockExecutionResult;

    @Mock
    private RuntimeExec mockTransformCommand;

    @Mock
    private RuntimeExec mockCheckCommand;

    @SpyBean
    private ImageMagickCommandExecutor commandExecutor;
    
    @SpyBean
    private ImageMagickController controller;

    @Before
    public void before() throws IOException
    {
        commandExecutor.setTransformCommand(mockTransformCommand);
        commandExecutor.setCheckCommand(mockCheckCommand);

        mockTransformCommand("jpg", "png", "image/jpg", true);
    }

    @Override
    protected void mockTransformCommand(String sourceExtension,
        String targetExtension, String sourceMimetype,
        boolean readTargetFileBytes) throws IOException
    {
        this.sourceExtension = sourceExtension;
        this.targetExtension = targetExtension;
        this.sourceMimetype = sourceMimetype;

        expectedOptions = null;
        expectedSourceSuffix = null;
        expectedSourceFileBytes = readTestFile(sourceExtension);
        expectedTargetFileBytes = readTargetFileBytes ? readTestFile(targetExtension) : null;
        sourceFile = new MockMultipartFile("file", "quick." + sourceExtension, sourceMimetype, expectedSourceFileBytes);

        when(mockTransformCommand.execute(any(), anyLong())).thenAnswer(
            (Answer<RuntimeExec.ExecutionResult>) invocation -> {
                Map<String, String> actualProperties = invocation.getArgument(0);
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

                Long actualTimeout = invocation.getArgument(1);
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
            });

        when(mockExecutionResult.getExitValue()).thenReturn(0);
        when(mockExecutionResult.getStdErr()).thenReturn("STDERROR");
        when(mockExecutionResult.getStdOut()).thenReturn("STDOUT");
    }

    
    @Override
    protected AbstractTransformerController getController()
    {
        return controller;
    }

    @Test
    public void cropGravityGoodTest() throws Exception
    {
        for (String value: new String[] {"North", "NorthEast", "East", "SouthEast", "South", "SouthWest", "West", "NorthWest", "Center"})
        {
            expectedOptions = "-gravity "+value+" +repage";
            mockMvc.perform(MockMvcRequestBuilders.multipart("/transform")
                    .file(sourceFile)
                    .param("targetExtension", targetExtension)
                    .param("cropGravity", value))
                    .andExpect(status().is(200))
                    .andExpect(content().bytes(expectedTargetFileBytes))
                    .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
        }
    }

    @Test
    public void cropGravityBadTest() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension)
                .param("cropGravity", "badValue"))
                .andExpect(status().is(400));
    }

    @Test
    public void optionsTest() throws Exception
    {
        expectedOptions = "-alpha remove -gravity SouthEast -crop 123x456%+90+12 +repage -thumbnail 321x654%!";
        expectedSourceSuffix = "[2-3]";
        mockMvc.perform(MockMvcRequestBuilders.multipart("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension)

                .param("startPage", "2")
                .param("endPage", "3")

                .param("alphaRemove", "true")
                .param("autoOrient", "false")

                .param("cropGravity", "SouthEast")
                .param("cropWidth", "123")
                .param("cropHeight", "456")
                .param("cropPercentage", "true")
                .param("cropXOffset", "90")
                .param("cropYOffset", "12")

                .param("thumbnail", "true")
                .param("resizeWidth", "321")
                .param("resizeHeight", "654")
                .param("resizePercentage", "true")
                .param("allowEnlargement", "true")
                .param("maintainAspectRatio", "true"))

                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
    }

    @Test
    public void optionsNegateBooleansTest() throws Exception
    {
        expectedOptions = "-auto-orient -gravity SouthEast -crop 123x456+90+12 +repage -resize 321x654>";
        expectedSourceSuffix = "[2-3]";
        mockMvc.perform(MockMvcRequestBuilders.multipart("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension)

                .param("startPage", "2")
                .param("endPage", "3")

                .param("alphaRemove", "false")
                .param("autoOrient", "true")

                .param("cropGravity", "SouthEast")
                .param("cropWidth", "123")
                .param("cropHeight", "456")
                .param("cropPercentage", "false")
                .param("cropXOffset", "90")
                .param("cropYOffset", "12")

                .param("thumbnail", "false")
                .param("resizeWidth", "321")
                .param("resizeHeight", "654")
                .param("resizePercentage", "false")
                .param("allowEnlargement", "false")
                .param("maintainAspectRatio", "false"))

                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
    }

    @Test
    public void deprecatedCommandOptionsTest() throws Exception
    {
        // Example of why the commandOptions parameter is a bad idea.
        expectedOptions = "( horrible command / ); -resize 321x654>";
        mockMvc.perform(MockMvcRequestBuilders.multipart("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension)
                .param("thumbnail", "false")
                .param("resizeWidth", "321")
                .param("resizeHeight", "654")
                .param("commandOptions", "( horrible command / );"))
                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
    }

    @Override
    protected void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest)
    {
        transformRequest.setSourceExtension("png");
        transformRequest.setTargetExtension("png");
        transformRequest.setSourceMediaType(MediaType.IMAGE_PNG_VALUE);
        transformRequest.setTargetMediaType(MediaType.IMAGE_PNG_VALUE);
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
        transformRequest.setTransformRequestOptions(new HashMap<>());
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

}
