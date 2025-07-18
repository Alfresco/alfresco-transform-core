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
 * Base object with {@code transformerName}, {@code sourceMediaType}, {@code targetMediaType}, {@code maxSourceSizeBytes} and {@code priority}.
 */
public abstract class TransformerTypesSizeAndPriority extends TransformerAndTypes
{
    Long maxSourceSizeBytes = null;
    Integer priority = null;

    protected TransformerTypesSizeAndPriority()
    {}

    public Long getMaxSourceSizeBytes()
    {
        return maxSourceSizeBytes;
    }

    public void setMaxSourceSizeBytes(Long maxSourceSizeBytes)
    {
        this.maxSourceSizeBytes = maxSourceSizeBytes;
    }

    public Integer getPriority()
    {
        return priority;
    }

    public void setPriority(Integer priority)
    {
        this.priority = priority;
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
        TransformerTypesSizeAndPriority that = (TransformerTypesSizeAndPriority) o;
        return getMaxSourceSizeBytes().equals(that.getMaxSourceSizeBytes()) &&
                getPriority() == that.getPriority();
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
        if (superToString != null)
            sj.add(superToString);
        if (maxSourceSizeBytes != null)
            sj.add("\"maxSourceSizeBytes\": \"" + maxSourceSizeBytes + '"');
        if (priority != null)
            sj.add("\"priority\": \"" + priority + '"');
        return "{" + sj.toString() + "}";
    }

    public static class Builder<B extends TransformerTypesSizeAndPriority.Builder, T extends TransformerTypesSizeAndPriority>
            extends TransformerAndTypes.Builder<B, T>
    {
        private final T t;

        protected Builder(T t)
        {
            super(t);
            this.t = t;
        }

        public B withMaxSourceSizeBytes(final long maxSourceSizeBytes)
        {
            t.setMaxSourceSizeBytes(maxSourceSizeBytes);
            return (B) this;
        }

        public B withPriority(final int priority)
        {
            t.setPriority(priority);
            return (B) this;
        }
    }
}
