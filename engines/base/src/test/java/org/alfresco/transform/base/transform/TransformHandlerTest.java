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
package org.alfresco.transform.base.transform;

import org.alfresco.transform.base.transform.TransformHandler;
import org.alfresco.transform.base.transform.TransformProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests {@link TransformHandler} and {@link TransformProcess}.
 */
public class TransformHandlerTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransformHandler transformHandler;

//    @Test
//    public void transformRequestUsingDirectAccessUrlTest() throws Exception
//    {
//        // Files
//        String sourceFileRef = UUID.randomUUID().toString();
//        File sourceFile = getTestFile("quick." + sourceExtension, true);
//        String targetFileRef = UUID.randomUUID().toString();
//
//        TransformRequest transformRequest = createTransformRequest(sourceFileRef, sourceFile);
//        Map<String, String> transformRequestOptions = transformRequest.getTransformRequestOptions();
//
//        String directUrl = "file://" + sourceFile.toPath();
//
//        transformRequestOptions.put(DIRECT_ACCESS_URL, directUrl);
//        transformRequest.setTransformRequestOptions(transformRequestOptions);
//
//        when(alfrescoSharedFileStoreClient.saveFile(any()))
//            .thenReturn(new FileRefResponse(new FileRefEntity(targetFileRef)));
//
//        // Update the Transformation Request with any specific params before sending it
//        updateTransformRequestWithSpecificOptions(transformRequest);
//
//        // Serialize and call the transformer
//        String tr = objectMapper.writeValueAsString(transformRequest);
//        String transformationReplyAsString = mockMvc
//                                                 .perform(MockMvcRequestBuilders
//                                                              .post("/transform")
//                                                              .header(ACCEPT, APPLICATION_JSON_VALUE)
//                                                              .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
//                                                              .content(tr))
//                                                 .andExpect(status().is(CREATED.value()))
//                                                 .andReturn().getResponse().getContentAsString();
//
//        TransformReply transformReply = objectMapper.readValue(transformationReplyAsString,
//            TransformReply.class);
//
//        // Assert the reply
//        assertEquals(transformRequest.getRequestId(), transformReply.getRequestId());
//        assertEquals(transformRequest.getClientData(), transformReply.getClientData());
//        assertEquals(transformRequest.getSchema(), transformReply.getSchema());
//    }
//
//    @Test
//    public void httpTransformRequestUsingDirectAccessUrlTest() throws Exception
//    {
//        File dauSourceFile = getTestFile("quick." + sourceExtension, true);
//        String directUrl = "file://" + dauSourceFile.toPath();
//
//        ResultActions resultActions = mockMvc.perform(
//                                                 mockMvcRequest(ENDPOINT_TRANSFORM, null)
//                                                     .param("targetExtension", targetExtension)
//                                                     .param(DIRECT_ACCESS_URL, directUrl))
//                                             .andExpect(status().is(OK.value()));
//
//        if (expectedTargetFileBytes != null)
//        {
//            resultActions.andExpect(content().bytes(expectedTargetFileBytes));
//        }
//    }
}
