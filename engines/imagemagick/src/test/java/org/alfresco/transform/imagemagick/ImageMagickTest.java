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
package org.alfresco.transform.imagemagick;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.util.StringUtils.getFilenameExtension;

import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import org.alfresco.transform.base.AbstractBaseTest;
import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.executors.RuntimeExec.ExecutionResult;
import org.alfresco.transform.base.model.FileRefEntity;
import org.alfresco.transform.base.model.FileRefResponse;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.imagemagick.transformers.ImageMagickTransformer;

/**
 * Test ImageMagick with mocked external command.
 */
public class ImageMagickTest extends AbstractBaseTest
{
    private static String PREFIX_IMAGE = "image/";

    @Autowired
    private ImageMagickTransformer imageMagickTransformer;

    @Mock
    protected ExecutionResult mockExecutionResult;
    @Mock
    protected RuntimeExec mockTransformCommand;
    @Mock
    protected RuntimeExec mockCheckCommand;
    @Value("${transform.core.imagemagick.exe}")
    protected String EXE;
    @Value("${transform.core.imagemagick.dyn}")
    protected String DYN;
    @Value("${transform.core.imagemagick.root}")
    protected String ROOT;
    @Value("${transform.core.imagemagick.coders}")
    protected String CODERS;
    @Value("${transform.core.imagemagick.config}")
    protected String CONFIG;

    @BeforeEach
    public void before() throws IOException
    {
        setMockExternalCommandsOnTransformer(imageMagickTransformer, mockTransformCommand, mockCheckCommand);
        mockTransformCommand("jpg", "png", "image/jpeg", true);
    }

    @AfterEach
    public void after()
    {
        resetExternalCommandsOnTransformer();
    }

    @Override
    protected MockHttpServletRequestBuilder mockMvcRequest(String url, MockMultipartFile sourceFile, String... params)
    {
        final MockHttpServletRequestBuilder builder = super.mockMvcRequest(url, sourceFile, params)
                .param("targetMimetype", targetMimetype)
                .param("sourceMimetype", sourceMimetype);
        return builder;
    }

    @Override
    public void mockTransformCommand(String sourceExtension,
            String targetExtension, String sourceMimetype,
            boolean readTargetFileBytes) throws IOException
    {
        this.sourceExtension = sourceExtension;
        this.targetExtension = targetExtension;
        this.sourceMimetype = sourceMimetype;
        this.targetMimetype = PREFIX_IMAGE + ("jpg".equals(targetExtension) ? "jpeg" : targetExtension);

        expectedOptions = null;
        expectedSourceSuffix = null;
        sourceFileBytes = readTestFile(sourceExtension);
        expectedTargetFileBytes = readTargetFileBytes ? readTestFile(targetExtension) : null;
        sourceFile = new MockMultipartFile("file", "quick." + sourceExtension, sourceMimetype, sourceFileBytes);

        when(mockTransformCommand.execute(any(), anyLong())).thenAnswer(
                (Answer<RuntimeExec.ExecutionResult>) invocation -> {
                    Map<String, String> actualProperties = invocation.getArgument(0);
                    assertEquals(3, actualProperties.size(), "There should be 3 properties");

                    String actualOptions = actualProperties.get("options");
                    String actualSource = actualProperties.get("source");
                    String actualTarget = actualProperties.get("target");
                    String actualTargetExtension = getFilenameExtension(actualTarget);

                    assertNotNull(actualSource);
                    assertNotNull(actualTarget);
                    if (expectedSourceSuffix != null)
                    {
                        assertTrue(actualSource.endsWith(expectedSourceSuffix),
                                "The source file \"" + actualSource + "\" should have ended in \"" + expectedSourceSuffix + "\"");
                        actualSource = actualSource.substring(0, actualSource.length() - expectedSourceSuffix.length());
                    }

                    assertNotNull(actualOptions);
                    if (expectedOptions != null)
                    {
                        assertEquals(expectedOptions, actualOptions, "expectedOptions");
                    }

                    Long actualTimeout = invocation.getArgument(1);
                    assertNotNull(actualTimeout);
                    if (expectedTimeout != null)
                    {
                        assertEquals(expectedTimeout, actualTimeout, "expectedTimeout");
                    }

                    // Copy a test file into the target file location if it exists
                    int i = actualTarget.lastIndexOf('_');
                    if (i >= 0)
                    {
                        String testFilename = actualTarget.substring(i + 1);
                        File testFile = getTestFile(testFilename, false);
                        File targetFile = new File(actualTarget);
                        generateTargetFileFromResourceFile(actualTargetExtension, testFile,
                                targetFile);
                    }

                    // Check the supplied source file has not been changed.
                    byte[] actualSourceFileBytes = Files.readAllBytes(new File(actualSource).toPath());
                    assertTrue(Arrays.equals(sourceFileBytes, actualSourceFileBytes),
                            "Source file is not the same");

                    return mockExecutionResult;
                });

        when(mockExecutionResult.getExitValue()).thenReturn(0);
        when(mockExecutionResult.getStdErr()).thenReturn("STDERROR");
        when(mockExecutionResult.getStdOut()).thenReturn("STDOUT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"North", "NorthEast", "East", "SouthEast", "South", "SouthWest", "West", "NorthWest", "Center"})
    public void cropGravityGoodTest(String value) throws Exception
    {
        expectedOptions = "-auto-orient " + "-gravity " + value + " +repage";
        mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(ENDPOINT_TRANSFORM)
                        .file(sourceFile)
                        .param("targetExtension", targetExtension)
                        .param("targetMimetype", targetMimetype)
                        .param("sourceMimetype", sourceMimetype)
                        .param("cropGravity", value))
                .andExpect(status().isOk())
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''transform." + targetExtension));
    }

    @Test
    public void cropGravityBadTest() throws Exception
    {
        mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(ENDPOINT_TRANSFORM)
                        .file(sourceFile)
                        .param("targetExtension", targetExtension)
                        .param("targetMimetype", targetMimetype)
                        .param("sourceMimetype", sourceMimetype)
                        .param("cropGravity", "badValue"))
                .andExpect(status().is(BAD_REQUEST.value()));
    }

    @Test
    public void optionsTest() throws Exception
    {
        expectedOptions = "-alpha remove -gravity SouthEast -crop 123x456%+90+12 +repage -thumbnail 321x654%!";
        expectedSourceSuffix = "[2-3]";
        mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(ENDPOINT_TRANSFORM)
                        .file(sourceFile)
                        .param("targetExtension", targetExtension)
                        .param("targetMimetype", targetMimetype)
                        .param("sourceMimetype", sourceMimetype)

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
                        .param("maintainAspectRatio", "false"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''transform." + targetExtension));
    }

    @Test
    public void optionsNegateBooleansTest() throws Exception
    {
        expectedOptions = "-auto-orient -gravity SouthEast -crop 123x456+90+12 +repage -resize 321x654>";
        expectedSourceSuffix = "[2-3]";
        mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(ENDPOINT_TRANSFORM)
                        .file(sourceFile)
                        .param("targetExtension", targetExtension)
                        .param("targetMimetype", targetMimetype)
                        .param("sourceMimetype", sourceMimetype)

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
                        .param("maintainAspectRatio", "true"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''transform." + targetExtension));
    }

    @Test
    public void deprecatedCommandOptionsTest() throws Exception
    {
        // Example of why the commandOptions parameter is a bad idea.
        expectedOptions = "( horrible command / ); -auto-orient -resize 321x654";
        mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(ENDPOINT_TRANSFORM)
                        .file(sourceFile)
                        .param("targetExtension", targetExtension)
                        .param("targetMimetype", targetMimetype)
                        .param("sourceMimetype", sourceMimetype)
                        .param("thumbnail", "false")
                        .param("resizeWidth", "321")
                        .param("resizeHeight", "654")
                        .param("commandOptions", "( horrible command / );"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''transform." + targetExtension));
    }

    @Override
    protected void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest)
    {
        transformRequest.setSourceExtension("png");
        transformRequest.setTargetExtension("png");
        transformRequest.setSourceMediaType(IMAGE_PNG_VALUE);
        transformRequest.setTargetMediaType(IMAGE_PNG_VALUE);
    }

    @Test
    public void badExitCodeTest() throws Exception
    {
        when(mockExecutionResult.getExitValue()).thenReturn(1);

        mockMvc.perform(mockMvcRequest(ENDPOINT_TRANSFORM, sourceFile, "targetExtension", "xxx"))
                .andExpect(status().is(BAD_REQUEST.value()))
                .andExpect(status()
                        .reason(containsString("Transformer exit code was not 0: \nSTDERR")));
    }

    @Test
    public void testPojoTransform() throws Exception
    {
        // Files
        String sourceFileRef = UUID.randomUUID().toString();
        File sourceFile = getTestFile("quick." + sourceExtension, true);
        String targetFileRef = UUID.randomUUID().toString();

        TransformRequest transformRequest = createTransformRequest(sourceFileRef, sourceFile);

        // HTTP Request
        HttpHeaders headers = new HttpHeaders();
        headers.set(CONTENT_DISPOSITION, "attachment; filename=quick." + sourceExtension);
        ResponseEntity<Resource> response = new ResponseEntity<>(new FileSystemResource(
                sourceFile), headers, OK);

        when(sharedFileStoreClient.retrieveFile(sourceFileRef)).thenReturn(response);
        when(sharedFileStoreClient.saveFile(any()))
                .thenReturn(new FileRefResponse(new FileRefEntity(targetFileRef)));
        when(mockExecutionResult.getExitValue()).thenReturn(0);

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

        TransformReply transformReply = objectMapper.readValue(transformationReplyAsString,
                TransformReply.class);

        // Assert the reply
        assertEquals(transformRequest.getRequestId(), transformReply.getRequestId());
        assertEquals(transformRequest.getClientData(), transformReply.getClientData());
        assertEquals(transformRequest.getSchema(), transformReply.getSchema());
    }

    @Test
    public void testOverridingExecutorPaths()
    {
        // System test property values can me modified in the pom.xml
        assertEquals(EXE, System.getProperty("IMAGEMAGICK_EXE"));
        assertEquals(DYN, System.getProperty("IMAGEMAGICK_DYN"));
        assertEquals(ROOT, System.getProperty("IMAGEMAGICK_ROOT"));
    }
}
