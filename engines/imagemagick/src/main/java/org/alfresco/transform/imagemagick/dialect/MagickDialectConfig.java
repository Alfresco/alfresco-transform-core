/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2026 Alfresco Software Limited
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
package org.alfresco.transform.imagemagick.dialect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MagickDialectConfig
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MagickDialectConfig.class);

    @Bean
    public MagickDialect magickDialect(@Value("${" + MagickBackend.PROPERTY + ":imagemagick}") String backend)
    {
        MagickDialect dialect = createDialect(MagickBackend.from(backend));
        LOGGER.info("{}={} selects the {} dialect", MagickBackend.PROPERTY, backend, dialect.displayName());
        return dialect;
    }

    static MagickDialect createDialect(MagickBackend backend)
    {
        return switch (backend)
        {
        case IMAGEMAGICK -> new ImageMagickDialect();
        case GRAPHICSMAGICK -> new GraphicsMagickDialect();
        };
    }
}
