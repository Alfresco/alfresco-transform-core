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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@WebMvcTest(AllInOneController.class)
@Import(AllInOneCustomConfig.class)
public class AllInOneControllerImageMagickTestPOC extends ImageMagickControllerTestBasePOC 
{
    
    static ImageMagickAdapter adapter;
    
    @Autowired
    AllInOneTransformer transformer;

    @SpyBean
    AllInOneController controller;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        adapter = new ImageMagickAdapter();
    }

    @Before @SuppressWarnings("unchecked")
    public void before() throws IOException
    {
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

        ReflectionTestUtils.setField(controller, "transformer",transformer);

        mockTransformCommand("jpg", "png", "image/jpg", true);
    }

    @Override
    protected AbstractTransformerController getController() 
    {
        return controller;
    }

    //There is currently a bug in the version of the surefire plugin that junit4 uses
    //which means that inherited tests are not detected and run if there are no tests
    //present in the super class, the below is a workaround.
    @Test
    public void emptyTest()
    {
        controller.info();
    }

   
}