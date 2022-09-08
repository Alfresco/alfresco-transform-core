/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2022 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * -
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * -
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * -
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * -
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transform.base.html;

import org.alfresco.transform.config.TransformOption;
import org.alfresco.transform.config.TransformOptionGroup;
import org.alfresco.transform.config.TransformOptionValue;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Used in the html test page, which provides a list of known transform option names.
 */
@Component
public class OptionsHelper
{
    private OptionsHelper()
    {
    }

    public static Set<String> getOptionNames(Map<String, Set<TransformOption>> transformOptionsByName)
    {
        Set<String> set = new TreeSet<>();
        transformOptionsByName.forEach(((optionName, optionSet) ->
            optionSet.stream().forEach(option -> addOption(set, option))));
        return set;
    }

    private static void addOption(Set<String> set, TransformOption option)
    {
        if (option instanceof TransformOptionGroup)
        {
            addGroup(set, (TransformOptionGroup)option);
        }
        else
        {
            addValue(set, (TransformOptionValue)option);
        }
    }

    private static void addGroup(Set<String> set, TransformOptionGroup group)
    {
        group.getTransformOptions().stream().forEach(option -> addOption(set, option));
    }

    private static void addValue(Set<String> set, TransformOptionValue value)
    {
        set.add(value.getName());
    }
}
