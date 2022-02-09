/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.alfresco.transformer.executors.LibreOfficeJavaExecutor;
import org.alfresco.transformer.executors.Transformer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AIOController.class)
@Import(AIOCustomConfig.class)
/**
 * Test the AIOController without a server.
 * Super class includes tests for the LibreOfficeController and AbstractTransformerController.
 */
public class AIOControllerLibreOfficeTest extends LibreOfficeControllerTest
{
    //Tests contained in LibreOfficeControllerTest
    
    @Test
    public void testTestValidity()
    {
        // just test that we are actually testing against the AIOController (instead of MiscController)
        assertTrue(controller instanceof AIOController,"Wrong controller wired for test");
    }

    @Autowired
    AIOTransformRegistry transformRegistry;

    @Override
    // Used by the super class to mock the javaExecutor, a different implementation is required here
    protected void setJavaExecutor(AbstractTransformerController controller, LibreOfficeJavaExecutor javaExecutor)
    {
        //Need to wire in the mocked javaExecutor into the controller...
        Map<String,Transformer> transformers = transformRegistry.getTransformerEngineMapping();
        transformers.replace("libreoffice", javaExecutor);
        // No need to set the transform registry to the controller as it is @Autowired in
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