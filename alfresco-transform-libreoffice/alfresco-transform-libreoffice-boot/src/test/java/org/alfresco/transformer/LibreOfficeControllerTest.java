/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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

import static org.alfresco.transformer.executors.RuntimeExec.ExecutionResult;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.util.StringUtils.getFilenameExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transformer.executors.LibreOfficeJavaExecutor;
import org.alfresco.transformer.model.FileRefEntity;
import org.alfresco.transformer.model.FileRefResponse;
import org.artofsolving.jodconverter.office.OfficeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.annotation.PostConstruct;

/**
 * Test the LibreOfficeController without a server.
 * Super class includes tests for the AbstractTransformerController.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(LibreOfficeControllerTest.class)
public class LibreOfficeControllerTest extends AbstractTransformerControllerTest
{

    private static final String ENGINE_CONFIG_NAME = "libreoffice_engine_config.json";

    @Mock
    private ExecutionResult mockExecutionResult;

    LibreOfficeJavaExecutor javaExecutor;

    @PostConstruct
    private void init()
    {
        javaExecutor = new LibreOfficeJavaExecutor(externalProps.getLibreoffice());
    }

    @SpyBean
    private LibreOfficeController controller;

    @Before
    public void before() throws IOException
    {
        sourceExtension = "doc";
        targetExtension = "pdf";
        sourceMimetype = "application/msword";

        ReflectionTestUtils.setField(controller, "javaExecutor", javaExecutor);

        // The following is based on super.mockTransformCommand(...)
        // This is because LibreOffice used JodConverter rather than a RuntimeExec

        expectedSourceFileBytes = Files.readAllBytes(
            getTestFile("quick." + sourceExtension, true).toPath());
        expectedTargetFileBytes = Files.readAllBytes(
            getTestFile("quick." + targetExtension, true).toPath());
        sourceFile = new MockMultipartFile("file", "quick." + sourceExtension, sourceMimetype,
            expectedSourceFileBytes);

        doAnswer(invocation ->
        {
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
            assertTrue("Source file is not the same",
                Arrays.equals(expectedSourceFileBytes, actualSourceFileBytes));

            return null;
        }).when(javaExecutor).convert(any(), any());
    }

    @Override
    public String getEngineConfigName()
    {
        return ENGINE_CONFIG_NAME;
    }

    @Override
    protected void mockTransformCommand(String sourceExtension, String targetExtension,
        String sourceMimetype, boolean readTargetFileBytes)
    {
        throw new IllegalStateException();
    }

    @Override
    protected AbstractTransformerController getController()
    {
        return controller;
    }

    @Test
    public void badExitCodeTest() throws Exception
    {
        doThrow(OfficeException.class).when(javaExecutor).convert(any(), any());

        mockMvc
            .perform(MockMvcRequestBuilders
                .multipart("/transform")
                .file(sourceFile)
                .param("targetExtension", "xxx"))
            .andExpect(status().is(400))
            .andExpect(status().reason(
                containsString("LibreOffice - LibreOffice server conversion failed:")));
    }

    @Override
    protected void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest)
    {
        transformRequest.setSourceExtension("doc");
        transformRequest.setTargetExtension("pdf");
        transformRequest.setSourceMediaType("application/msword");
        transformRequest.setTargetMediaType(IMAGE_PNG_VALUE);
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
        headers.set(CONTENT_DISPOSITION, "attachment; filename=quick." + sourceExtension);
        ResponseEntity<Resource> response = new ResponseEntity<>(new FileSystemResource(
            sourceFile), headers, OK);

        when(alfrescoSharedFileStoreClient.retrieveFile(sourceFileRef)).thenReturn(response);
        when(alfrescoSharedFileStoreClient.saveFile(any()))
            .thenReturn(new FileRefResponse(new FileRefEntity(targetFileRef)));
        when(mockExecutionResult.getExitValue()).thenReturn(0);

        // Update the Transformation Request with any specific params before sending it
        updateTransformRequestWithSpecificOptions(transformRequest);

        // Serialize and call the transformer
        String tr = objectMapper.writeValueAsString(transformRequest);
        String transformationReplyAsString = mockMvc
            .perform(MockMvcRequestBuilders
                .post("/transform")
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
}
