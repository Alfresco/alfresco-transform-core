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

import org.apache.commons.lang3.StringUtils;

public enum MagickBackend
{
    IMAGEMAGICK, GRAPHICSMAGICK;

    public static final String PROPERTY = "transform.core.imagemagick.backend";

    public static MagickBackend from(String value)
    {
        String trimmed = StringUtils.trimToEmpty(value);
        if (trimmed.isEmpty())
        {
            return IMAGEMAGICK;
        }
        for (MagickBackend backend : values())
        {
            if (backend.name().equalsIgnoreCase(trimmed))
            {
                return backend;
            }
        }
        throw new IllegalArgumentException("Unsupported " + PROPERTY + " '" + value
                + "'. Valid values are: imagemagick, graphicsmagick");
    }
}
