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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Map.Entry;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.alfresco.transform.config.TransformOption;
import org.alfresco.transform.config.TransformOptionGroup;
import org.alfresco.transform.config.TransformOptionValue;
import org.alfresco.transform.common.TransformException;

class TransformRegistryHelper
{
    private static final String TIMEOUT = "timeout";

    static Set<TransformOption> lookupTransformOptions(final Set<String> transformOptionNames,
        final Map<String, Set<TransformOption>> transformOptions, final String readFrom,
        final Consumer<String> logError)
    {
        if (transformOptionNames == null)
        {
            return emptySet();
        }

        final Set<TransformOption> options = new HashSet<>();
        for (String name : transformOptionNames)
        {
            final Set<TransformOption> oneSetOfTransformOptions = transformOptions.get(name);
            if (oneSetOfTransformOptions == null)
            {
                logError.accept("transformOptions in " + readFrom + " with the name " + name +
                                " does not exist. Ignored");
                continue;
            }
            options.add(new TransformOptionGroup(false, oneSetOfTransformOptions));
        }

        return options.size() == 1 ?
               ((TransformOptionGroup) options.iterator().next()).getTransformOptions() :
               options;
    }

    // Returns transformers in increasing supported size order, where lower priority transformers for the same size have
    // been discarded.
    static List<SupportedTransform> retrieveTransformListBySize(final TransformCache data,
        final String sourceMimetype, final String targetMimetype,
        Map<String, String> actualOptions, String transformerName)
    {
        if (actualOptions == null)
        {
            actualOptions = emptyMap();
        }
        if (transformerName != null && transformerName.trim().isEmpty())
        {
            transformerName = null;
        }

        final List<SupportedTransform> cachedTransformList =
            transformerName == null ? null :
            data.retrieveCached(transformerName, sourceMimetype);
        if (cachedTransformList != null)
        {
            return cachedTransformList;
        }

        final List<SupportedTransform> builtTransformList = buildTransformList(data,
            sourceMimetype,
            targetMimetype,
            filterTimeout(actualOptions));

        if (transformerName != null)
        {
            data.cache(transformerName, sourceMimetype, builtTransformList);
        }

        return builtTransformList;
    }

    private static List<SupportedTransform> buildTransformList(
        final TransformCache data, final String sourceMimetype, final String targetMimetype,
        final Map<String, String> actualOptions)
    {

        if(sourceMimetype == null)
        {
          throw new TransformException(400, "Null value provided for sourceMimetype, please provide a value");
        }

        if(targetMimetype == null)
        {
          throw new TransformException(400, "Null value provided for tragetMimetype, please provide a value");
        }

        final Map<String, List<SupportedTransform>> targetMap = data.retrieveTransforms(
            sourceMimetype);

        final List<SupportedTransform> supportedTransformList = targetMap.getOrDefault(
            targetMimetype, emptyList());

        final List<SupportedTransform> transformListBySize = new ArrayList<>();

        for (SupportedTransform supportedTransform : supportedTransformList)
        {
            final Map<String, Boolean> possibleTransformOptions = gatherPossibleTransformOptions(
                supportedTransform.getTransformOptions(), actualOptions);

            if (optionsMatch(possibleTransformOptions, actualOptions))
            {
                addToSupportedTransformList(transformListBySize, supportedTransform);
            }
        }
        return transformListBySize;
    }

    // Add newTransform to the transformListBySize in increasing size order and discards
    // lower priority (numerically higher) transforms with a smaller or equal size.
    private static void addToSupportedTransformList(
        final List<SupportedTransform> transformListBySize,
        final SupportedTransform newTransform)
    {
        if (transformListBySize.isEmpty())
        {
            transformListBySize.add(newTransform);
        }
        else
        {
            for (int i = 0; i < transformListBySize.size(); i++)
            {
                final SupportedTransform existingTransform = transformListBySize.get(i);
                final int compareMaxSize = compareMaxSize(newTransform.getMaxSourceSizeBytes(),
                        existingTransform.getMaxSourceSizeBytes());
                final int comparePriority = existingTransform.getPriority() - newTransform.getPriority();

                if (compareMaxSize == 0)
                {
                    if (comparePriority == 0)
                    {
                        // If same priority and size limit, replace with the newer transform.
                        // It is possibly a replacement in an extension.
                        transformListBySize.set(i, newTransform);
                        break;
                    }
                    else if (comparePriority > 0)
                    {
                        // Replace as newer one is higher priority and try to discard some existing ones.
                        transformListBySize.set(i, newTransform);
                        discardFromSupportedTransformList(transformListBySize, i);
                        break;
                    }
                    else
                    {
                        // Ignore as lower priority
                        break;
                    }
                }
                else if (compareMaxSize < 0)
                {
                    if (comparePriority > 0)
                    {
                        // If higher priority insert and try to discard some existing ones.
                        transformListBySize.add(i, newTransform);
                        discardFromSupportedTransformList(transformListBySize, i);
                        break;
                    }
                    else
                    {
                        // Ignore the newer one as its priority is lower or the same as one that has a higher size limit
                        break;
                    }
                }
                else // if (compareMaxSize > 0)
                {
                    if (comparePriority < 0)
                    {
                        if (i+1 < transformListBySize.size())
                        {
                            // Look at the next element as size is higher but the priority is lower.
                            continue;
                        }
                        else
                        {
                            // Append to the list as the size is higher but the priority is lower.
                            transformListBySize.add(newTransform);
                            break;
                        }
                    }
                    // Else same or better priority and higher size limit, so replace with the newer transform and try
                    // to discard some existing ones.
                    transformListBySize.set(i, newTransform);
                    discardFromSupportedTransformList(transformListBySize, i);
                    break;
                }
            }
        }
    }

    // Starting at i+1, try to remove transforms that will not be used.
    private static void discardFromSupportedTransformList(List<SupportedTransform> transformListBySize, int i)
    {
        SupportedTransform newTransform = transformListBySize.get(i++);
        while (i < transformListBySize.size())
        {
            final SupportedTransform existingTransform = transformListBySize.get(i);
            final int compareMaxSize = compareMaxSize(newTransform.getMaxSourceSizeBytes(),
                    existingTransform.getMaxSourceSizeBytes());
            final int comparePriority = existingTransform.getPriority() - newTransform.getPriority();

            // Discard those with
            // 1) the same priority but support a smaller size
            // 2) those with a lower priority and a smaller size
            if ((comparePriority == 0 && compareMaxSize >= 0) ||
                (comparePriority > 0 && compareMaxSize >= 0))
            {
                transformListBySize.remove(i);
            }
            else
            {
                break;
            }
        }
    }

    private static Map<String, Boolean> gatherPossibleTransformOptions(
        final TransformOptionGroup transformOptionGroup, final Map<String, String> actualOptions)
    {
        final Map<String, Boolean> possibleTransformOptions = new HashMap<>();
        addToPossibleTransformOptions(possibleTransformOptions, transformOptionGroup, true,
            actualOptions);
        return possibleTransformOptions;
    }

    /**
     * Flatten out the transform options by adding them to the supplied possibleTransformOptions.</p>
     *
     * If possible discards options in the supplied transformOptionGroup if the group is optional and the actualOptions
     * don't provide any of the options in the group. Or to put it another way:<p/>
     *
     * It adds individual transform options from the transformOptionGroup to possibleTransformOptions if the group is
     * required or if the actualOptions include individual options from the group. As a result it is possible that none
     * of the group are added if it is optional. It is also possible to add individual transform options that are
     * themselves required but not in the actualOptions. In this the optionsMatch method will return false.
     *
     * @return true if any options were added. Used by nested call parents to determine if an option was added from a
     * nested sub group.
     */
    static boolean addToPossibleTransformOptions(
        final Map<String, Boolean> possibleTransformOptions,
        final TransformOptionGroup transformOptionGroup, final Boolean parentGroupRequired,
        final Map<String, String> actualOptions)
    {
        boolean added = false;
        boolean required = false;

        final Set<TransformOption> optionList = transformOptionGroup.getTransformOptions();
        if (optionList != null && !optionList.isEmpty())
        {
            // We need to avoid adding options from a group that is required but its parents are not.
            boolean transformOptionGroupRequired = transformOptionGroup.isRequired() && parentGroupRequired;

            // Check if the group contains options in actualOptions. This will add any options from sub groups.
            for (final TransformOption transformOption : optionList)
            {
                if (transformOption instanceof TransformOptionGroup)
                {
                    added = addToPossibleTransformOptions(possibleTransformOptions,
                        (TransformOptionGroup) transformOption, transformOptionGroupRequired,
                        actualOptions);
                    required |= added;
                }
                else
                {
                    final String name = ((TransformOptionValue) transformOption).getName();
                    if (actualOptions.containsKey(name))
                    {
                        required = true;
                    }
                }
            }

            if (required || transformOptionGroupRequired)
            {
                for (TransformOption transformOption : optionList)
                {
                    if (transformOption instanceof TransformOptionValue)
                    {
                        added = true;
                        final TransformOptionValue option = (TransformOptionValue) transformOption;
                        possibleTransformOptions.put(option.getName(), option.isRequired());
                    }
                }
            }
        }

        return added;
    }

    // compare where -1 is unlimited.
    private static int compareMaxSize(final long a, final long b)
    {
        return a == -1 ? b == -1 ? 0 : 1 : b == -1 ? -1 : Long.compare(a, b);
    }

    static boolean optionsMatch(final Map<String, Boolean> transformOptions,
        final Map<String, String> actualOptions)
    {
        // Check all required transformOptions are supplied
        final boolean supported = transformOptions
            .entrySet()
            .stream()
            .filter(Entry::getValue)// filter by the required status
            .map(Entry::getKey)// map to the option name
            .allMatch(actualOptions::containsKey);

        if (!supported)
        {
            return false;
        }

        // Check there are no extra unused actualOptions
        return actualOptions
            .keySet()
            .stream()
            .allMatch(transformOptions::containsKey);
    }

    private static Map<String, String> filterTimeout(final Map<String, String> options)
    {
        // Remove the "timeout" property from the actualOptions as it is not used to select a transformer.
        if (!options.containsKey(TIMEOUT))
        {
            return options;
        }
        return options
            .entrySet()
            .stream()
            .filter(e -> !TIMEOUT.equals(e.getKey()))
            .collect(toMap(Entry::getKey, Entry::getValue));
    }
}
