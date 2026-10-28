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
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transform.common.RequestParamMap.TARGET_MIMETYPE;

import java.nio.charset.StandardCharsets;

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
}
