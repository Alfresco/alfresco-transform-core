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

import static org.alfresco.transform.imagemagick.dialect.ImageMagickDialect.GRAPHICSMAGICK_VERSION_PREFIX;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class GraphicsMagickDialect implements MagickDialect
{
    @Override
    public MagickBackend backend()
    {
        return MagickBackend.GRAPHICSMAGICK;
    }

    @Override
    public String displayName()
    {
        return "GraphicsMagick";
    }

    @Override
    public String defaultExe()
    {
        return "/usr/local/bin/gm";
    }

    @Override
    public String defaultRoot()
    {
        return "/usr/local";
    }

    @Override
    public String defaultDyn()
    {
        return "/usr/local/lib";
    }

    @Override
    public String defaultThreads()
    {
        return "1";
    }

    @Override
    public String[] transformCommand(String exe)
    {
        // "gm" is a single dispatch binary invoked as "gm convert ...".
        // "-quiet" is dropped: it is not a GraphicsMagick option (GM is quiet by default).
        return new String[]{exe, "convert", "${source}", "SPLIT:${options}", "-strip", "${target}"};
    }

    @Override
    public String[] checkCommand(String exe)
    {
        return new String[]{exe, "version"};
    }

    @Override
    public boolean isOwnVersionOutput(String versionOutput)
    {
        return StringUtils.trimToEmpty(versionOutput).startsWith(GRAPHICSMAGICK_VERSION_PREFIX);
    }

    @Override
    public List<String> alphaRemoveArgs()
    {
        // GraphicsMagick has no "-alpha remove"; the equivalent is compositing onto a white background.
        return List.of("-background", "white", "-flatten");
    }

    @Override
    public boolean flattensWholePsd()
    {
        return true;
    }

    @Override
    public boolean fillsUnspecifiedCropDimension()
    {
        return true;
    }

    @Override
    public long probeExpectedLength()
    {
        return 7913;
    }

    @Override
    public String startupMessage()
    {
        return "This transformer uses GraphicsMagick from the GraphicsMagick Group. " +
                "See the license at http://www.graphicsmagick.org/Copyright.html or in /GraphicsMagick-license.txt";
    }
}
