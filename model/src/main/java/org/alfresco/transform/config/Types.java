/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package org.alfresco.transform.config;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Base object with {@code sourceMediaType} and {@code targetMediaType}.
 * Used to identify supported transforms.
 */
public class Types
{
    String sourceMediaType;
    String targetMediaType;

    protected Types() {}

    public String getSourceMediaType()
    {
        return sourceMediaType;
    }

    public void setSourceMediaType(String sourceMediaType)
    {
        this.sourceMediaType = sourceMediaType;
    }

    public String getTargetMediaType()
    {
        return targetMediaType;
    }

    public void setTargetMediaType(String targetMediaType)
    {
        this.targetMediaType = targetMediaType;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Types that = (Types) o;
        return  Objects.equals(sourceMediaType, that.sourceMediaType) &&
                Objects.equals(targetMediaType, that.targetMediaType);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sourceMediaType, targetMediaType);
    }

    @Override
    public String toString()
    {
        StringJoiner sj = new StringJoiner(", ");
        if (sourceMediaType != null) sj.add("\"sourceMediaType\": \""+sourceMediaType+'"');
        if (targetMediaType != null) sj.add("\"targetMediaType\": \""+targetMediaType+'"');
        return sj.toString();
    }

    public static abstract class Builder<B extends Types.Builder, T extends Types>
    {
        final T t;

        protected Builder(T t)
        {
            this.t = t;
        }

        public T build()
        {
            return (T)t;
        }

        public B withSourceMediaType(final String sourceMediaType)
        {
            t.sourceMediaType = sourceMediaType;
            return (B)this;
        }

        public B withTargetMediaType(final String targetMediaType)
        {
            t.targetMediaType = targetMediaType;
            return (B)this;
        }
    }
}
