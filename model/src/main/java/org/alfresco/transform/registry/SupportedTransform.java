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
package org.alfresco.transform.registry;

import java.util.Objects;
import java.util.Set;

import org.alfresco.transform.config.TransformOption;
import org.alfresco.transform.config.TransformOptionGroup;

public class SupportedTransform
{
    private final TransformOptionGroup transformOptions;
    private final long maxSourceSizeBytes;
    private final String name;
    private final int priority;

    SupportedTransform(String name, Set<TransformOption> transformOptions,
            long maxSourceSizeBytes, int priority)
    {
        // Logically the top level TransformOptionGroup is required, so that child options are optional or required
        // based on their own setting.
        this.transformOptions = new TransformOptionGroup(true, transformOptions);
        this.maxSourceSizeBytes = maxSourceSizeBytes;
        this.name = name;
        this.priority = priority;
    }

    public TransformOptionGroup getTransformOptions()
    {
        return transformOptions;
    }

    public long getMaxSourceSizeBytes()
    {
        return maxSourceSizeBytes;
    }

    public String getName()
    {
        return name;
    }

    public int getPriority()
    {
        return priority;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SupportedTransform that = (SupportedTransform) o;
        return maxSourceSizeBytes == that.maxSourceSizeBytes &&
                priority == that.priority &&
                Objects.equals(transformOptions, that.transformOptions) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(transformOptions, maxSourceSizeBytes, name, priority);
    }

    @Override
    public String toString()
    {
        return name + ':' + maxSourceSizeBytes + ':' + priority;
    }
}
