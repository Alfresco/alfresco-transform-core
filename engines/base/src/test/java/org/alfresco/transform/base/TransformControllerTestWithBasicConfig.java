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
package org.alfresco.transform.base;

import org.alfresco.transform.base.components.TestTransformEngineTwoTransformers;
import org.alfresco.transform.base.components.TestTransformerPdf2Png;
import org.alfresco.transform.base.components.TestTransformerTxT2Pdf;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transform.common.RequestParamMap.TARGET_MIMETYPE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testing base t-engine TransformController functionality.
 */
@AutoConfigureMockMvc
@SpringBootTest(classes={org.alfresco.transform.base.Application.class})
@ContextConfiguration(classes = {
        TestTransformerTxT2Pdf.class,
        TestTransformerPdf2Png.class,
        TestTransformEngineTwoTransformers.class})
public class TransformControllerTestWithBasicConfig
{
    @Autowired
    protected MockMvc mockMvc;

    private void assertGoodTransform(String originalValue, String expectedValue, String sourceMimetype, String targetMimetype,
            String expectedTargetExtension) throws Exception
    {
        mockMvc.perform(
           MockMvcRequestBuilders.multipart(ENDPOINT_TRANSFORM)
                 .file(new MockMultipartFile("file", null, sourceMimetype,
                     originalValue.getBytes(StandardCharsets.UTF_8)))
                 .param(SOURCE_MIMETYPE, sourceMimetype)
                 .param(TARGET_MIMETYPE, targetMimetype))
           .andExpect(request().asyncStarted())
           .andDo(MvcResult::getAsyncResult)
           .andExpect(status().isOk())
           .andExpect(header().string("Content-Disposition",
                   "attachment; filename*=UTF-8''transform." + expectedTargetExtension))
           .andExpect(content().string(expectedValue));
    }

    @Test
    public void singleStepTransform() throws Exception
    {
        assertGoodTransform("Start", "Start -> TxT2Pdf()",
                MIMETYPE_TEXT_PLAIN, MIMETYPE_PDF, "pdf");
    }

    @Test
    public void pipelineTransform() throws Exception
    {
        assertGoodTransform("Start", "Start -> TxT2Pdf() -> Pdf2Png()",
                MIMETYPE_TEXT_PLAIN, MIMETYPE_IMAGE_PNG, "png");
    }
}
