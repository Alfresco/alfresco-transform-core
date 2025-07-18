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

import org.alfresco.transform.config.OverrideSupported;

/**
 * Key based using {@code transformerName} and {@code sourceMediaType} used as a key to lookup default values held in {@link OverrideSupported} objects.
 */
class TransformerAndSourceType
{
    private String transformerName;
    private String sourceMediaType;

    public TransformerAndSourceType(String transformerName, String sourceMediaType)
    {
        this.transformerName = transformerName;
        this.sourceMediaType = sourceMediaType;
    }

    public void setTransformerName(String transformerName)
    {
        this.transformerName = transformerName;
    }

    public void setSourceMediaType(String sourceMediaType)
    {
        this.sourceMediaType = sourceMediaType;
    }

    public String getTransformerName()
    {
        return transformerName;
    }

    public String getSourceMediaType()
    {
        return sourceMediaType;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        TransformerAndSourceType that = (TransformerAndSourceType) o;
        return Objects.equals(transformerName, that.transformerName) &&
                Objects.equals(sourceMediaType, that.sourceMediaType);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(transformerName, sourceMediaType);
    }
}
