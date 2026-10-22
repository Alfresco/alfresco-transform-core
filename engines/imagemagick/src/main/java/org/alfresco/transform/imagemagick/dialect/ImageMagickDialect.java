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

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class ImageMagickDialect implements MagickDialect
{
    static final String GRAPHICSMAGICK_VERSION_PREFIX = "GraphicsMagick";

    @Override
    public MagickBackend backend()
    {
        return MagickBackend.IMAGEMAGICK;
    }

    @Override
    public String displayName()
    {
        return "ImageMagick";
    }

    @Override
    public String defaultExe()
    {
        return "/usr/bin/convert";
    }

    @Override
    public String defaultRoot()
    {
        return "/usr/lib64/ImageMagick-7.1.2";
    }

    @Override
    public String defaultDyn()
    {
        return "/usr/lib64/ImageMagick-7.1.2/lib";
    }

    @Override
    public String defaultThreads()
    {
        return null;
    }

    @Override
    public String[] transformCommand(String exe)
    {
        return new String[]{exe, "${source}", "SPLIT:${options}", "-strip", "-quiet", "${target}"};
    }

    @Override
    public String[] checkCommand(String exe)
    {
        return new String[]{exe, "-version"};
    }

    @Override
    public boolean isOwnVersionOutput(String versionOutput)
    {
        String output = StringUtils.trimToEmpty(versionOutput);
        return output.contains("ImageMagick") && !output.startsWith(GRAPHICSMAGICK_VERSION_PREFIX);
    }

    @Override
    public List<String> alphaRemoveArgs()
    {
        return List.of("-alpha", "remove");
    }

    @Override
    public boolean flattensWholePsd()
    {
        return false;
    }

    @Override
    public boolean fillsUnspecifiedCropDimension()
    {
        return false;
    }

    @Override
    public long probeExpectedLength()
    {
        return 25383;
    }

    @Override
    public String startupMessage()
    {
        return "This transformer uses ImageMagick from ImageMagick Studio LLC. " +
                "See the license at http://www.imagemagick.org/script/license.php or in /ImageMagick-license.txt";
    }
}
