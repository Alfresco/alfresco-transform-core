/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2026 Alfresco Software Limited
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.alfresco.transform.config.OverrideSupported;

/**
 * Holds override information for deferred processing after wildcard generation. This is necessary because pipeline transformers have empty supportedSourceAndTargetList until addWildcardSupportedSourceAndTarget() is called. Also provides logic to apply deferred overrides to a list of transformers.
 */
public class DeferredOverride
{
    private final OverrideSupported overrideSupported;
    private final String readFrom;

    public DeferredOverride(OverrideSupported overrideSupported, String readFrom)
    {
        this.overrideSupported = overrideSupported;
        this.readFrom = readFrom;
    }

    public OverrideSupported getOverrideSupported()
    {
        return overrideSupported;
    }

    public String getReadFrom()
    {
        return readFrom;
    }

    /**
     * Applies all stored overrides AFTER wildcard generation. This ensures that pipeline and failover transformers have their supportedSourceAndTargetList populated before overrides are applied.
     *
     * @param deferredOverrides
     *            List of DeferredOverride objects
     * @param combinedTransformers
     *            List of Origin<Transformer> to apply overrides to
     * @param registry
     *            Used for logging
     */
    public static void applyDeferredOverrides(List<DeferredOverride> deferredOverrides, List<Origin<org.alfresco.transform.config.Transformer>> combinedTransformers, AbstractTransformRegistry registry)
    {
        if (deferredOverrides.isEmpty())
        {
            return;
        }

        Map<String, Set<OverrideSupported>> leftOverBySource = new HashMap<>();
        for (DeferredOverride deferredOverride : deferredOverrides)
        {
            OverrideSupported override = deferredOverride.getOverrideSupported();
            String readFrom = deferredOverride.getReadFrom();
            boolean found = false;

            for (Origin<org.alfresco.transform.config.Transformer> transformerOrigin : combinedTransformers)
            {
                org.alfresco.transform.config.Transformer transformer = transformerOrigin.get();
                if (transformer.getTransformerName().equals(override.getTransformerName()))
                {
                    Set<org.alfresco.transform.config.SupportedSourceAndTarget> supportedList = transformer.getSupportedSourceAndTargetList();
                    org.alfresco.transform.config.SupportedSourceAndTarget existingSupported = supportedList.stream()
                            .filter(supported -> supported.getSourceMediaType().equals(override.getSourceMediaType()) &&
                                    supported.getTargetMediaType().equals(override.getTargetMediaType()))
                            .findFirst()
                            .orElse(null);
                    if (existingSupported != null)
                    {
                        supportedList.remove(existingSupported);
                        existingSupported.setMaxSourceSizeBytes(override.getMaxSourceSizeBytes());
                        existingSupported.setPriority(override.getPriority());
                        supportedList.add(existingSupported);
                        found = true;
                        break;
                    }
                }
            }
            if (!found)
            {
                leftOverBySource.computeIfAbsent(readFrom, k -> new HashSet<>()).add(override);
            }
        }
        // Warn about overrides that didn't match anything
        leftOverBySource.forEach((readFrom, leftOvers) -> {
            if (!leftOvers.isEmpty())
            {
                StringJoiner sj = new StringJoiner(", ",
                        "Unable to process \"overrideSupported\": [", "]. Read from " + readFrom);
                leftOvers.forEach(override -> sj.add(override.toString()));
                registry.logWarn(sj.toString());
            }
        });
        deferredOverrides.clear();
    }
}
