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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test the AlfrescoPdfRendererController without a server.
 * Super class includes tests for the AbstractTransformerController.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(AlfrescoPdfRendererController.class)
public class AlfrescoPdfRendererControllerTest extends AbstractTransformerControllerTest
{
    @SpyBean
    private AlfrescoPdfRendererController controller;

    @Before
    public void before() throws IOException
    {
        super.mockTransformCommand(controller, "pdf", "png", "application/pdf", true);
    }

    @Test
    public void optionsTest() throws Exception
    {
        expectedOptions = "--width=321 --height=654 --allow-enlargement --maintain-aspect-ratio --page=2";
        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension)

                .param("page", "2")

                .param("width", "321")
                .param("height", "654")
                .param("allowEnlargement", "true")
                .param("maintainAspectRatio", "true"))

                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
    }

    @Test
    public void optionsNegateBooleansTest() throws Exception
    {
        expectedOptions = "--width=321 --height=654 --page=2";
        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension)

                .param("page", "2")

                .param("width", "321")
                .param("height", "654")
                .param("allowEnlargement", "false")
                .param("maintainAspectRatio", "false"))

                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
    }
}
