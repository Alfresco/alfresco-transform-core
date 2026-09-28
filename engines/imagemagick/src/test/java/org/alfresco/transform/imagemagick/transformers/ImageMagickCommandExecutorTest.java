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
package org.alfresco.transform.imagemagick.transformers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.alfresco.transform.imagemagick.transformers.ImageMagickCommandExecutor.maxConcurrentTransforms;
import static org.alfresco.transform.imagemagick.transformers.ImageMagickCommandExecutor.resolveOmpNumThreads;
import static org.alfresco.transform.imagemagick.transformers.ImageMagickCommandExecutor.verifyVersionOutput;

import org.junit.jupiter.api.Test;

import org.alfresco.transform.imagemagick.dialect.GraphicsMagickDialect;
import org.alfresco.transform.imagemagick.dialect.ImageMagickDialect;
import org.alfresco.transform.imagemagick.dialect.MagickDialect;

class ImageMagickCommandExecutorTest
{
    @Test
    void valueSetOnContainerTakesPrecedence()
    {
        assertEquals("8", resolveOmpNumThreads("1", "1-10", 2, "8"));
        assertEquals("8", resolveOmpNumThreads("auto", "1-10", 2, " 8 "));
        assertEquals("8", resolveOmpNumThreads("banana", "1-10", 2, "8"));
    }

    @Test
    void explicitPropertyUsedWhenNothingIsSetOnTheContainer()
    {
        assertEquals("3", resolveOmpNumThreads("3", "1-10", 16, null));
        assertEquals("1", resolveOmpNumThreads("1", "1-10", 16, ""));
        assertEquals("4", resolveOmpNumThreads(" 4 ", "1-10", 16, "   "));
    }

    @Test
    void autoDividesTheCpuBudgetByTheTransformConcurrency()
    {
        assertEquals("1", resolveOmpNumThreads("auto", "1-10", 2, null));
        assertEquals("1", resolveOmpNumThreads("auto", "1-10", 16, null));
        assertEquals("2", resolveOmpNumThreads("auto", "1-1", 2, null));
        assertEquals("4", resolveOmpNumThreads("auto", "1-2", 8, null));
        assertEquals("1", resolveOmpNumThreads("AUTO", "1-10", 4, null));
    }

    @Test
    void autoNeverResolvesToLessThanOneThread()
    {
        assertEquals("1", resolveOmpNumThreads("auto", "1-10", 1, null));
        assertEquals("1", resolveOmpNumThreads("auto", "1-100", 8, null));
    }

    @Test
    void anUnreadableConcurrencyRangeFallsBackToTheShippedDefault()
    {
        assertEquals(10, maxConcurrentTransforms("not-a-range"));
        assertEquals(10, maxConcurrentTransforms(""));
        assertEquals(10, maxConcurrentTransforms(null));
        assertEquals("1", resolveOmpNumThreads("auto", "not-a-range", 4, null));
    }

    @Test
    void aConcurrencyValueWithoutARangeIsReadAsTheUpperBound()
    {
        assertEquals(5, maxConcurrentTransforms("5"));
        assertEquals(10, maxConcurrentTransforms(" 1-10 "));
        assertEquals(1, maxConcurrentTransforms("0"));
    }

    @Test
    void anInvalidThreadCountFailsFast()
    {
        assertThrows(IllegalArgumentException.class,
                () -> resolveOmpNumThreads("0", "1-10", 4, null));
        assertThrows(IllegalArgumentException.class,
                () -> resolveOmpNumThreads("-2", "1-10", 4, null));
        assertThrows(IllegalArgumentException.class,
                () -> resolveOmpNumThreads("banana", "1-10", 4, null));
        assertThrows(IllegalArgumentException.class,
                () -> resolveOmpNumThreads("", "1-10", 4, null));
    }

    private static final MagickDialect IM = new ImageMagickDialect();
    private static final MagickDialect GM = new GraphicsMagickDialect();
    private static final String IM_VERSION = "Version: ImageMagick 7.1.2-13 Q16-HDRI x86_64 22800 https://imagemagick.org";
    private static final String GM_VERSION = "GraphicsMagick 1.3.48 2025-09-06 Q16 http://www.GraphicsMagick.org/";

    @Test
    void imageMagickLeavesTheThreadCountUnsetByDefault()
    {
        assertNull(resolveOmpNumThreads(null, IM.defaultThreads(), "1-10", 16, null));
        assertNull(resolveOmpNumThreads("", IM.defaultThreads(), "1-10", 16, "  "));
    }

    @Test
    void graphicsMagickUsesOneThreadByDefault()
    {
        assertEquals("1", resolveOmpNumThreads(null, GM.defaultThreads(), "1-10", 16, null));
        assertEquals("1", resolveOmpNumThreads(" ", GM.defaultThreads(), "1-10", 16, ""));
    }

    @Test
    void anExplicitThreadCountAppliesToBothDialects()
    {
        assertEquals("4", resolveOmpNumThreads("4", IM.defaultThreads(), "1-10", 16, null));
        assertEquals("4", resolveOmpNumThreads("4", GM.defaultThreads(), "1-10", 16, null));
        assertEquals("2", resolveOmpNumThreads("auto", IM.defaultThreads(), "1-8", 16, null));
        assertEquals("8", resolveOmpNumThreads(null, IM.defaultThreads(), "1-10", 16, "8"));
    }

    @Test
    void aMatchingBinaryIsAccepted()
    {
        assertDoesNotThrow(() -> verifyVersionOutput(IM, "/usr/bin/convert", 0, IM_VERSION + "\nCopyright...", ""));
        assertDoesNotThrow(() -> verifyVersionOutput(GM, "/usr/local/bin/gm", 0, GM_VERSION + "\nCopyright...", ""));
    }

    @Test
    void theWrongBinaryStopsTheEngineStarting()
    {
        IllegalStateException imOnGm = assertThrows(IllegalStateException.class,
                () -> verifyVersionOutput(IM, "/usr/local/bin/gm", 0, GM_VERSION, ""));
        assertTrue(imOnGm.getMessage().contains("selects ImageMagick"), imOnGm.getMessage());
        assertTrue(imOnGm.getMessage().contains(GM_VERSION), imOnGm.getMessage());

        IllegalStateException gmOnIm = assertThrows(IllegalStateException.class,
                () -> verifyVersionOutput(GM, "/usr/bin/convert", 0, IM_VERSION, ""));
        assertTrue(gmOnIm.getMessage().contains("selects GraphicsMagick"), gmOnIm.getMessage());
        assertTrue(gmOnIm.getMessage().contains("IMAGEMAGICK_BACKEND"), gmOnIm.getMessage());
    }

    @Test
    void aBinaryThatCannotBeRunIsLeftToTheReadinessProbe()
    {
        assertDoesNotThrow(() -> verifyVersionOutput(IM, "/missing/convert", 1, "", "No such file or directory"));
        assertDoesNotThrow(() -> verifyVersionOutput(GM, "/missing/gm", 0, null, null));
    }
}
