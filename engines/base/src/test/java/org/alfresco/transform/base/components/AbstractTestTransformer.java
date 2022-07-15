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
package org.alfresco.transform.base.components;

import org.alfresco.transform.base.CustomTransformer;
import org.alfresco.transform.base.TransformManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Subclass MUST be named TestTransformer\<something>. Appends the name of the CustomTransformer and any t-options
 * to the output. The output is always a String regardless of the stated mimetypes.
 */
@TestComponent
public abstract class AbstractTestTransformer implements CustomTransformer
{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public String getTransformerName()
    {
        String simpleClassName = getClass().getSimpleName();
        return simpleClassName.substring("TestTransformer".length());
    }

    @Override
    public void transform(String sourceMimetype, InputStream inputStream, String targetMimetype,
            OutputStream outputStream, Map<String, String> transformOptions, TransformManager transformManager)
            throws Exception
    {
        String oldValue = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        String newValue = new StringBuilder(oldValue)
                .append(" -> ")
                .append(getTransformerName())
                .append("(")
                .append(transformOptions.entrySet()
                                        .stream()
                                        .map(e -> e.getKey() + '=' + e.getValue())
                                        .collect(Collectors.joining(", ")))
                .append(')')
                .toString();
        logger.info(newValue);
        byte[] bytes = newValue.getBytes(StandardCharsets.UTF_8);
        outputStream.write(bytes, 0, bytes.length);
    }
}
