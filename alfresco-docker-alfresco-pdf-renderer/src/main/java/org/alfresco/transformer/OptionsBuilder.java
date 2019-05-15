/*
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
 *
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.transformer;

import static org.alfresco.transformer.util.Util.stringToBoolean;
import static org.alfresco.transformer.util.Util.stringToInteger;

import java.util.StringJoiner;

/**
 * PdfRenderer options builder.
 *
 * @author Cezar Leahu
 */
final class OptionsBuilder
{
    private Integer page;
    private Integer width;
    private Integer height;
    private Boolean allowPdfEnlargement;
    private Boolean maintainPdfAspectRatio;

    private OptionsBuilder()
    {
    }

    public OptionsBuilder withPage(final String page)
    {
        return withPage(stringToInteger(page));
    }

    public OptionsBuilder withPage(final Integer page)
    {
        this.page = page;
        return this;
    }

    public OptionsBuilder withWidth(final String width)
    {
        return withWidth(stringToInteger(width));
    }

    public OptionsBuilder withWidth(final Integer width)
    {
        this.width = width;
        return this;
    }

    public OptionsBuilder withHeight(final String height)
    {
        return withHeight(stringToInteger(height));
    }

    public OptionsBuilder withHeight(final Integer height)
    {
        this.height = height;
        return this;
    }

    public OptionsBuilder withAllowPdfEnlargement(final String allowPdfEnlargement)
    {
        return withAllowPdfEnlargement(stringToBoolean(allowPdfEnlargement));
    }

    public OptionsBuilder withAllowPdfEnlargement(final Boolean allowPdfEnlargement)
    {
        this.allowPdfEnlargement = allowPdfEnlargement;
        return this;
    }

    public OptionsBuilder withMaintainPdfAspectRatio(final String maintainPdfAspectRatio)
    {
        return withMaintainPdfAspectRatio(stringToBoolean(maintainPdfAspectRatio));
    }

    public OptionsBuilder withMaintainPdfAspectRatio(final Boolean maintainPdfAspectRatio)
    {
        this.maintainPdfAspectRatio = maintainPdfAspectRatio;
        return this;
    }

    public String build()
    {
        StringJoiner args = new StringJoiner(" ");
        if (width != null && width >= 0)
        {
            args.add("--width=" + width);
        }
        if (height != null && height >= 0)
        {
            args.add("--height=" + height);
        }
        if (allowPdfEnlargement != null && allowPdfEnlargement)
        {
            args.add("--allow-enlargement");
        }
        if (maintainPdfAspectRatio != null && maintainPdfAspectRatio)
        {
            args.add("--maintain-aspect-ratio");
        }
        if (page != null && page >= 0)
        {
            args.add("--page=" + page);
        }
        return args.toString();
    }

    public static OptionsBuilder builder()
    {
        return new OptionsBuilder();
    }
}
