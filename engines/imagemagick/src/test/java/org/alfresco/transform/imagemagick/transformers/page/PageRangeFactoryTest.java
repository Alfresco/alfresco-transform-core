/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2026 Alfresco Software Limited
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
package org.alfresco.transform.imagemagick.transformers.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PSD;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_TIFF;
import static org.alfresco.transform.common.RequestParamMap.END_PAGE;
import static org.alfresco.transform.common.RequestParamMap.START_PAGE;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.alfresco.transform.imagemagick.dialect.GraphicsMagickDialect;
import org.alfresco.transform.imagemagick.dialect.ImageMagickDialect;

class PageRangeFactoryTest
{
    private final PageRangeFactory im = new PageRangeFactory(new ImageMagickDialect());
    private final PageRangeFactory gm = new PageRangeFactory(new GraphicsMagickDialect());

    @Test
    void imageMagickReadsAWholePsdFromItsMergedCompositeAtIndexZero()
    {
        assertEquals("[0]", im.create(MIMETYPE_IMAGE_PSD, MIMETYPE_IMAGE_PNG, Map.of()));
        assertEquals("[0]", im.create(MIMETYPE_IMAGE_PSD, MIMETYPE_IMAGE_JPEG, Map.of(START_PAGE, "0", END_PAGE, "0")));
        assertFalse(im.flattensWholePsd(MIMETYPE_IMAGE_PSD, Map.of()));
    }

    @Test
    void graphicsMagickFlattensAWholePsdInsteadOfReadingIndexZero()
    {
        assertEquals("", gm.create(MIMETYPE_IMAGE_PSD, MIMETYPE_IMAGE_PNG, Map.of()));
        assertEquals("", gm.create(MIMETYPE_IMAGE_PSD, MIMETYPE_IMAGE_JPEG, Map.of(START_PAGE, "0", END_PAGE, "0")));
        assertTrue(gm.flattensWholePsd(MIMETYPE_IMAGE_PSD, Map.of()));
        assertTrue(gm.flattensWholePsd(MIMETYPE_IMAGE_PSD, Map.of(START_PAGE, "0", END_PAGE, "0")));
    }

    @Test
    void aRequestForASpecificPsdLayerIsLeftAloneByBothDialects()
    {
        Map<String, String> secondLayer = Map.of(START_PAGE, "1", END_PAGE, "1");
        assertEquals("[1]", im.create(MIMETYPE_IMAGE_PSD, MIMETYPE_IMAGE_PNG, secondLayer));
        assertEquals("[1]", gm.create(MIMETYPE_IMAGE_PSD, MIMETYPE_IMAGE_PNG, secondLayer));
        assertFalse(gm.flattensWholePsd(MIMETYPE_IMAGE_PSD, secondLayer));
    }

    @Test
    void otherSourcesAreTheSameForBothDialects()
    {
        for (PageRangeFactory factory : new PageRangeFactory[]{im, gm})
        {
            assertEquals("[0]", factory.create(MIMETYPE_IMAGE_TIFF, MIMETYPE_IMAGE_PNG, Map.of()));
            assertEquals("[2-3]", factory.create(MIMETYPE_IMAGE_TIFF, MIMETYPE_IMAGE_TIFF, Map.of(START_PAGE, "2", END_PAGE, "3")));
            assertEquals("", factory.create(MIMETYPE_IMAGE_TIFF, MIMETYPE_IMAGE_TIFF, Map.of()));
            assertEquals("", factory.create(MIMETYPE_IMAGE_JPEG, MIMETYPE_IMAGE_PNG, Map.of()));
            assertFalse(factory.flattensWholePsd(MIMETYPE_IMAGE_TIFF, Map.of()));
        }
    }
}
