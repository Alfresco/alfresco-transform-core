/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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
package org.alfresco.transformer;

import org.alfresco.transform.client.model.config.TransformConfig;
import org.alfresco.transform.client.registry.TransformServiceRegistry;
import org.alfresco.transformer.transformers.AllInOneTransformer;
import org.alfresco.transformer.transformers.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AIOCustomConfig
{

    @Bean("AllInOneTransformer")
    public Transformer aioTransformer()
    {
        return new AllInOneTransformer();
    }

    /**
     *
     * @return Override the TransformRegistryImpl used in {@link AbstractTransformerController}
     */
    @Bean
    @Primary
    public TransformServiceRegistry transformRegistryOverride()
    {
        return new TransformRegistryImpl()
        {

            @Autowired
            @Qualifier("AllInOneTransformer")
            Transformer transformer;

            @Override
            TransformConfig getTransformConfig()
            {
                return transformer.getTransformConfig();
            }
        };
    }
}
