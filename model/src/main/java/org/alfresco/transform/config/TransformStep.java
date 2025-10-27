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

/**
 * Represents a single transform step in a transform pipeline. The last step in the pipeline does not specify the target type as that is based on the supported types and what has been requested.
 */
public class TransformStep
{
    private String transformerName;
    private String targetMediaType;

    public TransformStep()
    {}

    public TransformStep(String transformerName, String targetMediaType)
    {
        this.transformerName = transformerName;
        this.targetMediaType = targetMediaType;
    }

    public String getTransformerName()
    {
        return transformerName;
    }

    public void setTransformerName(String transformerName)
    {
        this.transformerName = transformerName;
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TransformStep that = (TransformStep) o;
        return Objects.equals(transformerName, that.transformerName) &&
                Objects.equals(targetMediaType, that.targetMediaType);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(transformerName, targetMediaType);
    }

    @Override
    public String toString()
    {
        return "TransformStep{" +
                "transformerName='" + transformerName + '\'' +
                ", targetMediaType='" + targetMediaType + '\'' +
                '}';
    }
}
