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
package org.alfresco.transform.example;

import com.google.common.collect.ImmutableMap;
import org.alfresco.transform.base.TransformEngine;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.common.TransformConfigResourceReader;
import org.alfresco.transform.config.TransformConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HelloTransformEngine implements TransformEngine
{
    @Autowired
    private TransformConfigResourceReader transformConfigResourceReader;

    @Override
    public String getTransformEngineName()
    {
        return "0200_hello";
    }

    @Override
    public String getStartupMessage()
    {
        return "Startup "+getTransformEngineName()+"\nNo 3rd party licenses";
    }

    @Override
    public TransformConfig getTransformConfig()
    {
        return transformConfigResourceReader.read("classpath:hello_engine_config.json");
    }

    @Override
    public ProbeTransform getProbeTransform()
    {
        return new ProbeTransform("jane.txt", "text/plain", "text/plain",
            ImmutableMap.of("sourceEncoding", "UTF-8", "language", "English"),
            11, 10, 150, 1024, 1, 60 * 2);
    }
}