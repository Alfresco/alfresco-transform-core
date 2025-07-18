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
 * Base object with {@code transformerName}, {@code sourceMediaType}and {@code targetMediaType}. Used to identify supported transforms.
 */
public abstract class TransformerAndTypes extends Types
{
    String transformerName;

    protected TransformerAndTypes()
    {}

    public String getTransformerName()
    {
        return transformerName;
    }

    public void setTransformerName(String transformerName)
    {
        this.transformerName = transformerName;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        TransformerAndTypes that = (TransformerAndTypes) o;
        return Objects.equals(transformerName, that.transformerName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), transformerName);
    }

    @Override
    public String toString()
    {
        StringJoiner sj = new StringJoiner(", ");
        String superToString = super.toString();
        if (transformerName != null)
            sj.add("\"transformerName\": \"" + transformerName + '"');
        if (superToString != null)
            sj.add(superToString);
        return sj.toString();
    }

    public static class Builder<B extends TransformerAndTypes.Builder, T extends TransformerAndTypes>
            extends Types.Builder<B, T>
    {
        private final T t;

        protected Builder(T t)
        {
            super(t);
            this.t = t;
        }

        public B withTransformerName(final String transformerName)
        {
            t.transformerName = transformerName;
            return (B) this;
        }
    }
}
