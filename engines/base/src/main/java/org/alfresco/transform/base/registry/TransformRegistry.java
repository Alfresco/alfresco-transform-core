/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transform.base.registry;

import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.registry.AbstractTransformRegistry;
import org.alfresco.transform.registry.CombinedTransformConfig;
import org.alfresco.transform.registry.TransformCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.Comparator;
import java.util.List;

import static org.alfresco.transform.config.CoreVersionDecorator.setCoreVersionOnSingleStepTransformers;

public class TransformRegistry extends AbstractTransformRegistry
{
    private static final Logger log = LoggerFactory.getLogger(TransformRegistry.class);

    @Autowired
    private String coreVersion;
    @Autowired
    private List<TransformConfigSource> transformConfigSources;

    private TransformConfig transformConfigBeforeIncompleteTransformsAreRemoved;

    /**
     * Load the registry on application startup. This allows Components in projects that extend the t-engine base
     * to use @PostConstruct to add to {@code transformConfigSources}, before the registry is loaded.
     */
    @EventListener
    void init(final ContextRefreshedEvent event)
    {
        CombinedTransformConfig combinedTransformConfig = new CombinedTransformConfig();

        transformConfigSources.stream()
            .sorted(Comparator.comparing(TransformConfigSource::getReadFrom))
            .forEach(source -> {
                TransformConfig transformConfig = source.getTransformConfig();
                setCoreVersionOnSingleStepTransformers(transformConfig, coreVersion);
                combinedTransformConfig.addTransformConfig(transformConfig, source.getReadFrom(), source.getBaseUrl(),
                    this);
            });

        transformConfigBeforeIncompleteTransformsAreRemoved = combinedTransformConfig.buildTransformConfig();
        combinedTransformConfig.combineTransformerConfig(this);
        combinedTransformConfig.registerCombinedTransformers(this);
    }

    private TransformCache data = new TransformCache();

    public TransformConfig getTransformConfig()
    {
        return transformConfigBeforeIncompleteTransformsAreRemoved;
    }

    @Override
    public TransformCache getData()
    {
        return data;
    }

    @Override
    protected void logError(String msg)
    {
        log.error(msg);
    }

    @Override
    protected void logWarn(String msg)
    {
        log.warn(msg);
    }
}
