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
package org.alfresco.transform.base.registry;

import org.alfresco.transform.base.CustomTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class CustomTransformers
{
    private static final Logger logger = LoggerFactory.getLogger(CustomTransformers.class);

    @Autowired(required = false)
    private List<CustomTransformer> customTransformers;

    private final Map<String, CustomTransformer> customTransformersByName = new HashMap<>();

    @PostConstruct
    private void initCustomTransformersByName()
    {
        if (customTransformers != null)
        {
            customTransformers.forEach(customTransformer ->
                   customTransformersByName.put(customTransformer.getTransformerName(), customTransformer));

            List<String> nonNullTransformerNames = customTransformers.stream()
                 .map(CustomTransformer::getTransformerName)
                 .filter(Objects::nonNull)
                 .collect(Collectors.toList());

            if (!nonNullTransformerNames.isEmpty())
            {
                logger.info("Custom Transformers:");
                nonNullTransformerNames
                    .stream()
                    .sorted()
                    .map(name -> "  "+name)
                    .forEach(logger::debug);
            }
        }
    }

    public CustomTransformer get(String name)
    {
        CustomTransformer customTransformer = customTransformersByName.get(name);
        return customTransformer == null ? customTransformersByName.get(null) : customTransformer;
    }

    public void put(String name, CustomTransformer  customTransformer)
    {
        customTransformersByName.put(name, customTransformer);
    }

    public List<CustomTransformer> toList()
    {
        return customTransformers;
    }
}
