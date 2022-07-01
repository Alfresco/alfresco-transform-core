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
package org.alfresco.transform.base;

import org.alfresco.transform.config.TransformConfig;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Interface to be implemented by transform specific code. The {@code transformerName} should match the transformerName
 * in the {@link TransformConfig} returned by the {@link TransformEngine}. So that it is automatically picked up, it
 * must exist in a package under {@code org.alfresco.transform} and have the Spring {@code @Component} annotation.
 */
public interface CustomTransformer
{
    String getTransformerName();

    void transform(String sourceMimetype, String sourceEncoding, InputStream inputStream,
                   String targetMimetype, String targetEncoding, OutputStream outputStream,
                   Map<String, String> transformOptions) throws Exception;
}
