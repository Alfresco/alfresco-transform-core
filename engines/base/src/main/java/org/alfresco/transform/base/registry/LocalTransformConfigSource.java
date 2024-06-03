package org.alfresco.transform.base.registry;

import org.alfresco.transform.config.TransformConfig;

public class LocalTransformConfigSource extends AbstractTransformConfigSource
{
    private final TransformConfig transformConfig;

    protected LocalTransformConfigSource(TransformConfig transformConfig, String sortOnName, String readFrom, String baseUrl)
    {
        super(sortOnName, readFrom, baseUrl);
        this.transformConfig = transformConfig;
    }

    @Override
    public TransformConfig getTransformConfig()
    {
        return transformConfig;
    }
}
