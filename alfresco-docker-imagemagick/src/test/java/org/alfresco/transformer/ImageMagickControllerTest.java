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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.alfresco.transform.client.model.TransformRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Test the ImageMagickController without a server.
 * Super class includes tests for the AbstractTransformerController.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(ImageMagickController.class)
public class ImageMagickControllerTest extends AbstractTransformerControllerTest
{
    @SpyBean
    private ImageMagickController controller;

    @Before
    public void before() throws IOException
    {
        controller.setAlfrescoSharedFileStoreClient(alfrescoSharedFileStoreClient);
        super.controller = controller;

        super.mockTransformCommand(controller, "jpg", "png", "image/jpg", true);
    }

    @Test
    public void cropGravityGoodTest() throws Exception
    {
        for (String value: new String[] {"North", "NorthEast", "East", "SouthEast", "South", "SouthWest", "West", "NorthWest", "Center"})
        {
            expectedOptions = "-gravity "+value+" +repage";
            mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                    .file(sourceFile)
                    .param("targetExtension", targetExtension)
                    .param("cropGravity", value))
                    .andExpect(status().is(200))
                    .andExpect(content().bytes(expectedTargetFileBytes))
                    .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
        }
    }

    @Test
    public void cropGravityBadTest() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension)
                .param("cropGravity", "badValue"))
                .andExpect(status().is(400));
    }

    @Test
    public void optionsTest() throws Exception
    {
        expectedOptions = "-alpha remove -gravity SouthEast -crop 123x456%+90+12 +repage -thumbnail 321x654%!";
        expectedSourceSuffix = "[2-3]";
        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension)

                .param("startPage", "2")
                .param("endPage", "3")

                .param("alphaRemove", "true")
                .param("autoOrient", "false")

                .param("cropGravity", "SouthEast")
                .param("cropWidth", "123")
                .param("cropHeight", "456")
                .param("cropPercentage", "true")
                .param("cropXOffset", "90")
                .param("cropYOffset", "12")

                .param("thumbnail", "true")
                .param("resizeWidth", "321")
                .param("resizeHeight", "654")
                .param("resizePercentage", "true")
                .param("allowEnlargement", "true")
                .param("maintainAspectRatio", "true"))

                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
    }

    @Test
    public void optionsNegateBooleansTest() throws Exception
    {
        expectedOptions = "-auto-orient -gravity SouthEast -crop 123x456+90+12 +repage -resize 321x654>";
        expectedSourceSuffix = "[2-3]";
        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension)

                .param("startPage", "2")
                .param("endPage", "3")

                .param("alphaRemove", "false")
                .param("autoOrient", "true")

                .param("cropGravity", "SouthEast")
                .param("cropWidth", "123")
                .param("cropHeight", "456")
                .param("cropPercentage", "false")
                .param("cropXOffset", "90")
                .param("cropYOffset", "12")

                .param("thumbnail", "false")
                .param("resizeWidth", "321")
                .param("resizeHeight", "654")
                .param("resizePercentage", "false")
                .param("allowEnlargement", "false")
                .param("maintainAspectRatio", "false"))

                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
    }

    @Test
    public void deprecatedCommandOptionsTest() throws Exception
    {
        // Example of why the commandOptions parameter is a bad idea.
        expectedOptions = "( horrible command / ); -resize 321x654>";
        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/transform")
                .file(sourceFile)
                .param("targetExtension", targetExtension)
                .param("thumbnail", "false")
                .param("resizeWidth", "321")
                .param("resizeHeight", "654")
                .param("commandOptions", "( horrible command / );"))
                .andExpect(status().is(200))
                .andExpect(content().bytes(expectedTargetFileBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename*= UTF-8''quick."+targetExtension));
    }

    @Override
    protected void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest)
    {
        transformRequest.setSourceExtension("png");
        transformRequest.setTargetExtension("png");
    }
}
