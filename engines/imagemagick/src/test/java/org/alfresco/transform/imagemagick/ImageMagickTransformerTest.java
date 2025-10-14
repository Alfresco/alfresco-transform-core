/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2025 Alfresco Software Limited
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
package org.alfresco.transform.imagemagick;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.alfresco.transform.imagemagick.transformers.ImageMagickCommandExecutor;
import org.alfresco.transform.imagemagick.transformers.ImageMagickTransformer;
import org.alfresco.transform.imagemagick.transformers.page.PageRangeFactory;

public class ImageMagickTransformerTest
{
    @Test
    void shouldNotAllowArgumentInjectionThroughCommandOptions()
    {
        ImageMagickCommandExecutor imageMagickCommandExecutor = mock(ImageMagickCommandExecutor.class);
        ImageMagickTransformer imageMagickTransformer = new ImageMagickTransformer(imageMagickCommandExecutor, mock(PageRangeFactory.class));
        Map<String, String> transformOptions = Map.of(
                "commandOptions", "( horrible command / );");

        imageMagickTransformer.transform(null, null, transformOptions, null, null, null);

        ArgumentCaptor<String> optionsCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageMagickCommandExecutor).run(optionsCaptor.capture(), any(), any(), any(), any());
        assertThat(optionsCaptor.getValue()).doesNotContain("horrible", "horrible command", "( horrible command / );");
    }
}
