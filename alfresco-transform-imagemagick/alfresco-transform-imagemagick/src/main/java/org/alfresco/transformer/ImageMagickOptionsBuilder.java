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

import com.google.common.collect.ImmutableList;
import org.alfresco.transform.exceptions.TransformException;

import java.util.List;
import java.util.StringJoiner;

import static org.alfresco.transformer.util.Util.stringToBoolean;
import static org.alfresco.transformer.util.Util.stringToInteger;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * ImageMagick options builder.
 *
 * @author Cezar Leahu
 */
public final class ImageMagickOptionsBuilder
{
    private static final List<String> GRAVITY_VALUES = ImmutableList.of("North", "NorthEast",
        "East", "SouthEast", "South", "SouthWest", "West", "NorthWest", "Center");

    private Integer startPage;
    private Integer endPage;
    private Boolean alphaRemove;
    private Boolean autoOrient;
    private String cropGravity;
    private Integer cropWidth;
    private Integer cropHeight;
    private Boolean cropPercentage;
    private Integer cropXOffset;
    private Integer cropYOffset;
    private Boolean thumbnail;
    private Integer resizeWidth;
    private Integer resizeHeight;
    private Boolean resizePercentage;
    private Boolean allowEnlargement;
    private Boolean maintainAspectRatio;
    private String commandOptions;

    private ImageMagickOptionsBuilder() {}

    public ImageMagickOptionsBuilder withStartPage(final String startPage)
    {
        return withStartPage(stringToInteger(startPage));
    }

    public ImageMagickOptionsBuilder withStartPage(final Integer startPage)
    {
        this.startPage = startPage;
        return this;
    }

    public ImageMagickOptionsBuilder withEndPage(final String endPage)
    {
        return withEndPage(stringToInteger(endPage));
    }

    public ImageMagickOptionsBuilder withEndPage(final Integer endPage)
    {
        this.endPage = endPage;
        return this;
    }

    public ImageMagickOptionsBuilder withAlphaRemove(final String alphaRemove)
    {
        return withAlphaRemove(stringToBoolean(alphaRemove));
    }

    public ImageMagickOptionsBuilder withAlphaRemove(final Boolean alphaRemove)
    {
        this.alphaRemove = alphaRemove;
        return this;
    }

    public ImageMagickOptionsBuilder withAutoOrient(final String autoOrient)
    {
        return withAutoOrient(stringToBoolean(autoOrient));
    }

    public ImageMagickOptionsBuilder withAutoOrient(final Boolean autoOrient)
    {
        this.autoOrient = autoOrient == null ? true : autoOrient;
        return this;
    }

    public ImageMagickOptionsBuilder withCropGravity(final String cropGravity)
    {
        this.cropGravity = cropGravity;
        return this;
    }

    public ImageMagickOptionsBuilder withCropWidth(final String cropWidth)
    {
        return withCropWidth(stringToInteger(cropWidth));
    }

    public ImageMagickOptionsBuilder withCropWidth(final Integer cropWidth)
    {
        this.cropWidth = cropWidth;
        return this;
    }

    public ImageMagickOptionsBuilder withCropHeight(final String cropHeight)
    {
        return withCropHeight(stringToInteger(cropHeight));
    }

    public ImageMagickOptionsBuilder withCropHeight(final Integer cropHeight)
    {
        this.cropHeight = cropHeight;
        return this;
    }

    public ImageMagickOptionsBuilder withCropPercentage(final String cropPercentage)
    {
        return withCropPercentage(stringToBoolean(cropPercentage));
    }

    public ImageMagickOptionsBuilder withCropPercentage(final Boolean cropPercentage)
    {
        this.cropPercentage = cropPercentage;
        return this;
    }

    public ImageMagickOptionsBuilder withCropXOffset(final String cropXOffset)
    {
        return withCropXOffset(stringToInteger(cropXOffset));
    }

    public ImageMagickOptionsBuilder withCropXOffset(final Integer cropXOffset)
    {
        this.cropXOffset = cropXOffset;
        return this;
    }

    public ImageMagickOptionsBuilder withCropYOffset(final String cropYOffset)
    {
        return withCropYOffset(stringToInteger(cropYOffset));
    }

    public ImageMagickOptionsBuilder withCropYOffset(final Integer cropYOffset)
    {
        this.cropYOffset = cropYOffset;
        return this;
    }

    public ImageMagickOptionsBuilder withThumbnail(final String thumbnail)
    {
        return withThumbnail(stringToBoolean(thumbnail));
    }

    public ImageMagickOptionsBuilder withThumbnail(final Boolean thumbnail)
    {
        this.thumbnail = thumbnail;
        return this;
    }

    public ImageMagickOptionsBuilder withResizeWidth(final String resizeWidth)
    {
        return withResizeWidth(stringToInteger(resizeWidth));
    }

    public ImageMagickOptionsBuilder withResizeWidth(final Integer resizeWidth)
    {
        this.resizeWidth = resizeWidth;
        return this;
    }

    public ImageMagickOptionsBuilder withResizeHeight(final String resizeHeight)
    {
        return withResizeHeight(stringToInteger(resizeHeight));
    }

    public ImageMagickOptionsBuilder withResizeHeight(final Integer resizeHeight)
    {
        this.resizeHeight = resizeHeight;
        return this;
    }

    public ImageMagickOptionsBuilder withResizePercentage(final String resizePercentage)
    {
        return withResizePercentage(stringToBoolean(resizePercentage));
    }

    public ImageMagickOptionsBuilder withResizePercentage(final Boolean resizePercentage)
    {
        this.resizePercentage = resizePercentage;
        return this;
    }

    public ImageMagickOptionsBuilder withAllowEnlargement(final String allowEnlargement)
    {
        return withAllowEnlargement(stringToBoolean(allowEnlargement));
    }

    public ImageMagickOptionsBuilder withAllowEnlargement(final Boolean allowEnlargement)
    {
        this.allowEnlargement = allowEnlargement == null ? true : allowEnlargement;
        return this;
    }

    public ImageMagickOptionsBuilder withMaintainAspectRatio(final String maintainAspectRatio)
    {
        return withMaintainAspectRatio(stringToBoolean(maintainAspectRatio));
    }

    public ImageMagickOptionsBuilder withMaintainAspectRatio(final Boolean maintainAspectRatio)
    {
        this.maintainAspectRatio = maintainAspectRatio;
        return this;
    }

    public ImageMagickOptionsBuilder withCommandOptions(final String commandOptions)
    {
        this.commandOptions = commandOptions;
        return this;
    }

    public String build()
    {
        if (cropGravity != null)
        {
            cropGravity = cropGravity.trim();
            if (cropGravity.isEmpty())
            {
                cropGravity = null;
            }
            else if (!GRAVITY_VALUES.contains(cropGravity))
            {
                throw new TransformException(BAD_REQUEST.value(), "Invalid cropGravity value");
            }
        }

        StringJoiner args = new StringJoiner(" ");
        if (alphaRemove != null && alphaRemove)
        {
            args.add("-alpha");
            args.add(("remove"));
        }
        if (autoOrient != null && autoOrient)
        {
            args.add("-auto-orient");
        }

        if (cropGravity != null || cropWidth != null || cropHeight != null || cropPercentage != null ||
            cropXOffset != null || cropYOffset != null)
        {
            if (cropGravity != null)
            {
                args.add("-gravity");
                args.add(cropGravity);
            }

            StringBuilder crop = new StringBuilder();
            if (cropWidth != null && cropWidth >= 0)
            {
                crop.append(cropWidth);
            }
            if (cropHeight != null && cropHeight >= 0)
            {
                crop.append('x');
                crop.append(cropHeight);
            }
            if (cropPercentage != null && cropPercentage)
            {
                crop.append('%');
            }
            if (cropXOffset != null)
            {
                if (cropXOffset >= 0)
                {
                    crop.append('+');
                }
                crop.append(cropXOffset);
            }
            if (cropYOffset != null)
            {
                if (cropYOffset >= 0)
                {
                    crop.append('+');
                }
                crop.append(cropYOffset);
            }
            if (crop.length() > 1)
            {
                args.add("-crop");
                args.add(crop);
            }

            args.add("+repage");
        }

        if (resizeHeight != null || resizeWidth != null || resizePercentage != null || maintainAspectRatio != null)
        {
            args.add(thumbnail != null && thumbnail ? "-thumbnail" : "-resize");
            StringBuilder resize = new StringBuilder();
            if (resizeWidth != null && resizeWidth >= 0)
            {
                resize.append(resizeWidth);
            }
            if (resizeHeight != null && resizeHeight >= 0)
            {
                resize.append('x');
                resize.append(resizeHeight);
            }
            if (resizePercentage != null && resizePercentage)
            {
                resize.append('%');
            }
            if (allowEnlargement == null || !allowEnlargement)
            {
                resize.append('>');
            }
            if (maintainAspectRatio != null && !maintainAspectRatio)
            {
                resize.append('!');
            }
            if (resize.length() > 1)
            {
                args.add(resize);
            }
        }

        return (commandOptions == null || "".equals(
            commandOptions.trim()) ? "" : commandOptions + ' ') +
               args.toString();
    }

    public static ImageMagickOptionsBuilder builder()
    {
        return new ImageMagickOptionsBuilder();
    }
}
