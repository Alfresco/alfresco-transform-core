/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.transform.config.TransformOption;
import org.alfresco.transform.config.Transformer;

/**
 * Helper class for testing an {@link AbstractTransformRegistry}.
 */
public class FakeTransformRegistry extends AbstractTransformRegistry
{
    private static final String READ_FROM_A = "readFromA";
    private static final String BASE_URL_B = "baseUrlB";

    private final TransformCache data = new TransformCache();

    List<String> errorMessages = new ArrayList<>();
    List<String> warnMessages = new ArrayList<>();
    int registeredCount = 0;
    int readFromACount = 0;
    int baseUrlBCount = 0;
    Map<Transformer, String> transformerBaseUrls = new HashMap<>();

    @Override
    protected void logError(String msg)
    {
        System.out.println(msg);
        errorMessages.add(msg);
    }

    @Override
    protected void logWarn(String msg)
    {
        System.out.println(msg);
        warnMessages.add(msg);
    }

    @Override
    public TransformCache getData()
    {
        return data;
    }

    @Override
    protected void register(final Transformer transformer,
            final Map<String, Set<TransformOption>> transformOptions, final String baseUrl,
            final String readFrom)
    {
        super.register(transformer, transformOptions, baseUrl, readFrom);

        registeredCount++;
        if (READ_FROM_A.equals(readFrom))
        {
            readFromACount++;
        }
        if (BASE_URL_B.equals(baseUrl))
        {
            baseUrlBCount++;
        }
        transformerBaseUrls.put(transformer, baseUrl);
    }
}
