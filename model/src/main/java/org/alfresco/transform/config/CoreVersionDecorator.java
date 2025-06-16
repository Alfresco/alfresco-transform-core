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
package org.alfresco.transform.config;

import static java.util.function.Predicate.not;

import static org.alfresco.transform.common.RequestParamMap.DIRECT_ACCESS_URL;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_FILENAME;
import static org.alfresco.transform.config.CoreFunction.Constants.NO_VERSION;
import static org.alfresco.transform.config.CoreFunction.newComparableVersion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * <p>
 * Class sets or clears the {@code coreVersion} property of {@link Transformer}s in a {@link TransformConfig}
 * <p/>
 *
 * <p>
 * Since alfresco-transform-core 5.2.7, the config returned by T-Engines and T-Router via their {@code "/transform/config"} endpoint has been decorated with an {@code coreVersion} element, indicating what core functionality is provided by each transformer as a result of extending the {@code TransformController} in the {@code alfresco-transform-base}. This is automatically added, so need not be specified by the T-Engine developer. It was originally added to indicate that it was possible to use Direct Access URLs (DAU).
 * </p>
 *
 * <p>
 * This class provides methods to sets or clear the field with the version number of the {@code alfresco-transform-base}. No value indicates 5.2.6 or earlier.
 * </p>
 *
 * <p>
 * To allow older and newer version of the Repository, T-Router and T-Engines to work together, this field is only returned if requested by a client that also knows about the field. An optional {@code "configVersion"} parameter has been added to the endpoint. The config for T-Engines need only add the field to single-step-transforms. When configs are combined it is then possible to add this field to pipeline and failover transforms by using the lowest core value of any step transform.
 * </p>
 *
 * <p>
 * If the field is not requested in the T-Router or the all-in-one transformer endpoint, it may need to be stripped from the {@link TransformConfig} as some of the T-Engines may have supplied it.
 * </p>
 *
 * @see CoreFunction
 */
public class CoreVersionDecorator
{
    public static final int CONFIG_VERSION_INCLUDES_CORE_VERSION = 2;

    private static final Set<TransformOption> DIRECT_ACCESS_URL_TRANSFORM_OPTIONS = Set.of(new TransformOptionValue(false, DIRECT_ACCESS_URL));

    private static final Set<TransformOption> SOURCE_FILENAME_TRANSFORM_OPTIONS = Set.of(new TransformOptionValue(false, SOURCE_FILENAME));

    /**
     * Returns a new {@link TransformConfig} that includes or excludes the {@code coreVersion} field and associated elements like directAccessUrl.
     */
    public static TransformConfig setOrClearCoreVersion(TransformConfig transformConfig, int configVersion)
    {
        boolean includeCoreVersion = configVersion >= 2;

        Map<String, Set<TransformOption>> transformOptions = new HashMap<>(transformConfig.getTransformOptions());
        transformConfig = TransformConfig.builder()
                // We may need to create new Transformers as we must not change the original.
                .withTransformers(transformConfig.getTransformers().stream()
                        .map(transformer -> {
                            if ((includeCoreVersion && transformer.getCoreVersion() == null) ||
                                    (!includeCoreVersion && transformer.getCoreVersion() != null))
                            {
                                transformer = Transformer.builder()
                                        .withCoreVersion(includeCoreVersion ? transformer.getCoreVersion() : null)
                                        .withTransformOptions(setOrClearCoreTransformOptions(
                                                includeCoreVersion ? transformer.getCoreVersion() : null,
                                                transformer.getTransformOptions()))
                                        // Original values
                                        .withTransformerName(transformer.getTransformerName())
                                        .withTransformerPipeline(transformer.getTransformerPipeline())
                                        .withTransformerFailover(transformer.getTransformerFailover())
                                        .withSupportedSourceAndTargetList(transformer.getSupportedSourceAndTargetList())
                                        .build();
                            }
                            return transformer;
                        })
                        .collect(Collectors.toList()))
                .withTransformOptions(transformOptions)
                // Original values
                .withRemoveTransformers(transformConfig.getRemoveTransformers())
                .withAddSupported(transformConfig.getAddSupported())
                .withRemoveSupported(transformConfig.getRemoveSupported())
                .withOverrideSupported(transformConfig.getOverrideSupported())
                .withSupportedDefaults(transformConfig.getSupportedDefaults())
                .build();
        addOrRemoveDirectAccessUrlOption(transformConfig.getTransformOptions(), transformConfig.getTransformers());
        addOrRemoveSourceFileNameOption(transformConfig.getTransformOptions(), transformConfig.getTransformers());
        return transformConfig;
    }

    public static void setCoreVersionOnSingleStepTransformers(TransformConfig transformConfig, String coreVersion)
    {
        List<Transformer> transformers = transformConfig.getTransformers();
        transformers.stream()
                .filter(CoreVersionDecorator::isSingleStep)
                .forEach(transformer -> {
                    transformer.setCoreVersion(coreVersion);
                    transformer.setTransformOptions(setOrClearCoreTransformOptions(coreVersion, transformer.getTransformOptions()));
                });
        addOrRemoveDirectAccessUrlOption(transformConfig.getTransformOptions(), transformers);
        addOrRemoveSourceFileNameOption(transformConfig.getTransformOptions(), transformers);
    }

    /**
     * The list of {@code transformers} must not contain forward references
     */
    public static void setCoreVersionOnMultiStepTransformers(Map<String, Set<TransformOption>> transformOptions,
            List<Transformer> transformers)
    {
        Map<String, Transformer> transformersByName = transformers.stream()
                .collect(Collectors.toMap(Transformer::getTransformerName, Function.identity()));

        transformers.stream()
                .filter(not(CoreVersionDecorator::isSingleStep))
                .forEach(transformer -> {

                    // Create a list of step transformers
                    List<String> namesOfStepTransformers = transformer.getTransformerFailover().isEmpty()
                            ? transformer.getTransformerPipeline().stream()
                                    .map(TransformStep::getTransformerName)
                                    .collect(Collectors.toList())
                            : transformer.getTransformerFailover();

                    // Set the coreVersion to the lowest step transformer value
                    ComparableVersion minCoreVersion = namesOfStepTransformers.stream()
                            .map(transformerName -> transformersByName.get(transformerName).getCoreVersion())
                            .map(coreVersion -> newComparableVersion(coreVersion, NO_VERSION))
                            .min(ComparableVersion::compareTo).orElse(NO_VERSION);
                    String coreVersion = NO_VERSION.equals(minCoreVersion) ? null : minCoreVersion.toString();
                    transformer.setCoreVersion(coreVersion);
                    transformer.setTransformOptions(setOrClearCoreTransformOptions(transformer.getCoreVersion(),
                            transformer.getTransformOptions()));
                });
        addOrRemoveDirectAccessUrlOption(transformOptions, transformers);
        addOrRemoveSourceFileNameOption(transformOptions, transformers);
    }

    private static Set<String> setOrClearCoreTransformOptions(String coreVersion, Set<String> transformerTransformOptions)
    {
        // If we have more options being added in the future, consider adding an interface that will be implemented by
        // different implementations for each coreVersion and then iterate over them here.
        transformerTransformOptions = new HashSet<>(transformerTransformOptions);
        if (CoreFunction.DIRECT_ACCESS_URL.isSupported(coreVersion))
        {
            // Add DIRECT_ACCESS_URL to a copy of this Transformer's transform options.
            transformerTransformOptions.add(DIRECT_ACCESS_URL);
        }
        else
        {
            transformerTransformOptions.remove(DIRECT_ACCESS_URL);
        }

        return transformerTransformOptions;
    }

    private static void addOrRemoveDirectAccessUrlOption(Map<String, Set<TransformOption>> transformOptions,
            List<Transformer> transformers)
    {
        if (transformers.stream()
                .anyMatch(transformer -> CoreFunction.DIRECT_ACCESS_URL.isSupported(transformer.getCoreVersion())))
        {
            transformOptions.put(DIRECT_ACCESS_URL, DIRECT_ACCESS_URL_TRANSFORM_OPTIONS);
        }
        else
        {
            transformOptions.remove(DIRECT_ACCESS_URL);
        }
    }

    private static void addOrRemoveSourceFileNameOption(Map<String, Set<TransformOption>> transformOptions,
            List<Transformer> transformers)
    {
        if (transformers.stream()
                .anyMatch(transformer -> CoreFunction.SOURCE_FILENAME.isSupported(transformer.getCoreVersion())))
        {
            transformOptions.put(SOURCE_FILENAME, SOURCE_FILENAME_TRANSFORM_OPTIONS);
        }
        else
        {
            transformOptions.remove(SOURCE_FILENAME);
        }
    }

    private static boolean isSingleStep(Transformer transformer)
    {
        return (transformer.getTransformerFailover() == null || transformer.getTransformerFailover().isEmpty()) &&
                (transformer.getTransformerPipeline() == null || transformer.getTransformerPipeline().isEmpty());
    }
}
