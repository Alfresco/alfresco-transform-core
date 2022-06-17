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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Transform Configuration with multiple {@link Transformer}s and {@link TransformOption}s.
 * It can be used for one or more Transformers.
 */
public class TransformConfig
{
    private Map<String, Set<TransformOption>> transformOptions = new HashMap<>();

    private List<Transformer> transformers = new ArrayList<>();

    private Set<String> removeTransformers = new HashSet<>();

    private Set<AddSupported> addSupported = new HashSet<>();

    private Set<RemoveSupported> removeSupported = new HashSet<>();

    private Set<OverrideSupported> overrideSupported = new HashSet<>();

    private Set<SupportedDefaults> supportedDefaults = new HashSet<>();

    public Map<String, Set<TransformOption>> getTransformOptions()
    {
        return transformOptions;
    }

    public void setTransformOptions(Map<String, Set<TransformOption>> transformOptions)
    {
        this.transformOptions = transformOptions;
    }

    public List<Transformer> getTransformers()
    {
        return transformers;
    }

    public Set<String> getRemoveTransformers()
    {
        return removeTransformers;
    }

    public Set<AddSupported> getAddSupported()
    {
        return addSupported;
    }

    public Set<RemoveSupported> getRemoveSupported()
    {
        return removeSupported;
    }

    public Set<OverrideSupported> getOverrideSupported()
    {
        return overrideSupported;
    }

    public Set<SupportedDefaults> getSupportedDefaults()
    {
        return supportedDefaults;
    }

    public void setTransformers(List<Transformer> transformers)
    {
        this.transformers = transformers;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransformConfig that = (TransformConfig) o;
        return  transformOptions.equals(that.transformOptions) &&
                transformers.equals(that.transformers) &&
                removeTransformers.equals(that.removeTransformers) &&
                addSupported.equals(that.addSupported) &&
                removeSupported.equals(that.removeSupported) &&
                overrideSupported.equals(that.overrideSupported) &&
                supportedDefaults.equals(that.supportedDefaults);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(transformOptions, transformers, removeTransformers, addSupported, removeSupported,
                overrideSupported, supportedDefaults);
    }

    @Override
    public String toString()
    {
        return "TransformConfig{" +
               "transformOptions=" + transformOptions +
               ", transformers=" + transformers +
               '}';
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private final TransformConfig transformConfig = new TransformConfig();

        private Builder() {}

        public TransformConfig build()
        {
            return transformConfig;
        }

        public Builder withTransformOptions(final Map<String, Set<TransformOption>> transformOptions)
        {
            transformConfig.transformOptions = transformOptions;
            return this;
        }

        public Builder withTransformers(final List<Transformer> transformers)
        {
            transformConfig.transformers = transformers;
            return this;
        }

        public Builder withRemoveTransformers(final Set<String> removeTransformers)
        {
            transformConfig.removeTransformers = removeTransformers;
            return this;
        }

        public Builder withAddSupported(final Set<AddSupported> addSupported)
        {
            transformConfig.addSupported = addSupported;
            return this;
        }

        public Builder withRemoveSupported(final Set<RemoveSupported> removeSupported)
        {
            transformConfig.removeSupported = removeSupported;
            return this;
        }

        public Builder withOverrideSupported(final Set<OverrideSupported> overrideSupported)
        {
            transformConfig.overrideSupported = overrideSupported;
            return this;
        }

        public Builder withSupportedDefaults(final Set<SupportedDefaults> supportedDefaults)
        {
            transformConfig.supportedDefaults = supportedDefaults;
            return this;
        }
    }
}
