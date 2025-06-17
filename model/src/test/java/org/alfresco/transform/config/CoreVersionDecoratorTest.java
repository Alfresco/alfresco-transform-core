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

import static org.junit.jupiter.api.Assertions.*;

import static org.alfresco.transform.common.RequestParamMap.*;
import static org.alfresco.transform.config.CoreFunction.standardizeCoreVersion;
import static org.alfresco.transform.config.CoreVersionDecorator.CONFIG_VERSION_INCLUDES_CORE_VERSION;
import static org.alfresco.transform.config.CoreVersionDecorator.setCoreVersionOnMultiStepTransformers;
import static org.alfresco.transform.config.CoreVersionDecorator.setCoreVersionOnSingleStepTransformers;
import static org.alfresco.transform.config.CoreVersionDecorator.setOrClearCoreVersion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

class CoreVersionDecoratorTest
{
    private static final int CONFIG_VERSION_ORIGINAL = Integer.valueOf(CONFIG_VERSION_DEFAULT);

    private static final String SOME_NAME = "optionName";

    public static final Set<TransformOption> SOME_OPTIONS = Set.of(new TransformOptionValue(false, "someOption"));
    private static final Set<TransformOption> DIRECT_ACCESS_URL_OPTION = Set.of(new TransformOptionValue(false, DIRECT_ACCESS_URL));
    private static final Set<TransformOption> SOURCE_FILENAME_OPTION = Set.of(new TransformOptionValue(false, SOURCE_FILENAME));

    private final Map<String, Set<TransformOption>> TRANSFORM_OPTIONS_WITHOUT_DIRECT_ACCESS_URL = new HashMap<>();
    private final Map<String, Set<TransformOption>> TRANSFORM_OPTIONS_WITH_DIRECT_ACCESS_URL = new HashMap<>();

    private final Map<String, Set<TransformOption>> TRANSFORM_OPTIONS_WITHOUT_SOURCE_FILENAME = new HashMap<>();
    private final Map<String, Set<TransformOption>> TRANSFORM_OPTIONS_WITH_SOURCE_FILENAME = new HashMap<>();
    {
        TRANSFORM_OPTIONS_WITHOUT_DIRECT_ACCESS_URL.put(SOME_NAME, SOME_OPTIONS);
        TRANSFORM_OPTIONS_WITH_DIRECT_ACCESS_URL.put(SOME_NAME, SOME_OPTIONS);
        TRANSFORM_OPTIONS_WITH_DIRECT_ACCESS_URL.put(DIRECT_ACCESS_URL, DIRECT_ACCESS_URL_OPTION);

        TRANSFORM_OPTIONS_WITHOUT_SOURCE_FILENAME.put(SOME_NAME, SOME_OPTIONS);
        TRANSFORM_OPTIONS_WITHOUT_SOURCE_FILENAME.put(DIRECT_ACCESS_URL, DIRECT_ACCESS_URL_OPTION);
        TRANSFORM_OPTIONS_WITH_SOURCE_FILENAME.put(SOME_NAME, SOME_OPTIONS);
        TRANSFORM_OPTIONS_WITH_SOURCE_FILENAME.put(DIRECT_ACCESS_URL, DIRECT_ACCESS_URL_OPTION);
        TRANSFORM_OPTIONS_WITH_SOURCE_FILENAME.put(SOURCE_FILENAME, SOURCE_FILENAME_OPTION);
    }

    private TransformConfig newTransformConfig(String version1, String version2, String version3, String version4, String version5,
            boolean hasDirectAccessUrls, boolean multiStepHaveDirectAccessUrls)
    {
        HashSet<String> transformOptions1 = new HashSet<>();
        HashSet<String> transformOptions2 = new HashSet<>(transformOptions1);
        transformOptions2.add(SOME_NAME);
        HashSet<String> transformOptions3 = new HashSet<>(transformOptions1);

        HashSet<String> transformOptions4 = new HashSet<>(transformOptions1);
        transformOptions4.addAll(transformOptions2);
        HashSet<String> transformOptions5 = new HashSet<>(transformOptions1);
        transformOptions5.addAll(transformOptions2);
        transformOptions5.addAll(transformOptions3);

        if (hasDirectAccessUrls)
        {
            transformOptions1.add(DIRECT_ACCESS_URL);
            transformOptions2.add(DIRECT_ACCESS_URL);
            transformOptions3.add(DIRECT_ACCESS_URL);
        }

        if (multiStepHaveDirectAccessUrls)
        {
            transformOptions4.add(DIRECT_ACCESS_URL);
            transformOptions5.add(DIRECT_ACCESS_URL);
        }

        return TransformConfig.builder()
                .withTransformOptions(hasDirectAccessUrls ? TRANSFORM_OPTIONS_WITH_DIRECT_ACCESS_URL : TRANSFORM_OPTIONS_WITHOUT_DIRECT_ACCESS_URL)
                .withTransformers(ImmutableList.of(
                        Transformer.builder()
                                .withTransformerName("transformer1")
                                .withCoreVersion(version1)
                                .withTransformOptions(transformOptions1)
                                .build(),
                        Transformer.builder()
                                .withTransformerName("transformer2")
                                .withCoreVersion(version2)
                                .withTransformOptions(transformOptions2)
                                .build(),
                        Transformer.builder()
                                .withTransformerName("transformer3")
                                .withCoreVersion(version3)
                                .withTransformOptions(transformOptions3)
                                .build(),
                        Transformer.builder()
                                .withTransformerName("pipeline4")
                                .withCoreVersion(version4)
                                .withTransformerPipeline(List.of(
                                        new TransformStep("transformer1", "mimetype/c"),
                                        new TransformStep("transformer2", null)))
                                .withTransformOptions(transformOptions4)
                                .build(),
                        Transformer.builder()
                                .withTransformerName("failover5")
                                .withCoreVersion(version5)
                                .withTransformerFailover(List.of("transformer1", "transformer2", "transformer3"))
                                .withTransformOptions(transformOptions5)
                                .build()))
                .build();
    }

    @Test
    void setCoreVersionOnSingleStepTransformersTest()
    {
        TransformConfig transformConfigReadFormTEngineJson = newTransformConfig(
                null, null, null, null, null,
                false, false);

        setCoreVersionOnSingleStepTransformers(transformConfigReadFormTEngineJson, "2.3.1");
        assertEquals(newTransformConfig("2.3.1", "2.3.1", "2.3.1", null, null,
                false, false), transformConfigReadFormTEngineJson);

        // Now with Direct Access URLs
        transformConfigReadFormTEngineJson = newTransformConfig(
                null, null, null, null, null,
                false, false);

        setCoreVersionOnSingleStepTransformers(transformConfigReadFormTEngineJson, "2.5.7");
        assertEquals(newTransformConfig("2.5.7", "2.5.7", "2.5.7", null, null,
                true, false), transformConfigReadFormTEngineJson);
    }

    @Test
    void setCoreVersionOnMultiStepTransformersTest()
    {
        // All source T-Engines provide a coreVersion and have had the coreVersion added
        TransformConfig decoratedSingleStepTransformConfig = newTransformConfig(
                "2.1", "2.2", "1.2.3", null, null,
                false, false);

        setCoreVersionOnMultiStepTransformers(decoratedSingleStepTransformConfig.getTransformOptions(),
                decoratedSingleStepTransformConfig.getTransformers());
        assertEquals(newTransformConfig("2.1", "2.2", "1.2.3", "2.1", "1.2.3",
                false, false), decoratedSingleStepTransformConfig);

        // Some source T-Engines are pre coreVersion
        decoratedSingleStepTransformConfig = newTransformConfig(
                "2.1", null, null, null, null,
                false, false);

        setCoreVersionOnMultiStepTransformers(decoratedSingleStepTransformConfig.getTransformOptions(),
                decoratedSingleStepTransformConfig.getTransformers());
        assertEquals(newTransformConfig("2.1", null, null, null, null,
                false, false), decoratedSingleStepTransformConfig);

        // Now with Direct Access URLs
        decoratedSingleStepTransformConfig = newTransformConfig("2.5.7", "2.5.7", "2.5.7", null, null,
                true, false);

        setCoreVersionOnMultiStepTransformers(decoratedSingleStepTransformConfig.getTransformOptions(),
                decoratedSingleStepTransformConfig.getTransformers());
        assertEquals(newTransformConfig("2.5.7", "2.5.7", "2.5.7", "2.5.7", "2.5.7",
                true, true), decoratedSingleStepTransformConfig);
    }

    @Test
    void setOrClearCoreVersionTest()
    {
        // All source T-Engines provide a coreVersion
        TransformConfig transformConfigWithCoreVersion = newTransformConfig(
                "2.1", "2.2", "1.2.3", "2.1", "1.2.3",
                false, false);

        assertEquals(newTransformConfig(null, null, null, null, null,
                false, false),
                setOrClearCoreVersion(transformConfigWithCoreVersion, CONFIG_VERSION_ORIGINAL));
        assertEquals(newTransformConfig("2.1", "2.2", "1.2.3", "2.1", "1.2.3",
                false, false),
                setOrClearCoreVersion(transformConfigWithCoreVersion, CONFIG_VERSION_INCLUDES_CORE_VERSION));
        assertEquals(newTransformConfig("2.1", "2.2", "1.2.3", "2.1", "1.2.3",
                false, false),
                setOrClearCoreVersion(transformConfigWithCoreVersion, CONFIG_VERSION_INCLUDES_CORE_VERSION + 100));

        // Some source T-Engines are pre coreVersion
        TransformConfig transformConfigWithoutCoreVersion = newTransformConfig(
                null, null, null, null, null,
                false, false);

        assertEquals(newTransformConfig(null, null, null, null, null,
                false, false),
                setOrClearCoreVersion(transformConfigWithoutCoreVersion, CONFIG_VERSION_ORIGINAL));
        assertEquals(newTransformConfig(null, null, null, null, null,
                false, false),
                setOrClearCoreVersion(transformConfigWithoutCoreVersion, CONFIG_VERSION_INCLUDES_CORE_VERSION));

        // Now with Direct Access URLs
        transformConfigWithCoreVersion = newTransformConfig(
                "2.5.7", "2.5.7", "2.5.7", "2.5.7", "2.5.7",
                true, true);

        assertEquals(newTransformConfig(null, null, null, null, null,
                false, false),
                setOrClearCoreVersion(transformConfigWithCoreVersion, CONFIG_VERSION_ORIGINAL));
        assertEquals(newTransformConfig("2.5.7", "2.5.7", "2.5.7", "2.5.7", "2.5.7",
                true, true),
                setOrClearCoreVersion(transformConfigWithCoreVersion, CONFIG_VERSION_INCLUDES_CORE_VERSION));
    }

    @Test
    void standardizeCoreVersionTest()
    {
        assertEquals("2.5.7", standardizeCoreVersion("2.5.7"));
        assertEquals("2.5.7", standardizeCoreVersion("2.5.7-SNAPSHOT"));
        assertEquals("2", standardizeCoreVersion("2"));
        assertEquals("2.5.7", standardizeCoreVersion("2.5.7-A-SNAPSHOT"));
    }

    private TransformConfig newTransformConfigForSourceFileName(String version1, String version2, String version3, String version4, String version5,
            boolean hasSourceFilename, boolean multiStepHaveSourceFilename)
    {
        HashSet<String> transformOptions1 = new HashSet<>();
        HashSet<String> transformOptions2 = new HashSet<>(transformOptions1);
        transformOptions2.add(SOME_NAME);
        HashSet<String> transformOptions3 = new HashSet<>(transformOptions1);

        HashSet<String> transformOptions4 = new HashSet<>(transformOptions1);
        transformOptions4.addAll(transformOptions2);
        HashSet<String> transformOptions5 = new HashSet<>(transformOptions1);
        transformOptions5.addAll(transformOptions2);
        transformOptions5.addAll(transformOptions3);

        transformOptions1.add(DIRECT_ACCESS_URL);
        transformOptions2.add(DIRECT_ACCESS_URL);
        transformOptions3.add(DIRECT_ACCESS_URL);
        transformOptions4.add(DIRECT_ACCESS_URL);
        transformOptions5.add(DIRECT_ACCESS_URL);

        if (hasSourceFilename)
        {
            transformOptions1.add(SOURCE_FILENAME);
            transformOptions2.add(SOURCE_FILENAME);
            transformOptions3.add(SOURCE_FILENAME);
        }

        if (multiStepHaveSourceFilename)
        {
            transformOptions4.add(SOURCE_FILENAME);
            transformOptions5.add(SOURCE_FILENAME);
        }

        return TransformConfig.builder()
                .withTransformOptions(hasSourceFilename ? TRANSFORM_OPTIONS_WITH_SOURCE_FILENAME : TRANSFORM_OPTIONS_WITHOUT_SOURCE_FILENAME)
                .withTransformers(ImmutableList.of(
                        Transformer.builder()
                                .withTransformerName("transformer1")
                                .withCoreVersion(version1)
                                .withTransformOptions(transformOptions1)
                                .build(),
                        Transformer.builder()
                                .withTransformerName("transformer2")
                                .withCoreVersion(version2)
                                .withTransformOptions(transformOptions2)
                                .build(),
                        Transformer.builder()
                                .withTransformerName("transformer3")
                                .withCoreVersion(version3)
                                .withTransformOptions(transformOptions3)
                                .build(),
                        Transformer.builder()
                                .withTransformerName("pipeline4")
                                .withCoreVersion(version4)
                                .withTransformerPipeline(List.of(
                                        new TransformStep("transformer1", "mimetype/c"),
                                        new TransformStep("transformer2", null)))
                                .withTransformOptions(transformOptions4)
                                .build(),
                        Transformer.builder()
                                .withTransformerName("failover5")
                                .withCoreVersion(version5)
                                .withTransformerFailover(List.of("transformer1", "transformer2", "transformer3"))
                                .withTransformOptions(transformOptions5)
                                .build()))
                .build();
    }

    @Test
    void setCoreVersionWithSourceFileNameOptionTest()
    {

        String sourceFileName = SOURCE_FILENAME;

        // Create transform config with no SOURCE_FILENAME option
        TransformConfig transformConfig = newTransformConfigForSourceFileName(
                "5.1.9", "5.1.9", "5.1.9",
                "5.1.9", "5.1.9",
                false, false);

        // Add SOURCE_FILENAME to all single step transformers
        setCoreVersionOnSingleStepTransformers(transformConfig, "5.1.9");

        // Check that SOURCE_FILENAME is present in all single step transformers' options

        assertEquals(newTransformConfigForSourceFileName("4.0.0", "4.0.0", "4.0.0", "4.0.0", "4.0.0",
                false, false),
                setOrClearCoreVersion(
                        newTransformConfigForSourceFileName("4.0.0", "4.0.0", "4.0.0", "4.0.0", "4.0.0",
                                false, false),
                        CONFIG_VERSION_INCLUDES_CORE_VERSION));

        // Supported version: SOURCE_FILENAME should be present
        assertEquals(newTransformConfigForSourceFileName("5.1.9", "5.1.9", "5.1.9", "5.1.9", "5.1.9",
                true, true),
                setOrClearCoreVersion(
                        newTransformConfigForSourceFileName("5.1.9", "5.1.9", "5.1.9", "5.1.9", "5.1.9", true, true),
                        CONFIG_VERSION_INCLUDES_CORE_VERSION));
    }

}
