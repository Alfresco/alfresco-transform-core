/*
 *  Copyright 2015-2022 Alfresco Software, Ltd.  All rights reserved.
 *
 *  License rights for this program may be obtained from Alfresco Software, Ltd.
 *  pursuant to a written agreement and any use of this program without such an
 *  agreement is prohibited.
 */
package org.alfresco.transform.registry;

import org.alfresco.transform.config.Transformer;

public enum TransformerType
{
    ENGINE_TRANSFORMER,
    PIPELINE_TRANSFORMER,
    FAILOVER_TRANSFORMER,
    UNSUPPORTED_TRANSFORMER;

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