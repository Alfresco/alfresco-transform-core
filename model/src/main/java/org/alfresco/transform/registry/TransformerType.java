/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2015 - 2022 Alfresco Software Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package org.alfresco.transform.registry;

import org.alfresco.transform.config.Transformer;

public enum TransformerType
{
    ENGINE_TRANSFORMER, PIPELINE_TRANSFORMER, FAILOVER_TRANSFORMER, UNSUPPORTED_TRANSFORMER;

    public static TransformerType valueOf(Transformer transformer)
    {
        if (transformer == null)
        {
            return null;
        }
        if ((transformer.getTransformerFailover() == null || transformer.getTransformerFailover().isEmpty()) &&
                (transformer.getTransformerPipeline() == null || transformer.getTransformerPipeline().isEmpty()))
        {
            return ENGINE_TRANSFORMER;
        }
        if (transformer.getTransformerPipeline() != null && !transformer.getTransformerPipeline().isEmpty())
        {
            return PIPELINE_TRANSFORMER;
        }
        if (transformer.getTransformerFailover() != null && !transformer.getTransformerFailover().isEmpty())
        {
            return FAILOVER_TRANSFORMER;
        }

        return UNSUPPORTED_TRANSFORMER;
    }
}
