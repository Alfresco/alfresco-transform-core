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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MagickDialectTest
{
    private static final String IM_VERSION = "Version: ImageMagick 7.1.2-13 Q16-HDRI x86_64 22800 https://imagemagick.org\n"
            + "Copyright: (C) 1999 ImageMagick Studio LLC\n";
    private static final String GM_VERSION = "GraphicsMagick 1.3.48 2025-09-06 Q16 http://www.GraphicsMagick.org/\n"
            + "Copyright (C) 2002-2025 GraphicsMagick Group.\n";

    private final MagickDialect im = new ImageMagickDialect();
    private final MagickDialect gm = new GraphicsMagickDialect();

    @Test
    void imageMagickKeepsTheOriginalCommandLines()
    {
        assertArrayEquals(new String[]{"/usr/bin/convert", "${source}", "SPLIT:${options}", "-strip", "-quiet", "${target}"},
                im.transformCommand("/usr/bin/convert"));
        assertArrayEquals(new String[]{"/usr/bin/convert", "-version"}, im.checkCommand("/usr/bin/convert"));
    }

    @Test
    void imageMagickKeepsTheOriginalDefaults()
    {
        assertEquals(MagickBackend.IMAGEMAGICK, im.backend());
        assertEquals("/usr/bin/convert", im.defaultExe());
        assertEquals("/usr/lib64/ImageMagick-7.1.2", im.defaultRoot());
        assertEquals("/usr/lib64/ImageMagick-7.1.2/lib", im.defaultDyn());
        assertNull(im.defaultThreads(), "OMP_NUM_THREADS was never set for ImageMagick");
        assertEquals(List.of("-alpha", "remove"), im.alphaRemoveArgs());
        assertFalse(im.flattensWholePsd());
        assertFalse(im.fillsUnspecifiedCropDimension());
        assertEquals(25383, im.probeExpectedLength());
        assertTrue(im.startupMessage().contains("ImageMagick Studio LLC"));
    }

    @Test
    void graphicsMagickUsesTheGmDispatchBinary()
    {
        assertArrayEquals(new String[]{"/usr/local/bin/gm", "convert", "${source}", "SPLIT:${options}", "-strip", "${target}"},
                gm.transformCommand("/usr/local/bin/gm"));
        assertArrayEquals(new String[]{"/usr/local/bin/gm", "version"}, gm.checkCommand("/usr/local/bin/gm"));
    }

    @Test
    void graphicsMagickDefaults()
    {
        assertEquals(MagickBackend.GRAPHICSMAGICK, gm.backend());
        assertEquals("/usr/local/bin/gm", gm.defaultExe());
        assertEquals("/usr/local", gm.defaultRoot());
        assertEquals("/usr/local/lib", gm.defaultDyn());
        assertEquals("1", gm.defaultThreads());
        assertEquals(List.of("-background", "white", "-flatten"), gm.alphaRemoveArgs());
        assertTrue(gm.flattensWholePsd());
        assertTrue(gm.fillsUnspecifiedCropDimension());
        assertEquals(7913, gm.probeExpectedLength());
        assertTrue(gm.startupMessage().contains("GraphicsMagick Group"));
    }

    @Test
    void graphicsMagickNeverPassesTheImageMagickOnlyQuietOption()
    {
        assertFalse(List.of(gm.transformCommand("gm")).contains("-quiet"));
    }

    @Test
    void eachDialectRecognisesOnlyItsOwnVersionOutput()
    {
        assertTrue(im.isOwnVersionOutput(IM_VERSION));
        assertFalse(im.isOwnVersionOutput(GM_VERSION));
        assertTrue(gm.isOwnVersionOutput(GM_VERSION));
        assertFalse(gm.isOwnVersionOutput(IM_VERSION));

        assertFalse(im.isOwnVersionOutput(null));
        assertFalse(gm.isOwnVersionOutput(""));
    }
}
