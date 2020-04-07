/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

import java.io.IOException;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

import org.alfresco.transformer.transformers.AllInOneTransformer;
import org.alfresco.transformer.transformers.ImageMagickAdapter;
import org.alfresco.transformer.transformers.Transformer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RunWith(SpringRunner.class)
@WebMvcTest(AIOController.class)
@Import(AIOCustomConfig.class)
public class AIOControllerImageMagickTest extends ImageMagickControllerTestBase 
{
    
    static ImageMagickAdapter adapter;
    
    @Autowired
    AllInOneTransformer transformer;

    @SpyBean
    AIOController controller;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        adapter = new ImageMagickAdapter();
    }

    @Before @SuppressWarnings("unchecked")
    public void before() throws IOException, Exception
    {
        adapter = new ImageMagickAdapter();
        ReflectionTestUtils.setField(commandExecutor, "transformCommand", mockTransformCommand);
        ReflectionTestUtils.setField(commandExecutor, "checkCommand", mockCheckCommand);
        ReflectionTestUtils.setField(adapter, "commandExecutor", commandExecutor);
        //Need to wire in the mocked adpater into the controller...
        if (ReflectionTestUtils.getField(transformer,"transformerTransformMapping") instanceof Map)
        {
            Map<String,Transformer> transformers = (Map<String,Transformer>)ReflectionTestUtils.getField(transformer,"transformerTransformMapping");
            transformers.replace("imagemagick", adapter);
            ReflectionTestUtils.setField(transformer, "transformerTransformMapping", transformers);
        }

        mockTransformCommand("jpg", "png", "image/jpeg", true);
    }

    @Override
    protected AbstractTransformerController getController() 
    {
        return controller;
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
    public void noTargetFileTest()
    {
        // Ignore the test in super class as the AIO transforms we not be selected .
        // It is the mock that returns a zero length file for other transformers, when we supply an invalid targetExtension.
    }
   
    @Test
    @Override
    public void testGetTransformConfigInfo()
    {
        // Ignore the test in super class as the AIO transforms we not be selected .

    }

    @Test
    @Override
    public void testGetInfoFromConfigWithDuplicates()
    {
        // Ignore the test in super class as the AIO transforms we not be selected .

    }
    @Test
    @Override
    public void testGetInfoFromConfigWithEmptyTransformOptions()
    {
        // Ignore the test in super class as the AIO transforms we not be selected .

    }
    @Test
    @Override
    public void testGetInfoFromConfigWithNoTransformOptions()
    {
        // Ignore the test in super class as the AIO transforms we not be selected .

    }
    
    @Test
    @Override
    public void deprecatedCommandOptionsTest() throws Exception
    {
        // Ignore test in super class as the CommandOptions is not within the imagemagick_engine_config.json
        // as such it will not be considere a to have a valid transformer.

    }
}