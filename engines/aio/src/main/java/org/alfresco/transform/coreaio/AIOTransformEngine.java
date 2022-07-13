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
package org.alfresco.transform.coreaio;

import org.alfresco.transform.base.TransformEngine;
import org.alfresco.transform.base.probes.ProbeTestTransform;
import org.alfresco.transform.config.TransformConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.alfresco.transform.base.logging.StandardMessages.COMMUNITY_LICENCE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;

@Component
public class AIOTransformEngine implements TransformEngine
{
//    private static String SPLIT_UP_COMMUNITY_LICENCE = Arrays.stream(COMMUNITY_LICENCE.split("\\n"));
    @Autowired(required = false)
    private List<TransformEngine> transformEngines;

    @Override
    public String getTransformEngineName()
    {
        return "0060-AllInOne";
    }

    @Override
    public String getStartupMessage()
    {
        String message = "";
        if (transformEngines != null)
        {
            // Combines the messages of the component TransformEngines. Removes duplicate community license messages.
            message = transformEngines.stream()
                    .filter(transformEngine -> transformEngine != this)
                    .map(transformEngine -> transformEngine.getStartupMessage())
                    .collect( Collectors.joining("\n"));
            message = message.replace(COMMUNITY_LICENCE, "");
        }
        return COMMUNITY_LICENCE + message;
    }

    @Override
    public TransformConfig getTransformConfig()
    {
        return null;
    }

    @Override
    public ProbeTestTransform getLivenessAndReadinessProbeTestTransform()
    {
        return new ProbeTestTransform("quick.pdf", "quick.txt",
                MIMETYPE_PDF, MIMETYPE_TEXT_PLAIN, Collections.emptyMap(),
                60, 16, 400, 10240, 60 * 30 + 1, 60 * 15 + 20);
    }
}
