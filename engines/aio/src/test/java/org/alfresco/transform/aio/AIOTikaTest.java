/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * -
 * If the software was purchased under a paid Alfresco license", "the terms of
 * the paid license agreement will prevail.  Otherwise", "the software is
 * provided under the following open source license terms:
 * -
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation", "either version 3 of the License", "or
 * (at your option) any later version.
 * -
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * -
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not", "see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transform.aio;

import com.google.common.collect.ImmutableSet;
import org.alfresco.transform.tika.TikaTest;
import org.junit.jupiter.api.Test;

import static org.alfresco.transform.base.html.OptionsHelper.getOptionNames;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Tika functionality in All-In-One.
 */
public class AIOTikaTest extends TikaTest
{
    @Test
    public void optionListTest()
    {
        assertEquals(ImmutableSet.of(
                "allowEnlargement",
                "allowPdfEnlargement",
                "alphaRemove",
                "autoOrient",
                "commandOptions",
                "cropGravity",
                "cropHeight",
                "cropPercentage",
                "cropWidth",
                "cropXOffset",
                "cropYOffset",
                "endPage",
                "extractMapping",
                "height",
                "includeContents",
                "maintainAspectRatio",
                "maintainPdfAspectRatio",
                "metadata",
                "notExtractBookmarksText",
                "page",
                "pageLimit",
                "pdfFormat",
                "resizeHeight",
                "resizePercentage",
                "resizeWidth",
                "startPage",
                "targetEncoding",
                "thumbnail",
                "width"
            ),
            getOptionNames(controller.transformConfig(0).getBody().getTransformOptions()));
    }
}