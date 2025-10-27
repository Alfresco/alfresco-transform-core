/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2023 Alfresco Software Limited
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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.alfresco.transform.base.transform.StreamHandlerTest.read;
import static org.alfresco.transform.common.Mimetype.*;
import static org.alfresco.transform.common.RequestParamMap.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import jakarta.jms.Destination;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import org.alfresco.transform.base.fakes.FakeTransformEngineWithFragments;
import org.alfresco.transform.base.fakes.FakeTransformerFragments;
import org.alfresco.transform.base.messaging.TransformReplySender;
import org.alfresco.transform.base.model.FileRefEntity;
import org.alfresco.transform.base.model.FileRefResponse;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.base.sfs.SharedFileStoreClient;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;

@AutoConfigureMockMvc
@SpringBootTest(classes = {org.alfresco.transform.base.Application.class})
@ContextConfiguration(classes = {
        FakeTransformEngineWithFragments.class,
        FakeTransformerFragments.class})
public class FragmentHandlerTest
{
    @Autowired
    private TransformHandler transformHandler;
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    protected SharedFileStoreClient fakeSfsClient;
    @MockBean
    private TransformReplySender transformReplySender;
    @MockBean
    private ProbeTransform probeTransform;

    private void assertFragments(String sourceText, String expectedError, List<String> expectedLines)
    {
        List<Pair<Destination, TransformReply>> replies = new ArrayList<>();
        List<String> lines = new ArrayList<>();

        String sourceReference = UUID.randomUUID().toString();
        String targetReference = UUID.randomUUID().toString();

        when(fakeSfsClient.retrieveFile(any()))
                .thenReturn(new ResponseEntity<>(new ByteArrayResource(sourceText.getBytes(StandardCharsets.UTF_8)),
                        new HttpHeaders(), OK));

        when(fakeSfsClient.saveFile(any()))
                .thenAnswer(invocation -> {
                    lines.add(read(invocation.getArgument(0)));
                    return new FileRefResponse(new FileRefEntity(targetReference));
                });

        doAnswer(invocation -> {
            replies.add(Pair.of(invocation.getArgument(0), invocation.getArgument(1)));
            return null;
        }).when(transformReplySender).send(any(), any());

        TransformRequest request = TransformRequest
                .builder()
                .withRequestId(UUID.randomUUID().toString())
                .withSourceMediaType(MIMETYPE_PDF)
                .withTargetMediaType(MIMETYPE_IMAGE_JPEG)
                .withTargetExtension("jpeg")
                .withSchema(1)
                .withClientData("ACS")
                .withSourceReference(sourceReference)
                .withSourceSize(32L)
                .withInternalContextForTransformEngineTests()
                .build();
        transformHandler.handleMessageRequest(request, Long.MAX_VALUE, null, probeTransform);

        TransformReply lastReply = replies.get(replies.size() - 1).getRight();
        String errorDetails = lastReply.getErrorDetails();
        int status = lastReply.getStatus();
        if (expectedError == null)
        {
            assertNull(errorDetails);
            assertEquals(HttpStatus.CREATED.value(), status);
        }
        else
        {
            assertEquals("Transform failed - " + expectedError, errorDetails);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), status);
        }
        assertEquals(expectedLines, lines);
    }

    @Test
    public void testErrorIfHttp()
    {
        String expectedError = "Fragments may only be sent via message queues. This an http request";
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> mockMvc.perform(
                        MockMvcRequestBuilders.multipart(ENDPOINT_TRANSFORM)
                                .file(new MockMultipartFile("file", null, MIMETYPE_TEXT_PLAIN,
                                        "Start".getBytes(StandardCharsets.UTF_8)))
                                .param(SOURCE_MIMETYPE, MIMETYPE_PDF)
                                .param(TARGET_MIMETYPE, MIMETYPE_IMAGE_JPEG))
                        .andExpect(status().isInternalServerError())
                        .andExpect(status().reason(containsString(expectedError))));
    }

    @Test
    public void testWithoutCallingRespondWithFragment()
    {
        assertFragments("WithoutFragments", null, ImmutableList.of("WithoutFragments"));
    }

    @Test
    public void testSingleRespondWithFragmentCall()
    {
        assertFragments("Finished", null, ImmutableList.of("Finished"));
    }

    @Test
    public void testMultipleFragmentCallsWithFinished()
    {
        assertFragments("line1\nline2\nFinished", null,
                ImmutableList.of("line1", "line2", "Finished"));
    }

    @Test
    public void testMultipleFragmentsCallsWithoutFinish()
    {
        assertFragments("line1\nline2\nline3", null,
                ImmutableList.of("line1", "line2", "line3"));
    }

    @Test
    public void testMultipleFragmentsCallsWithoutSendingLastFragment()
    {
        assertFragments("line1\nline2\nline3\nIgnored", null,
                ImmutableList.of("line1", "line2", "line3"));

    }

    @Test
    public void testNoFragments()
    {
        assertFragments("NullFinished", "No fragments were produced", ImmutableList.of());
    }

    @Test
    public void testEndTooEarlyUsingFinished()
    {
        assertFragments("line1\nFinished\nline3", "Final fragment already sent",
                ImmutableList.of("line1", "Finished"));
    }

    @Test
    public void testEndTooEarlyUsingNull()
    {
        assertFragments("line1\nNull\nline3", "Final fragment already sent",
                ImmutableList.of("line1"));
    }

    @Test
    public void testFinishedAndNull()
    {
        // Able to just ignore the extra null call that request nothing
        assertFragments("line1\nFinished\nNull", null, ImmutableList.of("line1", "Finished"));
    }

    @Test
    public void testNullAndNull()
    {
        // Able to just ignore the extra null call that request nothing
        assertFragments("line1\nNull\nNull", null, ImmutableList.of("line1"));
    }

    @Test
    public void testNullAndFinished()
    {
        assertFragments("line1\nNull\nFinished", "Final fragment already sent",
                ImmutableList.of("line1"));
    }
}
