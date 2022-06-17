/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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
package org.alfresco.transform;

import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transformer.probes.ProbeTestTransform;

import java.util.Set;

/**
 * The interface to the custom transform code applied on top of a base t-engine.
 */
public interface TransformEngine
{
    /**
     * @return the name of the t-engine. The t-router reads config from t-engines in name order.
     */
    String getTransformEngineName();

    /**
     * @return a definition of what the t-engine supports. Normally read from a json Resource on the classpath.
     */
    TransformConfig getTransformConfig();

    /**
     * @return actual transform codes.
     */
    Set<CustomTransformer> getTransformers();

    /**
     * @return a ProbeTestTransform (will do a quick transform) for k8 liveness and readiness probes.
     */
    ProbeTestTransform getLivenessAndReadinessProbeTestTransform();
}
