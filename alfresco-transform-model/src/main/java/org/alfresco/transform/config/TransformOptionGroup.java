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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a group of one or more options. If the group is optional, child options that are marked as required are
 * only required if any child in the group is supplied by the client. If the group is required, child options are
 * optional or required based on their own setting alone.
 *
 * In a pipeline transformation, a group of options
 */
public class TransformOptionGroup extends AbstractTransformOption
{
    private Set<TransformOption> transformOptions = new HashSet<>();

    public TransformOptionGroup()
    {
    }

    public TransformOptionGroup(boolean required, Set<TransformOption> transformOptions)
    {
        super(required);
        this.transformOptions = transformOptions;
    }

    public Set<TransformOption> getTransformOptions()
    {
        return transformOptions;
    }

    public void setTransformOptions(Set<TransformOption> transformOptions)
    {
        this.transformOptions = transformOptions;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TransformOptionGroup that = (TransformOptionGroup) o;
        return Objects.equals(transformOptions, that.transformOptions);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), transformOptions);
    }

    @Override
    public String toString()
    {
        return "TransformOptionGroup{" +
               "transformOptions=" + transformOptions +
               '}';
    }
}