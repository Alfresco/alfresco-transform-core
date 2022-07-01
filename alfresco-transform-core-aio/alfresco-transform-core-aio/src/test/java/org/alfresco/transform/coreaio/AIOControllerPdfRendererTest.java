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
package org.alfresco.transform.coreaio;


import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.alfresco.transform.pdfRenderer.AlfrescoPdfRendererControllerTest;
import org.alfresco.transform.base.executors.Transformer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AIOController.class)
@Import(AIOCustomConfig.class)
/**
 * Test the AIOController PDF Renderer transforms without a server.
 * Super class includes tests for the AbstractTransformerController.
 */
public class AIOControllerPdfRendererTest extends AlfrescoPdfRendererControllerTest
{
    @Autowired AIOTransformRegistry transformRegistry;

    @Override
    protected void setFields()
    {
        ReflectionTestUtils.setField(commandExecutor, "transformCommand", mockTransformCommand);
        ReflectionTestUtils.setField(commandExecutor, "checkCommand", mockCheckCommand);
        //Need to wire in the mocked commandExecutor into the controller...
        Map<String,Transformer> transformers = transformRegistry.getTransformerEngineMapping();
        transformers.replace("pdfrenderer", commandExecutor);
    }

    @Override
    protected MockHttpServletRequestBuilder mockMvcRequest(String url, MockMultipartFile sourceFile,
        String... params)
    {
        final MockHttpServletRequestBuilder builder = super.mockMvcRequest(url, sourceFile, params)
            .param("targetMimetype", targetMimetype)
            .param("sourceMimetype", sourceMimetype);

        return builder;
    }

    @Test
    public void testTestValidity()
    {
        // just test that we are actually testing against the AIOController (instead of MiscController)
        assertTrue(controller instanceof AIOController, "Wrong controller wired for test");
    }
    
    @Test
    @Override
    public void testGetTransformConfigInfo()
    {
        // Ignore the test in super class as the way the AIO transformer provides config is fundamentally different.
    }

    @Test
    @Override
    public void testGetTransformConfigInfoExcludingCoreVersion()
    {
        // Ignore the test in super class as the way the AIO transformer provides config is fundamentally different.
    }

    @Test
    @Override
    public void testGetInfoFromConfigWithDuplicates()
    {
        // Ignore the test in super class as the way the AIO transformer provides config is fundamentally different.

    }

    @Test
    @Override
    public void testGetInfoFromConfigWithEmptyTransformOptions()
    {
        // Ignore the test in super class as the way the AIO transformer provides config is fundamentally different.

    }
    
    @Test
    @Override
    public void testGetInfoFromConfigWithNoTransformOptions()
    {
        // Ignore the test in super class as the way the AIO transformer provides config is fundamentally different.

    }
}