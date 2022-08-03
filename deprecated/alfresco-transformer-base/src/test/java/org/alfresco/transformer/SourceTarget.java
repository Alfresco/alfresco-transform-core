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

import java.util.Objects;

/**
 * @deprecated will be removed in a future release. Replaced by alfresco-base-t-engine.
 *
 * Source & Target media type pair
 *
 * @author Cezar Leahu
 */
@Deprecated
public class SourceTarget
{
    public final String source;
    public final String target;

    private SourceTarget(final String source, final String target)
    {
        this.source = source;
        this.target = target;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceTarget that = (SourceTarget) o;
        return Objects.equals(source, that.source) &&
               Objects.equals(target, that.target);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(source, target);
    }

    @Override
    public String toString()
    {
        return source + '|' + target;
    }

    public static SourceTarget of(final String source, final String target)
    {
        return new SourceTarget(source, target);
    }
}
