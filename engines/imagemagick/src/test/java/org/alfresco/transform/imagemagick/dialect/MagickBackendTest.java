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
package org.alfresco.transform.imagemagick.dialect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.alfresco.transform.imagemagick.dialect.MagickBackend.GRAPHICSMAGICK;
import static org.alfresco.transform.imagemagick.dialect.MagickBackend.IMAGEMAGICK;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MagickBackendTest
{
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void anUnsetBackendIsImageMagick(String value)
    {
        assertEquals(IMAGEMAGICK, MagickBackend.from(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"imagemagick", "ImageMagick", "IMAGEMAGICK", " imagemagick "})
    void imageMagickIsMatchedIgnoringCaseAndWhitespace(String value)
    {
        assertEquals(IMAGEMAGICK, MagickBackend.from(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"graphicsmagick", "GraphicsMagick", "GRAPHICSMAGICK", " graphicsmagick "})
    void graphicsMagickIsMatchedIgnoringCaseAndWhitespace(String value)
    {
        assertEquals(GRAPHICSMAGICK, MagickBackend.from(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"gm", "im", "graphicmagick", "magick"})
    void anUnknownBackendFailsNamingThePropertyAndTheValidValues(String value)
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> MagickBackend.from(value));
        assertTrue(e.getMessage().contains(MagickBackend.PROPERTY), e.getMessage());
        assertTrue(e.getMessage().contains("imagemagick, graphicsmagick"), e.getMessage());
    }

    @Test
    void eachBackendCreatesItsOwnDialect()
    {
        assertInstanceOf(ImageMagickDialect.class, MagickDialectConfig.createDialect(IMAGEMAGICK));
        assertInstanceOf(GraphicsMagickDialect.class, MagickDialectConfig.createDialect(GRAPHICSMAGICK));
        for (MagickBackend backend : MagickBackend.values())
        {
            assertEquals(backend, MagickDialectConfig.createDialect(backend).backend());
        }
    }

    @Test
    void theConfigDefaultsToImageMagick()
    {
        MagickDialectConfig config = new MagickDialectConfig();
        assertInstanceOf(ImageMagickDialect.class, config.magickDialect("imagemagick"));
        assertInstanceOf(ImageMagickDialect.class, config.magickDialect(""));
        assertInstanceOf(GraphicsMagickDialect.class, config.magickDialect("graphicsmagick"));
    }
}
