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

import org.alfresco.transform.base.TransformEngine;
import org.alfresco.transform.config.TransformConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Makes {@link TransformConfig} from {@link TransformEngine}s available to the {@link TransformRegistry}.
 */
@Component
public class TransformConfigFromTransformEngines
{
    @Autowired(required = false)
    private List<TransformEngine> transformEngines;
    @Autowired
    private List<TransformConfigSource> transformConfigSources;
    @Value("${container.isTRouter}")
    private boolean isTRouter;

    @PostConstruct
    public void initTransformEngineConfig()
    {
        if (transformEngines != null)
        {
            transformEngines.stream()
                .forEach(transformEngine -> {
                    TransformConfig transformConfig = transformEngine.getTransformConfig();
                    if (transformConfig != null) // if not a wrapping TransformEngine like all-in-one
                    {
                        String engineName = transformEngine.getTransformEngineName();
                        transformConfigSources.add(
                            new AbstractTransformConfigSource(engineName, engineName, isTRouter ? null : "---")
                            {
                                 @Override public TransformConfig getTransformConfig()
                                {
                                    return transformEngine.getTransformConfig();
                                }
                            });
                    }
                });
        }
    }
}
