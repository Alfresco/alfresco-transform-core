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
package org.alfresco.transform.client.registry;

import org.alfresco.transform.client.model.config.SupportedDefaults;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

/**
 * Maintains a list of defaults of {@code maxSourceSizeBytes} and {@code priority} keyed on
 * {@code transformerName} and {@code sourceMediaType} so that it can provide a lookup when we
 * join everything together in {@link CombinedTransformConfig#combineTransformerConfig(AbstractTransformRegistry)}.
 *
 * @see SupportedDefaults
 */
class Defaults
{
    private static final Integer DEFAULT_PRIORITY = 50;
    private static final Long DEFAULT_MAX_SOURCE_SIZE_BYTES = -1L;

    private static final TransformerAndSourceType SYSTEM_WIDE_KEY = new TransformerAndSourceType(null, null);

    private final Map<TransformerAndSourceType, Integer> priorityDefaults = new HashMap<>();
    private final Map<TransformerAndSourceType, Long> maxSourceSizeBytesDefaults = new HashMap<>();

    public void add(SupportedDefaults supportedDefault)
    {
        TransformerAndSourceType key =
                new TransformerAndSourceType(supportedDefault.getTransformerName(), supportedDefault.getSourceMediaType());
        Long maxSourceSizeBytes = supportedDefault.getMaxSourceSizeBytes();
        if (maxSourceSizeBytes != null)
        {
            maxSourceSizeBytesDefaults.put(key, maxSourceSizeBytes);
        }
        Integer priority = supportedDefault.getPriority();
        if (priority != null)
        {
            priorityDefaults.put(key, priority);
        }
    }

    Defaults()
    {
        clear();
    }

    public boolean valuesUnset(Integer supportedSourceAndTargetPriority, Long supportedSourceAndTargetMaxSourceSizeBytes)
    {
        return supportedSourceAndTargetPriority == null || supportedSourceAndTargetMaxSourceSizeBytes == null;
    }

    public int getPriority(String transformerName, String sourceMediaType, Integer supportedSourceAndTargetPriority)
    {
        return getDefault(transformerName, sourceMediaType, supportedSourceAndTargetPriority, priorityDefaults);
    }

    public long getMaxSourceSizeBytes(String transformerName, String sourceMediaType, Long supportedSourceAndTargetMaxSourceSizeBytes)
    {
        return getDefault(transformerName, sourceMediaType, supportedSourceAndTargetMaxSourceSizeBytes, maxSourceSizeBytesDefaults);
    }

    private <T> T getDefault(String transformerName, String sourceMediaType, T supportedSourceAndTargetValue,
                            Map<TransformerAndSourceType, T> map)
    {
        if (supportedSourceAndTargetValue != null)
        {
            return supportedSourceAndTargetValue;
        }

        // 0: transformer and source media type default
        // 1: transformer default
        // 2: source media type default
        // 3: system wide default
        TransformerAndSourceType key = new TransformerAndSourceType(transformerName, sourceMediaType);
        for (int i=0; ; i++)
        {
            T value = map.get(key);
            if (value != null)
            {
                return value;
            }

            switch (i)
            {
                case 0:
                case 2: key.setSourceMediaType(null);                                          break;
                case 1: key.setSourceMediaType(sourceMediaType); key.setTransformerName(null); break;
                default: throw new IllegalStateException("Should have found an entry with a null, null lookup");
            }
        }
    }

    public Set<SupportedDefaults> getSupportedDefaults()
    {
        return Stream.concat(maxSourceSizeBytesDefaults.keySet().stream(), priorityDefaults.keySet().stream())
                .filter(key ->
                {
                    // Discard the entry added by clear()
                    return !SYSTEM_WIDE_KEY.equals(key) ||
                           !DEFAULT_MAX_SOURCE_SIZE_BYTES.equals(maxSourceSizeBytesDefaults.get(key)) ||
                           !DEFAULT_PRIORITY.equals(priorityDefaults.get(key));
                })
                .map(key ->
                {
                    Long maxSourceSizeBytes = maxSourceSizeBytesDefaults.get(key);
                    Integer priority = priorityDefaults.get(key);
                    return SupportedDefaults.builder()
                            .withTransformerName(key.getTransformerName())
                            .withSourceMediaType(key.getSourceMediaType())
                            .withPriority(priority)
                            .withMaxSourceSizeBytes(maxSourceSizeBytes)
                            .build();
                }).collect(toSet());
    }

    public void clear()
    {
        priorityDefaults.clear();
        maxSourceSizeBytesDefaults.clear();

        priorityDefaults.put(SYSTEM_WIDE_KEY, DEFAULT_PRIORITY);
        maxSourceSizeBytesDefaults.put(SYSTEM_WIDE_KEY, DEFAULT_MAX_SOURCE_SIZE_BYTES);
    }
}
