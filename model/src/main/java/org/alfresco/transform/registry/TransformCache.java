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

import static java.util.Collections.emptyMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransformCache
{
    // Looks up supported transform routes given source to target media types.
    private final Map<String, Map<String, List<SupportedTransform>>> transforms = new ConcurrentHashMap<>();

    // Looks up coreVersion given the transformer name
    private final Map<String, String> coreVersions = new ConcurrentHashMap<>();

    // Caches results in the ACS repository implementations which repeatedly make the same request.
    // Looks up a sorted list of transform routes, for a rendition (if the name is supplied) and the source
    // media type. Unlike a lookup on the transforms map above, processing of the transform options and priorities
    // will have already been done if cached.
    private final Map<String, Map<String, List<SupportedTransform>>> cachedSupportedTransformList = new ConcurrentHashMap<>();

    protected int transformerCount = 0;
    protected int transformCount = 0;

    public void incrementTransformerCount()
    {
        transformerCount++;
    }

    public void appendTransform(final String sourceMimetype,
            final String targetMimetype, final SupportedTransform transform,
            String transformerName, String coreVersion)
    {
        transforms
                .computeIfAbsent(sourceMimetype, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(targetMimetype, k -> new ArrayList<>())
                .add(transform);
        coreVersions.put(transformerName, coreVersion == null ? "" : coreVersion);
        transformCount++;
    }

    public Map<String, List<SupportedTransform>> retrieveTransforms(final String sourceMimetype)
    {
        return transforms.getOrDefault(sourceMimetype, emptyMap());
    }

    public Map<String, Map<String, List<SupportedTransform>>> getTransforms()
    {
        return transforms;
    }

    public void cache(final String renditionName, final String sourceMimetype,
            final List<SupportedTransform> transformListBySize)
    {
        cachedSupportedTransformList
                .get(renditionName)
                .put(sourceMimetype, transformListBySize);
    }

    public List<SupportedTransform> retrieveCached(final String renditionName,
            final String sourceMimetype)
    {
        return cachedSupportedTransformList
                .computeIfAbsent(renditionName, k -> new ConcurrentHashMap<>())
                .get(sourceMimetype);
    }

    @Override
    public String toString()
    {
        return transformerCount == 0 && transformCount == 0
                ? ""
                : "(transformers: " + transformerCount + " transforms: " + transformCount + ")";
    }

    public String getCoreVersion(String transformerName)
    {
        String coreVersion = coreVersions.get(transformerName);
        return coreVersion.isBlank() ? null : coreVersion;
    }
}
