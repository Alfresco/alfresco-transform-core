package org.alfresco.transform.base.registry;

import org.alfresco.transform.config.TransformConfig;

public interface TransformConfigSource
{
    String getSortOnName();

    String getReadFrom();

    String getBaseUrl();

    TransformConfig getTransformConfig();
}
