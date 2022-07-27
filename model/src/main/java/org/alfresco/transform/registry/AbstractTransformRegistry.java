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
import org.alfresco.transform.config.TransformOption;
import org.alfresco.transform.config.Transformer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.alfresco.transform.registry.TransformRegistryHelper.lookupTransformOptions;
import static org.alfresco.transform.registry.TransformRegistryHelper.retrieveTransformListBySize;

/**
 * Used to work out if a transformation is supported. Sub classes should implement {@link #getData()} to return an
 * instance of the {@link TransformCache} class. This allows sub classes to periodically replace the registry's data with newer
 * values. They may also extend the Data class to include extra fields and methods.
 */
public abstract class AbstractTransformRegistry implements TransformServiceRegistry
{
    /**
     * Logs an error message if there is an error in the configuration.
     *
     * @param msg to be logged.
     */
    protected abstract void logError(String msg);

    /**
     * Logs a warning message if there is a problem in the configuration.
     *
     * @param msg to be logged.
     */
    protected void logWarn(String msg)
    {
        logError(msg);
    }

    /**
     * Returns the data held by the registry. Sub classes may extend the base Data and replace it at run time.
     *
     * @return the Data object that contains the registry's data.
     */
    public abstract TransformCache getData();

    /**
     * Registers a single transformer. This is an internal method called by
     * {@link CombinedTransformConfig#registerCombinedTransformers(AbstractTransformRegistry)}.
     *
     * @param transformer      to be registered
     * @param transformOptions all the transform options
     * @param baseUrl          where the config was be read from. Only needed when it is remote. Is null when local.
     *                         Does not need to be a URL. May just be a name.
     * @param readFrom         debug message for log messages, indicating what type of config was read.
     */
    protected void register(final Transformer transformer,
        final Map<String, Set<TransformOption>> transformOptions, final String baseUrl,
        final String readFrom)
    {
        getData().incrementTransformerCount();
        transformer
            .getSupportedSourceAndTargetList()
            .forEach(e -> getData().appendTransform(e.getSourceMediaType(), e.getTargetMediaType(),
                new SupportedTransform(
                    transformer.getTransformerName(),
                    lookupTransformOptions(transformer.getTransformOptions(), transformOptions,
                        readFrom, this::logError),
                    e.getMaxSourceSizeBytes(),
                    e.getPriority()),
                    transformer.getTransformerName(),
                    transformer.getCoreVersion()));
    }

    /**
     * Works out the name of the transformer (might not map to an actual transformer) that will be used to transform
     * content of a given source mimetype and size into a target mimetype given a list of actual transform option names
     * and values (Strings) plus the data contained in the Transform objects registered with this class.
     *
     * @param sourceMimetype    the mimetype of the source content
     * @param sourceSizeInBytes the size in bytes of the source content. Ignored if negative.
     * @param targetMimetype    the mimetype of the target
     * @param actualOptions     the actual name value pairs available that could be passed to the Transform Service.
     * @param renditionName     (optional) name for the set of options and target mimetype. If supplied is used to cache
     *                          results to avoid having to work out if a given transformation is supported a second time.
     *                          The sourceMimetype and sourceSizeInBytes may still change. In the case of ACS this is the
     *                          rendition name.
     */
    @Override
    public String findTransformerName(final String sourceMimetype, final long sourceSizeInBytes,
        final String targetMimetype, final Map<String, String> actualOptions,
        final String renditionName)
    {
        return retrieveTransformListBySize(getData(), sourceMimetype, targetMimetype, actualOptions,
            renditionName)
            .stream()
            .filter(t -> t.getMaxSourceSizeBytes() == -1 ||
                         t.getMaxSourceSizeBytes() >= sourceSizeInBytes)
            .findFirst()
            .map(SupportedTransform::getName)
            .orElse(null);
    }

    @Override
    public long findMaxSize(final String sourceMimetype, final String targetMimetype,
        final Map<String, String> actualOptions, final String renditionName)
    {
        final List<SupportedTransform> supportedTransforms = retrieveTransformListBySize(getData(),
            sourceMimetype, targetMimetype, actualOptions, renditionName);
        return supportedTransforms.isEmpty() ? 0 :
               supportedTransforms.get(supportedTransforms.size() - 1).getMaxSourceSizeBytes();
    }

    @Override
    public boolean isSupported(CoreFunction function, String transformerName)
    {
        return function.isSupported(getData().getCoreVersion(transformerName));
    }

    // When testing, we need to be able to set the baseUrl when reading from a file.
    public String getBaseUrlIfTesting(String name, String baseUrl)
    {
        return baseUrl;
    }
}
