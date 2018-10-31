package org.alfresco.transformer;

import static java.util.Arrays.asList;
import static org.alfresco.transformer.util.Util.stringToBoolean;
import static org.alfresco.transformer.util.Util.stringToInteger;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.List;
import java.util.StringJoiner;

import org.alfresco.transformer.exceptions.TransformException;

/**
 * ImageMagick options builder.
 *
 * @author Cezar Leahu
 */
public final class OptionsBuilder
{
    private static final List<String> GRAVITY_VALUES = asList("North", "NorthEast", "East",
        "SouthEast", "South", "SouthWest", "West", "NorthWest", "Center");

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

    private OptionsBuilder()
    {
    }

    public OptionsBuilder withStartPage(final String startPage)
    {
        return withStartPage(stringToInteger(startPage));
    }

    public OptionsBuilder withStartPage(final Integer startPage)
    {
        this.startPage = startPage;
        return this;
    }

    public OptionsBuilder withEndPage(final String endPage)
    {
        return withEndPage(stringToInteger(endPage));
    }

    public OptionsBuilder withEndPage(final Integer endPage)
    {
        this.endPage = endPage;
        return this;
    }

    public OptionsBuilder withAlphaRemove(final String alphaRemove)
    {
        return withAlphaRemove(stringToBoolean(alphaRemove));
    }

    public OptionsBuilder withAlphaRemove(final Boolean alphaRemove)
    {
        this.alphaRemove = alphaRemove;
        return this;
    }

    public OptionsBuilder withAutoOrient(final String autoOrient)
    {
        return withAutoOrient(stringToBoolean(autoOrient));
    }

    public OptionsBuilder withAutoOrient(final Boolean autoOrient)
    {
        this.autoOrient = autoOrient;
        return this;
    }

    public OptionsBuilder withCropGravity(final String cropGravity)
    {
        this.cropGravity = cropGravity;
        return this;
    }

    public OptionsBuilder withCropWidth(final String cropWidth)
    {
        return withCropWidth(stringToInteger(cropWidth));
    }

    public OptionsBuilder withCropWidth(final Integer cropWidth)
    {
        this.cropWidth = cropWidth;
        return this;
    }

    public OptionsBuilder withCropHeight(final String cropHeight)
    {
        return withCropHeight(stringToInteger(cropHeight));
    }

    public OptionsBuilder withCropHeight(final Integer cropHeight)
    {
        this.cropHeight = cropHeight;
        return this;
    }

    public OptionsBuilder withCropPercentage(final String cropPercentage)
    {
        return withCropPercentage(stringToBoolean(cropPercentage));
    }

    public OptionsBuilder withCropPercentage(final Boolean cropPercentage)
    {
        this.cropPercentage = cropPercentage;
        return this;
    }

    public OptionsBuilder withCropXOffset(final String cropXOffset)
    {
        return withCropXOffset(stringToInteger(cropXOffset));
    }

    public OptionsBuilder withCropXOffset(final Integer cropXOffset)
    {
        this.cropXOffset = cropXOffset;
        return this;
    }

    public OptionsBuilder withCropYOffset(final String cropYOffset)
    {
        return withCropYOffset(stringToInteger(cropYOffset));
    }

    public OptionsBuilder withCropYOffset(final Integer cropYOffset)
    {
        this.cropYOffset = cropYOffset;
        return this;
    }

    public OptionsBuilder withThumbnail(final String thumbnail)
    {
        return withThumbnail(stringToBoolean(thumbnail));
    }

    public OptionsBuilder withThumbnail(final Boolean thumbnail)
    {
        this.thumbnail = thumbnail;
        return this;
    }

    public OptionsBuilder withResizeWidth(final String resizeWidth)
    {
        return withResizeWidth(stringToInteger(resizeWidth));
    }

    public OptionsBuilder withResizeWidth(final Integer resizeWidth)
    {
        this.resizeWidth = resizeWidth;
        return this;
    }

    public OptionsBuilder withResizeHeight(final String resizeHeight)
    {
        return withResizeHeight(stringToInteger(resizeHeight));
    }

    public OptionsBuilder withResizeHeight(final Integer resizeHeight)
    {
        this.resizeHeight = resizeHeight;
        return this;
    }

    public OptionsBuilder withResizePercentage(final String resizePercentage)
    {
        return withResizePercentage(stringToBoolean(resizePercentage));
    }

    public OptionsBuilder withResizePercentage(final Boolean resizePercentage)
    {
        this.resizePercentage = resizePercentage;
        return this;
    }

    public OptionsBuilder withAllowEnlargement(final String allowEnlargement)
    {
        return withAllowEnlargement(stringToBoolean(allowEnlargement));
    }

    public OptionsBuilder withAllowEnlargement(final Boolean allowEnlargement)
    {
        this.allowEnlargement = allowEnlargement;
        return this;
    }

    public OptionsBuilder withMaintainAspectRatio(final String maintainAspectRatio)
    {
        return withMaintainAspectRatio(stringToBoolean(maintainAspectRatio));
    }

    public OptionsBuilder withMaintainAspectRatio(final Boolean maintainAspectRatio)
    {
        this.maintainAspectRatio = maintainAspectRatio;
        return this;
    }

    public OptionsBuilder withCommandOptions(final String commandOptions)
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
            if (maintainAspectRatio != null && maintainAspectRatio)
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

    public static OptionsBuilder builder()
    {
        return new OptionsBuilder();
    }
}
