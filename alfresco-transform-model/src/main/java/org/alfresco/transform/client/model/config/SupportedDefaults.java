/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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
package org.alfresco.transform.client.model.config;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Holds information to provide default {@code priority} and / or {@code maxSourceSizeBytes} defaults. In precedence
 * order from most specific to most general these are defined by combinations of {@code transformerName} and
 * {@code sourceMediaType}:<p><br>
 * <ul>
 *     <li><b>transformer and source media type default</b> {@code transformerName} + {@code sourceMediaType}</li>
 *     <li><b>transformer default</b> {@code transformerName}</li>
 *     <li><b>source media type default</b> {@code sourceMediaType}</li>
 *     <li><b>system wide default</b> none</li>
 * </ul><br>
 *
 * Both {@code maxSourceSizeBytes} and {@code priority} may be specified in a {@code "supportedDefaults"} element, but
 * if only one is specified it is only that value that is being defaulted at the level specified by the combination of
 * {@code transformerName} and {@code sourceMediaType}.<p><br>
 *
 * <pre>
 *   "supportedDefaults" : [
 *     {
 *       "transformerName": "Office",              // default for a source type within a transformer
 *       "sourceMediaType": "application/zip",
 *       "maxSourceSizeBytes": 18874368
 *     },
 *     {
 *       "sourceMediaType": "application/msword",  // defaults for a source type
 *       "maxSourceSizeBytes": 4194304,
 *       "priority": 45
 *     },
 *     {
 *       "priority": 60                            // system default
 *     }
 *     {
 *       "maxSourceSizeBytes": -1                  // system default
 *     }
 *   ]
 * </pre>
 */
public class SupportedDefaults
{
    String transformerName;
    String sourceMediaType;
    Long maxSourceSizeBytes = null;
    Integer priority = null;

    public String getTransformerName()
    {
        return transformerName;
    }

    public void setTransformerName(String transformerName)
    {
        this.transformerName = transformerName;
    }

    public String getSourceMediaType()
    {
        return sourceMediaType;
    }

    public void setSourceMediaType(String sourceMediaType)
    {
        this.sourceMediaType = sourceMediaType;
    }

    public Long getMaxSourceSizeBytes()
    {
        return maxSourceSizeBytes;
    }

    public void setMaxSourceSizeBytes(long maxSourceSizeBytes)
    {
        this.maxSourceSizeBytes = maxSourceSizeBytes;
    }

    public Integer getPriority()
    {
        return priority;
    }

    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupportedDefaults that = (SupportedDefaults)o;
        return Objects.equals(transformerName, that.transformerName) &&
                Objects.equals(sourceMediaType, that.sourceMediaType) &&
                Objects.equals(maxSourceSizeBytes, that.maxSourceSizeBytes) &&
                Objects.equals(priority, that.priority);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(transformerName, sourceMediaType, maxSourceSizeBytes, priority);
    }

    @Override
    public String toString()
    {
        StringJoiner sj = new StringJoiner(", ");
        if (transformerName != null) sj.add("\"transformerName\": \""+transformerName+'"');
        if (sourceMediaType != null) sj.add("\"sourceMediaType\": \""+sourceMediaType+'"');
        if (maxSourceSizeBytes != null) sj.add("\"maxSourceSizeBytes\": \""+maxSourceSizeBytes+'"');
        if (priority != null) sj.add("\"priority\": \""+priority+'"');
        return "{" + sj.toString() + "}";
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        final SupportedDefaults supportedDefaults;

        protected Builder()
        {
            this.supportedDefaults = new SupportedDefaults();
        }

        public SupportedDefaults build()
        {
            return supportedDefaults;
        }

        public Builder withTransformerName(final String transformerName)
        {
            supportedDefaults.transformerName = transformerName;
            return this;
        }

        public Builder withSourceMediaType(final String sourceMediaType)
        {
            supportedDefaults.sourceMediaType = sourceMediaType;
            return this;
        }

        public Builder withMaxSourceSizeBytes(final Long maxSourceSizeBytes)
        {
            supportedDefaults.maxSourceSizeBytes = maxSourceSizeBytes;
            return this;
        }

        public Builder withPriority(final Integer priority)
        {
            supportedDefaults.priority = priority;
            return this;
        }
    }
}
