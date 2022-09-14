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

import org.alfresco.transform.config.CoreFunction;

import java.util.Map;

/**
 * Used by clients work out if a transformation is supported by a Transform Service.
 */
public interface TransformServiceRegistry
{
    /**
     * Works out if the Transform Server should be able to transform content of a given source mimetype and size into a
     * target mimetype given a list of actual transform option names and values (Strings) plus the data contained in the
     * Transformer objects registered with this class.
     *
     * @param sourceMimetype    the mimetype of the source content
     * @param sourceSizeInBytes the size in bytes of the source content. Ignored if negative.
     * @param targetMimetype    the mimetype of the target
     * @param actualOptions     the actual name value pairs available that could be passed to the Transform Service.
     * @param transformName     (optional) name for the set of options and target mimetype. If supplied is used to cache
     *                          results to avoid having to work out if a given transformation is supported a second time.
     *                          The sourceMimetype and sourceSizeInBytes may still change. In the case of ACS this is the
     *                          rendition name.
     * @return {@code}true{@code} if it is supported.
     */
    default boolean isSupported(final String sourceMimetype, final long sourceSizeInBytes,
        final String targetMimetype, final Map<String, String> actualOptions,
        final String transformName)
    {
        long maxSize = findMaxSize(sourceMimetype, targetMimetype, actualOptions, transformName);
        return maxSize != 0 && (maxSize == -1L || maxSize >= sourceSizeInBytes);
    }

    /**
     * Returns the maximun size (in bytes) of the source content that can be transformed.
     *
     * @param sourceMimetype the mimetype of the source content
     * @param targetMimetype the mimetype of the target
     * @param actualOptions  the actual name value pairs available that could be passed to the Transform Service.
     * @param transformName  (optional) name for the set of options and target mimetype. If supplied is used to cache
     *                       results to avoid having to work out if a given transformation is supported a second time.
     *                       The sourceMimetype and sourceSizeInBytes may still change. In the case of ACS this is the
     *                       rendition name.
     * @return the maximum size (in bytes) of the source content that can be transformed. If {@code -1} there is no
     * limit, but if {@code 0} the transform is not supported.
     */
    long findMaxSize(String sourceMimetype, String targetMimetype, Map<String, String> actualOptions,
        String transformName);

    /**
     * Works out the name of the transformer (might not map to an actual transformer) that will be used to transform
     * content of a given source mimetype and size into a target mimetype given a list of actual transform option names
     * and values (Strings) plus the data contained in the Transformer objects registered with this class.
     *
     * @param sourceMimetype    the mimetype of the source content
     * @param sourceSizeInBytes the size in bytes of the source content. Ignored if negative.
     * @param targetMimetype    the mimetype of the target
     * @param actualOptions     the actual name value pairs available that could be passed to the Transform Service.
     * @param renditionName     (optional) name for the set of options and target mimetype. If supplied is used to cache
     *                          results to avoid having to work out if a given transformation is supported a second time.
     *                          The sourceMimetype and sourceSizeInBytes may still change. In the case of ACS this is the
     *                          rendition name.
     * @return the name of the transformer or {@code}null{@code} if not set or there is no supported transformer.
     */
    String findTransformerName(String sourceMimetype, long sourceSizeInBytes, String targetMimetype,
        Map<String, String> actualOptions, String renditionName);

    /**
     * Returns {@code true} if the {@code function} is supported by the named transformer. Not all transformers are
     * able to support all functionality, as newer features may have been introduced into the core t-engine code since
     * it was released. Normally used in conjunction with {@link #findTransformerName(String, long, String, Map, String)}
     * rather than {@link #isSupported(String, long, String, Map, String)}.
     * @param function to be checked.
     * @param transformerName name of the transformer.
     * @return {@code true} is supported, {@code false} otherwise.
     */
    default boolean isSupported(CoreFunction function, String transformerName)
    {
        return false;
    }
}
