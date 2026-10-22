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
package org.alfresco.transform.imagemagick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transform.imagemagick.dialect.GraphicsMagickDialect;
import org.alfresco.transform.imagemagick.dialect.ImageMagickDialect;
import org.alfresco.transform.imagemagick.dialect.MagickDialect;

class ImageMagickOptionsBuilderTest
{
    private static final MagickDialect IM = new ImageMagickDialect();
    private static final MagickDialect GM = new GraphicsMagickDialect();

    static Stream<Arguments> options()
    {
        return Stream.of(
                Arguments.of("nothing set", options(b -> b), "", ""),
                Arguments.of("auto orient by default", options(b -> b.withAutoOrient((Boolean) null)),
                        "-auto-orient", "-auto-orient"),
                Arguments.of("alpha remove", options(b -> b.withAlphaRemove(true).withAutoOrient(false)),
                        "-alpha remove", "-background white -flatten"),
                Arguments.of("crop width only", options(b -> b.withCropWidth(100)),
                        "-crop 100 +repage", "-crop 100x1000000 +repage"),
                Arguments.of("crop height only as a percentage", options(b -> b.withCropHeight(50).withCropPercentage(true)),
                        "-crop x50% +repage", "-crop 100x50% +repage"),
                Arguments.of("crop with a zero width and offsets",
                        options(b -> b.withCropWidth(0).withCropHeight(50).withCropXOffset(10).withCropYOffset(-4)),
                        "-crop 0x50+10-4 +repage", "-crop 1000000x50+10-4 +repage"),
                Arguments.of("crop width and height", options(b -> b.withCropWidth(123).withCropHeight(456)
                        .withCropXOffset(90).withCropYOffset(12)),
                        "-crop 123x456+90+12 +repage", "-crop 123x456+90+12 +repage"),
                Arguments.of("resize without enlargement", options(b -> b.withResizeWidth(400).withAllowEnlargement(false)),
                        "-resize 400>", "-resize 400>"),
                Arguments.of("every option", options(b -> b.withAlphaRemove(true).withAutoOrient(false)
                        .withCropGravity("SouthEast").withCropWidth(123).withCropHeight(456).withCropPercentage(true)
                        .withCropXOffset(90).withCropYOffset(12).withThumbnail(true).withResizeWidth(321)
                        .withResizeHeight(654).withResizePercentage(true).withAllowEnlargement(true)
                        .withMaintainAspectRatio(false)),
                        "-alpha remove -gravity SouthEast -crop 123x456%+90+12 +repage -thumbnail 321x654%!",
                        "-background white -flatten -gravity SouthEast -crop 123x456%+90+12 +repage -thumbnail 321x654%!"),
                Arguments.of("every option negated", options(b -> b.withAlphaRemove(false).withAutoOrient(true)
                        .withCropGravity("SouthEast").withCropWidth(123).withCropHeight(456).withCropPercentage(false)
                        .withCropXOffset(90).withCropYOffset(12).withThumbnail(false).withResizeWidth(321)
                        .withResizeHeight(654).withResizePercentage(false).withAllowEnlargement(false)
                        .withMaintainAspectRatio(true)),
                        "-auto-orient -gravity SouthEast -crop 123x456+90+12 +repage -resize 321x654>",
                        "-auto-orient -gravity SouthEast -crop 123x456+90+12 +repage -resize 321x654>"));
    }

    private static UnaryOperator<ImageMagickOptionsBuilder> options(UnaryOperator<ImageMagickOptionsBuilder> options)
    {
        return options;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("options")
    void imageMagickOptionsAreUnchanged(String description, UnaryOperator<ImageMagickOptionsBuilder> options,
            String imageMagick, String graphicsMagick)
    {
        assertEquals(imageMagick, options.apply(ImageMagickOptionsBuilder.builder(IM)).build());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("options")
    void graphicsMagickOptions(String description, UnaryOperator<ImageMagickOptionsBuilder> options,
            String imageMagick, String graphicsMagick)
    {
        assertEquals(graphicsMagick, options.apply(ImageMagickOptionsBuilder.builder(GM)).build());
    }

    @Test
    void flatteningTheLayersOfAPsdAddsFlattenOnce()
    {
        assertEquals("-flatten", ImageMagickOptionsBuilder.builder(GM).withFlattenLayers(true).build());
        assertEquals("-background white -flatten",
                ImageMagickOptionsBuilder.builder(GM).withFlattenLayers(true).withAlphaRemove(true).build());
        assertEquals("-background white -flatten -auto-orient",
                ImageMagickOptionsBuilder.builder(GM).withFlattenLayers(true).withAlphaRemove(true)
                        .withAutoOrient(true).build());
        assertEquals("-auto-orient -flatten",
                ImageMagickOptionsBuilder.builder(GM).withFlattenLayers(true).withAutoOrient(true).build());
    }

    @Test
    void theCommandOptionsArePrefixedForBothDialects()
    {
        assertEquals("-x -auto-orient",
                ImageMagickOptionsBuilder.builder(IM).withCommandOptions("-x").withAutoOrient(true).build());
        assertEquals("-x -auto-orient",
                ImageMagickOptionsBuilder.builder(GM).withCommandOptions("-x").withAutoOrient(true).build());
    }

    @Test
    void anInvalidGravityIsRejectedForBothDialects()
    {
        assertThrows(TransformException.class,
                () -> ImageMagickOptionsBuilder.builder(IM).withCropGravity("Up").build());
        assertThrows(TransformException.class,
                () -> ImageMagickOptionsBuilder.builder(GM).withCropGravity("Up").build());
    }

    @Test
    void aDialectIsRequired()
    {
        assertThrows(NullPointerException.class, () -> ImageMagickOptionsBuilder.builder(null));
    }
}
