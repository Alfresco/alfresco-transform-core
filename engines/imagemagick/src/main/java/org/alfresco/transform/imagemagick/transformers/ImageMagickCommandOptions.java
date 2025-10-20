/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2025 Alfresco Software Limited
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
package org.alfresco.transform.imagemagick.transformers;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageMagickCommandOptions
{
    private static final Logger LOG = LoggerFactory.getLogger(ImageMagickCommandOptions.class);

    @Value("${transform.core.imagemagick.commandOptions.enabled}")
    private boolean commandOptionsEnabled;

    @PostConstruct
    void init()
    {
        if (commandOptionsEnabled)
        {
            LOG.warn("Parameter commandOptions is enabled. It is deprecated and due to security concerns, it will be removed in the future.");
        }
        else
        {
            LOG.debug("Parameter commandOptions is disabled.");
        }
    }

    public boolean isCommandOptionsEnabled()
    {
        return commandOptionsEnabled;
    }
}
