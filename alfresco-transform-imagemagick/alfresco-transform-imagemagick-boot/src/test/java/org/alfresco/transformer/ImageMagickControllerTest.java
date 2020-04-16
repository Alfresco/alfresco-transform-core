/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test the ImageMagickController without a server.
 * Super class includes tests for the ImageMagickController & AbstractTransformerController.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(ImageMagickController.class)
public class ImageMagickControllerTest extends ImageMagickControllerTestBase
{

    // Brought to the sub class in order to prevent inheritance.
    // Could not inherit from this class if requred to instantiate this variable
    @SpyBean
    private ImageMagickController controller;

    @Before
    public void before() throws IOException
    {
        ReflectionTestUtils.setField(commandExecutor, "transformCommand", mockTransformCommand);
        ReflectionTestUtils.setField(commandExecutor, "checkCommand", mockCheckCommand);
        ReflectionTestUtils.setField(controller, "commandExecutor", commandExecutor);

        mockTransformCommand("jpg", "png", "image/jpg", true);
    }

    @Override
    protected AbstractTransformerController getController() 
    {
        return controller;
    }

}
