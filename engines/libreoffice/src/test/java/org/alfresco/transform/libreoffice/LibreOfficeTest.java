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
package org.alfresco.transform.libreoffice;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.util.StringUtils.getFilenameExtension;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transform.common.RequestParamMap.TARGET_EXTENSION;
import static org.alfresco.transform.common.RequestParamMap.TARGET_MIMETYPE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.UUID;

import org.artofsolving.jodconverter.office.OfficeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import org.alfresco.transform.base.AbstractBaseTest;
import org.alfresco.transform.base.executors.RuntimeExec.ExecutionResult;
import org.alfresco.transform.base.model.FileRefEntity;
import org.alfresco.transform.base.model.FileRefResponse;
import org.alfresco.transform.base.registry.CustomTransformers;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.libreoffice.transformers.LibreOfficeTransformer;

/**
 * Test LibreOffice with mocked external command.
 */
@AutoConfigureMockMvc
public class LibreOfficeTest extends AbstractBaseTest
{
    protected static String targetMimetype = MIMETYPE_PDF;

    @MockitoBean
    private LibreOfficeTransformer libreOfficeTransformer;
    @Autowired
    private CustomTransformers customTransformers;

    @Autowired
    private LibreOfficeTransformer spyLibreOfficeTransformer;
    @MockitoBean
    protected ExecutionResult mockExecutionResult;

    @Value("${transform.core.libreoffice.path}")
    private String execPath;
    @Value("${transform.core.libreoffice.maxTasksPerProcess}")
    private String maxTasksPerProcess;
    @Value("${transform.core.libreoffice.timeout}")
    private String timeout;
    @Value("${transform.core.libreoffice.portNumbers}")
    private String portNumbers;
    @Value("${transform.core.libreoffice.templateProfileDir}")
    private String templateProfileDir;
    @Value("${transform.core.libreoffice.isEnabled}")
    private String isEnabled;

    @BeforeEach
    public void before() throws IOException
    {
        customTransformers.put("libreoffice", spyLibreOfficeTransformer);

        sourceExtension = "doc";
        targetExtension = "pdf";
        sourceMimetype = "application/msword";

        // The following is based on super.mockTransformCommand(...)
        // This is because LibreOffice used JodConverter rather than a RuntimeExec

        sourceFileBytes = Files.readAllBytes(
                getTestFile("quick." + sourceExtension, true).toPath());
        expectedTargetFileBytes = Files.readAllBytes(
                getTestFile("quick." + targetExtension, true).toPath());
        sourceFile = new MockMultipartFile("file", "quick." + sourceExtension, sourceMimetype, sourceFileBytes);

        doAnswer(invocation -> {
            File sourceFile = invocation.getArgument(0);
            File targetFile = invocation.getArgument(1);
            String actualTargetExtension = getFilenameExtension(targetFile.getAbsolutePath());

            assertNotNull(sourceFile);
            assertNotNull(targetFile);

            // Copy a test file into the target file location if it exists
            String actualTarget = targetFile.getAbsolutePath();
            int i = actualTarget.lastIndexOf('_');
            if (i >= 0)
            {
                String testFilename = actualTarget.substring(i + 1);
                File testFile = getTestFile(testFilename, false);
                generateTargetFileFromResourceFile(actualTargetExtension, testFile, targetFile);
            }

            // Check the supplied source file has not been changed.
            byte[] actualSourceFileBytes = Files.readAllBytes(sourceFile.toPath());
            assertTrue(Arrays.equals(sourceFileBytes, actualSourceFileBytes), "Source file is not the same");

            return null;
        }).when(spyLibreOfficeTransformer).convert(any(), any());
    }

    @AfterEach
    public void after() throws IOException
    {
        customTransformers.put("libreoffice", libreOfficeTransformer);
    }

    @Override
    protected MockMultipartHttpServletRequestBuilder mockMvcRequest(String url, MockMultipartFile sourceFile, String... params)
    {
        final MockMultipartHttpServletRequestBuilder builder = super.mockMvcRequest(url, sourceFile, params)
                .param("targetMimetype", targetMimetype)
                .param("sourceMimetype", sourceMimetype);
        return builder;
    }

    @Test
    public void badExitCodeTest() throws Exception
    {
        doThrow(OfficeException.class).when(spyLibreOfficeTransformer).convert(any(), any());

        mockMvc
                .perform(MockMvcRequestBuilders
                        .multipart(ENDPOINT_TRANSFORM)
                        .file(sourceFile)
                        .param(TARGET_EXTENSION, "xxx")
                        .param(SOURCE_MIMETYPE, sourceMimetype)
                        .param(TARGET_MIMETYPE, targetMimetype))
                .andExpect(status().is(400))
                .andExpect(status().reason(
                        containsString("LibreOffice server conversion failed:")));
    }

    @Override
    protected void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest)
    {
        transformRequest.setSourceExtension("doc");
        transformRequest.setTargetExtension("pdf");
        transformRequest.setSourceMediaType("application/msword");
        transformRequest.setTargetMediaType(targetMimetype);
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
        // System test property value can be modified in the pom.xml
        assertEquals(execPath, System.getProperty("LIBREOFFICE_HOME"));
    }

    @Test
    public void testOverridingExecutorMaxTasksPerProcess()
    {
        // System test property value can be modified in the pom.xml
        assertEquals(maxTasksPerProcess, System.getProperty("LIBREOFFICE_MAX_TASKS_PER_PROCESS"));
    }

    @Test
    public void testOverridingExecutorTimeout()
    {
        // System test property value can be modified in the pom.xml
        assertEquals(timeout, System.getProperty("LIBREOFFICE_TIMEOUT"));
    }

    @Test
    public void testOverridingExecutorPortNumbers()
    {
        // System test property value can be modified in the pom.xml
        assertEquals(portNumbers, System.getProperty("LIBREOFFICE_PORT_NUMBERS"));
    }

    @Test
    public void testOverridingExecutorTemplateProfileDir()
    {
        // System test property value can be modified in the pom.xml
        assertEquals(templateProfileDir, System.getProperty("LIBREOFFICE_TEMPLATE_PROFILE_DIR"));
    }

    @Test
    public void testOverridingExecutorIsEnabled()
    {
        // System test property value can be modified in the pom.xml
        assertEquals(isEnabled, System.getProperty("LIBREOFFICE_IS_ENABLED"));
    }

    @Test
    public void testInvalidExecutorMaxTasksPerProcess()
    {
        testInvalidValue("maxTasksPerProcess", "INVALID",
                "LibreOfficeTransformer LIBREOFFICE_MAX_TASKS_PER_PROCESS must have a numeric value");
    }

    @Test
    public void testInvalidExecutorTimeout()
    {
        testInvalidValue("timeout", "INVALID",
                "LibreOfficeTransformer LIBREOFFICE_TIMEOUT must have a numeric value");
    }

    @Test
    public void testInvalidExecutorPortNumbers()
    {
        testInvalidValue("portNumbers", null,
                "LibreOfficeTransformer LIBREOFFICE_PORT_NUMBERS variable cannot be null or empty");
    }

    @Test
    public void testInvalidExecutorIsEnabled()
    {
        testInvalidValue("isEnabled", "INVALID",
                "LibreOfficeTransformer LIBREOFFICE_IS_ENABLED variable must be set to true/false");
    }

    private void testInvalidValue(String fieldName, String invalidValue, String expectedErrorMessage)
    {
        String validValue = (String) ReflectionTestUtils.getField(libreOfficeTransformer, fieldName);
        String errorMessage = "";
        try
        {
            ReflectionTestUtils.setField(libreOfficeTransformer, fieldName, invalidValue);
            ReflectionTestUtils.invokeMethod(libreOfficeTransformer, "createJodConverter");
        }
        catch (IllegalArgumentException e)
        {
            errorMessage = e.getMessage();
        }
        finally
        {
            ReflectionTestUtils.setField(libreOfficeTransformer, fieldName, validValue);
        }

        assertEquals(expectedErrorMessage, errorMessage);
    }
}
