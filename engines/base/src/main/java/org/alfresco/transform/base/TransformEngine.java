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
package org.alfresco.transform.base;

import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.config.reader.TransformConfigResourceReader;
import org.alfresco.transform.config.TransformConfig;

/**
 * Interface to be implemented by transform specific code. Provides information about the t-engine as a whole.
 * Also see {@link CustomTransformer} which provides the code that performs transformation. There may be several
 * in a single t-engine. So that it is automatically picked up, it must exist in a package under
 * {@code org.alfresco.transform} and have the Spring {@code @Component} annotation.
 */
public interface TransformEngine
{
    /**
     * @return the name of the t-engine. The t-router reads config from t-engines in name order.
     */
    String getTransformEngineName();

    /**
     * @return messages to be logged on start up (license & settings). Use \n to split onto multiple lines.
     */
    String getStartupMessage();

    /**
     * @return a definition of what the t-engine supports. Normally read from a json Resource on the classpath using a
     * {@link TransformConfigResourceReader}. To combine to code from multiple t-engine into a single t-engine
     * include all the TransformEngines and CustomTransform implementations, plus a wrapper TransformEngine for the
     * others. The wrapper should return {@code null} from this method.
     */
    TransformConfig getTransformConfig();

    /**
     * @return a ProbeTransform (will do a quick transform) for k8 liveness and readiness probes.
     */
    ProbeTransform getProbeTransform();
}
