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
package org.alfresco.transform.misc;

import com.google.common.collect.ImmutableMap;
import org.alfresco.transform.base.TransformEngine;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.common.TransformConfigResourceReader;
import org.alfresco.transform.config.TransformConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.alfresco.transform.base.logging.StandardMessages.COMMUNITY_LICENCE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_ENCODING;

@Component
public class MiscTransformEngine implements TransformEngine
{
    private final Map<String, String> transformOptions = ImmutableMap.of(SOURCE_ENCODING, "UTF-8");

    @Autowired
    private TransformConfigResourceReader transformConfigResourceReader;

    @Override
    public String getTransformEngineName()
    {
        return "0050-Misc";
    }

    @Override
    public String getStartupMessage()
    {
        return COMMUNITY_LICENCE +
                "This transformer uses libraries from Apache. " +
                "See the license at http://www.apache.org/licenses/LICENSE-2.0. or in /Apache\\\\ 2.0.txt\n" +
                "This transformer uses htmlparser. See the license at http://htmlparser.sourceforge.net/license.html";
    }

    @Override
    public TransformConfig getTransformConfig()
    {
        return transformConfigResourceReader.read("classpath:misc_engine_config.json");
    }

    @Override
    public ProbeTransform getProbeTransform()
    {
        return new ProbeTransform("quick.html", MIMETYPE_HTML, MIMETYPE_TEXT_PLAIN, transformOptions,
                119, 30, 150, 1024, 60 * 2 + 1, 60 * 2);
    }
}
