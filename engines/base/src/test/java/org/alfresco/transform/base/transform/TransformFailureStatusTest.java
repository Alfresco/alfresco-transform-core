/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2026 Alfresco Software Limited
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.RequestParamMap.DIRECT_ACCESS_URL;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transform.common.RequestParamMap.TARGET_MIMETYPE;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import org.alfresco.transform.base.fakes.FakeTransformEngineWithFailingTransformer;
import org.alfresco.transform.base.fakes.FakeTransformerFails;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;

/**
 * Checks the status a failing transform actually returns over HTTP, rather than what {@link TransformExceptionClassifier} decides in isolation.
 * <p>
 * The T-Router only stops retrying if the classified status survives into the response, because {@code Router.isRecoverableError} looks at nothing but the reply status.
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = {org.alfresco.transform.base.Application.class})
@ContextConfiguration(classes = {
        FakeTransformEngineWithFailingTransformer.class,
        FakeTransformerFails.class})
class TransformFailureStatusTest
{
    /** "corrupt-pdf" is 11 bytes. Claiming 22 makes what the server serves exactly half the content. */
    private static final long HALF_ARRIVED = 22;
    private static final long ALL_ARRIVED = 11;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCorruptZipIsReportedAsUnprocessableEntity()
    {
        assertEquals(UNPROCESSABLE_ENTITY.value(), statusOfTransforming("corrupt-zip"),
                "A corrupt zip is a content failure, so the reply must be terminal rather than retryable.");
    }

    @Test
    void testCorruptPdfIsReportedAsUnprocessableEntity()
    {
        assertEquals(UNPROCESSABLE_ENTITY.value(), statusOfTransforming("corrupt-pdf"),
                "A corrupt pdf is a content failure, so the reply must be terminal rather than retryable.");
    }

    /**
     * The safety net: anything that might be the engine or its infrastructure must stay retryable.
     */
    @Test
    void testInfrastructureFailureIsStillReportedAsInternalServerError()
    {
        assertEquals(INTERNAL_SERVER_ERROR.value(), statusOfTransforming("infrastructure"),
                "An infrastructure failure must stay retryable.");
    }

    private int statusOfTransforming(String content)
    {
        return assertDoesNotThrow(() -> mockMvc.perform(
                MockMvcRequestBuilders.multipart(ENDPOINT_TRANSFORM)
                        .file(new MockMultipartFile("file", null, MIMETYPE_TEXT_PLAIN,
                                content.getBytes(StandardCharsets.UTF_8)))
                        .param(SOURCE_MIMETYPE, MIMETYPE_TEXT_PLAIN)
                        .param(TARGET_MIMETYPE, MIMETYPE_PDF))
                .andReturn().getResponse().getStatus());
    }

    /**
     * A Direct Access URL is read lazily, inside the transform, so a download that dies part way through is reported by the transformer as a broken document. Answering {@code 422} would stop the T-Router retrying a transform that was never given its source, losing it for good. This is the one case where the stream has to overrule the exception.
     */
    @Test
    void testATruncatedDirectAccessUrlDownloadIsReportedAsInternalServerError() throws IOException
    {
        HttpServer server = sourceServer("corrupt-pdf");
        try
        {
            assertEquals(INTERNAL_SERVER_ERROR.value(), statusOfTransformingDirectAccessUrl(server, HALF_ARRIVED),
                    "A source that never arrived in full must stay retryable, whatever the transformer called it.");
        }
        finally
        {
            server.stop(0);
        }
    }

    /**
     * The counterpart, and the reason the check is on the stream rather than on how much the transformer read: a document that arrived in full and was then rejected is still a content failure, so it must not be retried.
     */
    @Test
    void testACompleteDirectAccessUrlDownloadIsStillReportedAsUnprocessableEntity() throws IOException
    {
        HttpServer server = sourceServer("corrupt-pdf");
        try
        {
            assertEquals(UNPROCESSABLE_ENTITY.value(), statusOfTransformingDirectAccessUrl(server, ALL_ARRIVED),
                    "The source arrived in full, so the transformer's verdict on the document must stand.");
        }
        finally
        {
            server.stop(0);
        }
    }

    /**
     * Drives the asynchronous path, which is the one ACS uses for a Direct Access URL and the only one that carries the source size. {@code sourceSize} is what the client says the content is, so serving less than that is a download that did not finish.
     */
    private int statusOfTransformingDirectAccessUrl(HttpServer server, long sourceSize)
    {
        TransformRequest request = TransformRequest.builder()
                .withRequestId("1")
                .withSchema(1)
                .withClientData("Alfresco Digital Business Platform")
                .withSourceReference(UUID.randomUUID().toString())
                .withSourceMediaType(MIMETYPE_TEXT_PLAIN)
                .withSourceExtension("txt")
                .withSourceSize(sourceSize)
                .withTargetMediaType(MIMETYPE_PDF)
                .withTargetExtension("pdf")
                .withTransformRequestOptions(Map.of(DIRECT_ACCESS_URL,
                        "http://localhost:" + server.getAddress().getPort() + "/source"))
                .withInternalContextForTransformEngineTests()
                .build();

        return assertDoesNotThrow(() -> {
            String reply = mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_TRANSFORM)
                    .header(ACCEPT, APPLICATION_JSON_VALUE)
                    .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .content(new ObjectMapper().writeValueAsString(request)))
                    .andReturn().getResponse().getContentAsString();
            return new ObjectMapper().readValue(reply, TransformReply.class).getStatus();
        });
    }

    /**
     * Serves {@code content} in full. Whether that counts as the whole document is decided by the source size on the request, which is how ACS says how big the content is.
     */
    private static HttpServer sourceServer(String content) throws IOException
    {
        final byte[] body = content.getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/source", exchange -> {
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream responseBody = exchange.getResponseBody())
            {
                responseBody.write(body);
            }
            exchange.close();
        });
        server.start();
        return server;
    }
}
