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
package org.alfresco.transform.base;

import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.registry.AbstractTransformRegistry;
import org.alfresco.transform.registry.CombinedTransformConfig;
import org.alfresco.transform.registry.TransformCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;

import static org.alfresco.transform.config.CoreVersionDecorator.setCoreVersionOnSingleStepTransformers;

/**
 * Used by clients to work out if a transformation is supported based on the engine_config.json.
 */
public class TransformRegistryImpl extends AbstractTransformRegistry
{
    private static final Logger log = LoggerFactory.getLogger(TransformRegistryImpl.class);

    @Autowired(required = false)
    private List<TransformEngine> transformEngines;

    @Autowired
    private String coreVersion;

    private TransformConfig transformConfigBeforeIncompleteTransformsAreRemoved;

    @PostConstruct
    public void init()
    {
        CombinedTransformConfig combinedTransformConfig = new CombinedTransformConfig();
        if (transformEngines != null)
        {
            transformEngines.stream()
                        .sorted(Comparator.comparing(TransformEngine::getTransformEngineName))
                        .forEach(transformEngine -> {
                            TransformConfig transformConfig = transformEngine.getTransformConfig();
                            if (transformConfig != null) // if not a wrapping TransformEngine like all-in-one
                            {
                                setCoreVersionOnSingleStepTransformers(transformConfig, coreVersion);
                                combinedTransformConfig.addTransformConfig(transformConfig,
                                        transformEngine.getTransformEngineName(), "---", this);
                            }
                        });
        }
        transformConfigBeforeIncompleteTransformsAreRemoved = combinedTransformConfig.buildTransformConfig();
        combinedTransformConfig.combineTransformerConfig(this);
        combinedTransformConfig.registerCombinedTransformers(this);
    }

    // Unlike other subclasses this class does not extend Data or replace it at run time.
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
