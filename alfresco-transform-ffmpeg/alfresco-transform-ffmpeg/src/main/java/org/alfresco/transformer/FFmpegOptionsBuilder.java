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
 * FFmpeg options builder.
 *
 * @author janv
 */
// TODO PoC for FFmpeg
public final class FFmpegOptionsBuilder
{
    private String timeOffset;
    private String duration;
    
    private Integer framesNum;

    // TODO PoC - add other FFmpeg transform options ...
    private FFmpegOptionsBuilder() {}

    public FFmpegOptionsBuilder withTimeOffset(final String timeOffset)
    {
        this.timeOffset = timeOffset;
        return this;
    }

    public FFmpegOptionsBuilder withFramesNum(final Integer framesNum)
    {
        this.framesNum = framesNum;
        return this;
    }

    public FFmpegOptionsBuilder withDuration(final String duration)
    {
        this.duration = duration;
        return this;
    }

    public String build()
    {
        StringJoiner args = new StringJoiner(" ");

        if (timeOffset != null)
        {
            args.add("-ss "+timeOffset);
        }

        if (framesNum != null)
        {
            args.add("-frames:v "+framesNum);
        }

        if (duration != null)
        {
            args.add("-t "+duration);
        }

        return args.toString();
    }

    public static FFmpegOptionsBuilder builder()
    {
        return new FFmpegOptionsBuilder();
    }
}
