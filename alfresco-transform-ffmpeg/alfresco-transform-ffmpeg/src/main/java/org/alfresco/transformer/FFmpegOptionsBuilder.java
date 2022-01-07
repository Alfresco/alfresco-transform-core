/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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

import java.util.StringJoiner;

/**
 * FFmpeg options builder
 *
 * https://ffmpeg.org/ffmpeg.html#Options including:
 * - https://ffmpeg.org/ffmpeg.html#Main-options
 * - https://ffmpeg.org/ffmpeg.html#Video-Options
 * - https://ffmpeg.org/ffmpeg-utils.html#time-duration-syntax
 * 
 * @author janv
 */
// TODO PoC for FFmpeg - add other FFmpeg transform options (as needed) ...
public final class FFmpegOptionsBuilder
{
    private Integer framesNum;

    // temporal
    private String timeOffset;
    private String duration;

    // frame resolution
    private Integer frameWidth;
    private Integer frameHeight;

    private FFmpegOptionsBuilder() {}

    public FFmpegOptionsBuilder withFramesNum(final Integer framesNum)
    {
        this.framesNum = framesNum;
        return this;
    }

    public FFmpegOptionsBuilder withTimeOffset(final String timeOffset)
    {
        this.timeOffset = timeOffset;
        return this;
    }

    public FFmpegOptionsBuilder withDuration(final String duration)
    {
        this.duration = duration;
        return this;
    }

    public FFmpegOptionsBuilder withFrameWidth(final Integer frameWidth)
    {
        this.frameWidth = frameWidth;
        return this;
    }

    public FFmpegOptionsBuilder withFrameHeight(final Integer frameHeight)
    {
        this.frameHeight = frameHeight;
        return this;
    }

    public String build()
    {
        StringJoiner args = new StringJoiner(" ");

        if (framesNum != null)
        {
            args.add("-frames:v "+framesNum);
        }

        if (timeOffset != null)
        {
            args.add("-ss "+timeOffset);
        }

        if (duration != null)
        {
            args.add("-t "+duration);
        }

        if ((frameWidth != null) && (frameHeight != null))
        {
            args.add("-s "+frameWidth+"x"+frameHeight);
        }

        return args.toString();
    }

    public static FFmpegOptionsBuilder builder()
    {
        return new FFmpegOptionsBuilder();
    }
}
