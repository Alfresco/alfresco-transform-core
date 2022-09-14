/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.example;

import org.alfresco.transform.base.CustomTransformer;
import org.alfresco.transform.base.TransformManager;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

@Component
public class HelloTransformer implements CustomTransformer
{
    @Override
    public String getTransformerName()
    {
        return "hello";
    }

    @Override
    public void transform(String sourceMimetype, InputStream inputStream, String targetMimetype,
            OutputStream outputStream, Map<String, String> transformOptions, TransformManager transformManager)
            throws Exception
    {
        String name = new String(inputStream.readAllBytes(), transformOptions.get("sourceEncoding"));
        String greeting = String.format(getGreeting(transformOptions.get("language")), name);
        byte[] bytes = greeting.getBytes(transformOptions.get("sourceEncoding"));
        outputStream.write(bytes, 0, bytes.length);
    }

    private String getGreeting(String language)
    {
        return "Hello %s";
    }
}