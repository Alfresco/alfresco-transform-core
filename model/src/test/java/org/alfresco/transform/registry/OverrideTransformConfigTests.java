/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2005 - 2026 Alfresco Software Limited
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import org.alfresco.transform.config.AddSupported;
import org.alfresco.transform.config.OverrideSupported;
import org.alfresco.transform.config.RemoveSupported;
import org.alfresco.transform.config.SupportedDefaults;
import org.alfresco.transform.config.SupportedSourceAndTarget;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.TransformStep;
import org.alfresco.transform.config.Transformer;

/**
 * Tests the json elements: {@code removeTransformers}, {@code addSupported}, {@code removeSupported}, {@code overrideSupported} and {@code supportedDefaults}.
 */
public class OverrideTransformConfigTests
{
    private static final String READ_FROM_A = "readFromA";
    private static final String READ_FROM_B = "readFromB";
    private static final String BASE_URL_A = "baseUrlA";
    private static final String BASE_URL_B = "baseUrlB";

    private final SupportedSourceAndTarget supported_A2B = SupportedSourceAndTarget.builder()
            .withSourceMediaType("mimetype/a")
            .withTargetMediaType("mimetype/b")
            .build();

    // Override result: priority overridden to 40; maxSourceSizeBytes not in override, retained as -1 (unlimited default from original)
    private final SupportedSourceAndTarget supported_A2B_default_40 = SupportedSourceAndTarget.builder()
            .withSourceMediaType("mimetype/a")
            .withTargetMediaType("mimetype/b")
            .withPriority(40)
            .withMaxSourceSizeBytes(-1L)
            .build();

    private final SupportedSourceAndTarget supported_C2D = SupportedSourceAndTarget.builder()
            .withSourceMediaType("mimetype/c")
            .withTargetMediaType("mimetype/d")
            .build();

    private final SupportedSourceAndTarget supported_A2D_1234_44 = SupportedSourceAndTarget.builder()
            .withSourceMediaType("mimetype/a")
            .withTargetMediaType("mimetype/d")
            .withMaxSourceSizeBytes(1234L)
            .withPriority(44)
            .build();

    private final SupportedSourceAndTarget supported_X2Y_100_23 = SupportedSourceAndTarget.builder()
            .withSourceMediaType("mimetype/x")
            .withTargetMediaType("mimetype/y")
            .withMaxSourceSizeBytes(100L)
            .withPriority(23)
            .build();

    // Override result: maxSourceSizeBytes overridden to 200; priority not in override, retained as 23 from original entry
    private final SupportedSourceAndTarget supported_X2Y_200_23 = SupportedSourceAndTarget.builder()
            .withSourceMediaType("mimetype/x")
            .withTargetMediaType("mimetype/y")
            .withMaxSourceSizeBytes(200L)
            .withPriority(23)
            .build();

    private final TransformConfig transformConfig_A2B_X2Y_100_23 = TransformConfig.builder()
            .withTransformers(ImmutableList.of(
                    Transformer.builder().withTransformerName("1")
                            .withSupportedSourceAndTargetList(new HashSet<>(Set.of(
                                    supported_A2B,
                                    supported_X2Y_100_23)))
                            .build()))
            .build();

    private final CombinedTransformConfig config = new CombinedTransformConfig();

    private final FakeTransformRegistry registry = new FakeTransformRegistry();

    @Test
    public void testRemoveTransformers()
    {
        final Transformer transformer1 = Transformer.builder().withTransformerName("1").build();
        final Transformer transformer2 = Transformer.builder().withTransformerName("2").build();
        final Transformer transformer3 = Transformer.builder().withTransformerName("3").build();
        final Transformer transformer4 = Transformer.builder().withTransformerName("4").build();

        final TransformConfig firstConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        transformer1,
                        transformer2,
                        transformer3,
                        transformer4))
                .build();
        final TransformConfig secondConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        transformer2)) // Puts transform 2 back again
                .withRemoveTransformers(ImmutableSet.of("2", "7", "3", "2", "5"))
                .build();

        config.addTransformConfig(firstConfig, READ_FROM_A, BASE_URL_A, registry);
        TransformConfig resultConfig = config.buildTransformConfig();
        assertEquals(4, resultConfig.getTransformers().size());

        config.addTransformConfig(secondConfig, READ_FROM_B, BASE_URL_B, registry);
        resultConfig = config.buildTransformConfig();
        assertEquals(3, resultConfig.getTransformers().size());

        String expected = "Unable to process \"removeTransformers\": [\"7\", \"5\"]. Read from readFromB";
        assertEquals(1, registry.warnMessages.size());
        assertEquals(expected, registry.warnMessages.get(0));
    }

    @Test
    public void testSupportedDefaultsSet()
    {
        SupportedDefaults default_1A_100 = SupportedDefaults.builder()
                .withTransformerName("1")
                .withSourceMediaType("mimetype/a")
                .withMaxSourceSizeBytes(100L)
                .build();

        SupportedDefaults default_1A_200 = SupportedDefaults.builder()
                .withTransformerName("1")
                .withSourceMediaType("mimetype/a")
                .withMaxSourceSizeBytes(200L)
                .build();

        SupportedDefaults default_2A__45 = SupportedDefaults.builder()
                .withTransformerName("2")
                .withSourceMediaType("mimetype/a")
                .withPriority(45)
                .build();

        SupportedDefaults default_3_400 = SupportedDefaults.builder()
                .withTransformerName("3")
                .withMaxSourceSizeBytes(400L)
                .build();

        SupportedDefaults default_B_400 = SupportedDefaults.builder()
                .withSourceMediaType("mimetype/b")
                .withMaxSourceSizeBytes(400L)
                .build();

        SupportedDefaults default_B_500 = SupportedDefaults.builder()
                .withSourceMediaType("mimetype/b")
                .withMaxSourceSizeBytes(500L)
                .build();

        SupportedDefaults default__600 = SupportedDefaults.builder()
                .withMaxSourceSizeBytes(600L)
                .build();

        SupportedDefaults default___45 = SupportedDefaults.builder()
                .withPriority(45)
                .build();

        SupportedDefaults default___50 = SupportedDefaults.builder()
                .withPriority(50)
                .build();

        SupportedDefaults default__500_50 = SupportedDefaults.builder()
                .withMaxSourceSizeBytes(500L)
                .withPriority(50)
                .build();

        SupportedDefaults default__600_45 = SupportedDefaults.builder()
                .withMaxSourceSizeBytes(600L)
                .withPriority(45)
                .build();

        final TransformConfig firstConfig = TransformConfig.builder()
                .build();

        final TransformConfig secondConfig = TransformConfig.builder()
                .withSupportedDefaults(ImmutableSet.of(
                        default_1A_100)) // 0: transformer and source media type default
                .build();

        final TransformConfig thirdConfig = TransformConfig.builder()
                .withSupportedDefaults(ImmutableSet.of(
                        default_1A_200, // 0: transformer and source media type default
                        default_2A__45, // 0: transformer and source media type default
                        default_3_400, // 1: transformer default
                        default_B_400, // 2: source media type default
                        default_B_500, // 2: source media type default - overrides the previous value 400 defined in the same config
                        default__500_50, // 3: system wide default - totally overridden by the next lines.
                        default__600, // 3: system wide default
                        default___50, // 3: system wide default (combined with the other system default)
                        default___45)) // 3: system wide default - overrides the value 45 defined in the same config
                .build();

        final TransformConfig fourthConfig = TransformConfig.builder()
                .withSupportedDefaults(ImmutableSet.of(
                        SupportedDefaults.builder() // 3: system wide default
                                .withMaxSourceSizeBytes(-1L)
                                .withPriority(45)
                                .build()))
                .build();

        final TransformConfig fifthConfig = TransformConfig.builder()
                .withSupportedDefaults(ImmutableSet.of(
                        SupportedDefaults.builder() // 3: system wide default (reset to the default, so removed)
                                .withPriority(50)
                                .build(),
                        SupportedDefaults.builder() // Invalid as neither priority nor maxSourceSizeBytes are set
                                .withTransformerName("9")
                                .withSourceMediaType("mimetype/z")
                                .build()))
                .build();

        config.addTransformConfig(firstConfig, READ_FROM_A, BASE_URL_A, registry);
        TransformConfig resultConfig = config.buildTransformConfig();
        assertEquals(0, resultConfig.getSupportedDefaults().size());

        config.addTransformConfig(secondConfig, READ_FROM_B, BASE_URL_B, registry);
        resultConfig = config.buildTransformConfig();
        assertEquals(ImmutableSet.of(
                default_1A_100),
                resultConfig.getSupportedDefaults());

        config.addTransformConfig(thirdConfig, READ_FROM_B, BASE_URL_B, registry);
        resultConfig = config.buildTransformConfig();
        assertEquals(ImmutableSet.of(
                default_1A_200, // overrides default_1A_100
                default_2A__45,
                default_3_400,
                default_B_500,
                default__600_45), // default__600 + default___45
                resultConfig.getSupportedDefaults());

        config.addTransformConfig(fourthConfig, READ_FROM_B, BASE_URL_B, registry);
        resultConfig = config.buildTransformConfig();
        assertEquals(5, resultConfig.getSupportedDefaults().size());

        config.addTransformConfig(fifthConfig, READ_FROM_A, BASE_URL_A, registry);
        resultConfig = config.buildTransformConfig();
        assertEquals(4, resultConfig.getSupportedDefaults().size());
        assertEquals(ImmutableSet.of(
                default_1A_200, // overrides default_1A_100
                default_2A__45,
                default_3_400,
                default_B_500), // default__600_45 removed as the system defaults have been reset to the defaults -1 and 50
                resultConfig.getSupportedDefaults());

        String expected = "Unable to process \"supportedDefaults\": [" +
                "{\"transformerName\": \"9\", \"sourceMediaType\": \"mimetype/z\"}]. Read from readFromA";
        assertEquals(1, registry.warnMessages.size());
        assertEquals(expected, registry.warnMessages.get(0));
    }

    @Test
    public void testRemoveSupported()
    {
        addTransformConfig_A2B_X2Y_100_23();

        final TransformConfig secondConfig = TransformConfig.builder()
                .withRemoveSupported(ImmutableSet.of(
                        RemoveSupported.builder()
                                .withTransformerName("1") // c -> d does not exist
                                .withSourceMediaType("mimetype/c")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        RemoveSupported.builder()
                                .withTransformerName("1")
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/b")
                                .build(),
                        RemoveSupported.builder() // transformer does not exist
                                .withTransformerName("bad")
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        RemoveSupported.builder() // transform name not set
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        RemoveSupported.builder() // source type not set
                                .withTransformerName("1")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        RemoveSupported.builder() // target type not set
                                .withTransformerName("1")
                                .withSourceMediaType("mimetype/a")
                                .build()))
                .build();

        String expectedWarnMessage = "Unable to process \"removeSupported\": [" +
                "{\"transformerName\": \"bad\", \"sourceMediaType\": \"mimetype/a\", \"targetMediaType\": \"mimetype/d\"}, " +
                "{\"transformerName\": \"1\", \"sourceMediaType\": \"mimetype/a\"}, " +
                "{\"transformerName\": \"1\", \"sourceMediaType\": \"mimetype/c\", \"targetMediaType\": \"mimetype/d\"}, " +
                "{\"sourceMediaType\": \"mimetype/a\", \"targetMediaType\": \"mimetype/d\"}, " +
                "{\"transformerName\": \"1\", \"targetMediaType\": \"mimetype/d\"}]. " +
                "Read from readFromB";
        ImmutableSet<SupportedSourceAndTarget> expectedSupported = ImmutableSet.of(supported_X2Y_100_23);
        String expectedToString = "[" +
                "{\"sourceMediaType\": \"mimetype/x\", \"targetMediaType\": \"mimetype/y\", \"maxSourceSizeBytes\": \"100\", \"priority\": \"23\"}" +
                "]";

        addTransformConfig(secondConfig, expectedWarnMessage, expectedSupported, expectedToString);
    }

    @Test
    public void testAddSupported()
    {
        addTransformConfig_A2B_X2Y_100_23();

        final TransformConfig secondConfig = TransformConfig.builder()
                .withAddSupported(ImmutableSet.of(
                        AddSupported.builder()
                                .withTransformerName("1")
                                .withSourceMediaType("mimetype/c")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        AddSupported.builder() // duplicates original
                                .withTransformerName("1")
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/b")
                                .withPriority(40)
                                .build(),
                        AddSupported.builder()
                                .withTransformerName("1")
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/d")
                                .withPriority(44)
                                .withMaxSourceSizeBytes(1234)
                                .build(),
                        AddSupported.builder() // transformer does not exist
                                .withTransformerName("bad")
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        AddSupported.builder() // transform name not set
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        AddSupported.builder() // source type not set
                                .withTransformerName("1")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        AddSupported.builder() // target type not set
                                .withTransformerName("1")
                                .withSourceMediaType("mimetype/a")
                                .build()))
                .build();

        String expectedWarnMessage = "Unable to process \"addSupported\": [" +
                "{\"transformerName\": \"1\", \"sourceMediaType\": \"mimetype/a\"}, " +
                "{\"transformerName\": \"1\", \"targetMediaType\": \"mimetype/d\"}, " +
                "{\"transformerName\": \"1\", \"sourceMediaType\": \"mimetype/a\", \"targetMediaType\": \"mimetype/b\", \"priority\": \"40\"}, " +
                "{\"transformerName\": \"bad\", \"sourceMediaType\": \"mimetype/a\", \"targetMediaType\": \"mimetype/d\"}, " +
                "{\"sourceMediaType\": \"mimetype/a\", \"targetMediaType\": \"mimetype/d\"}]. " +
                "Read from readFromB";
        ImmutableSet<SupportedSourceAndTarget> expectedSupported = ImmutableSet.of(
                supported_A2B,
                supported_C2D,
                supported_X2Y_100_23,
                supported_A2D_1234_44);
        String expectedToString = "[" +
                "{\"sourceMediaType\": \"mimetype/x\", \"targetMediaType\": \"mimetype/y\", \"maxSourceSizeBytes\": \"100\", \"priority\": \"23\"}, " +
                "{\"sourceMediaType\": \"mimetype/a\", \"targetMediaType\": \"mimetype/d\", \"maxSourceSizeBytes\": \"1234\", \"priority\": \"44\"}, " +
                "{\"sourceMediaType\": \"mimetype/a\", \"targetMediaType\": \"mimetype/b\"}, " + // priority and size should be missing (i.e. use defaults)
                "{\"sourceMediaType\": \"mimetype/c\", \"targetMediaType\": \"mimetype/d\"}" +
                "]";

        addTransformConfig(secondConfig, expectedWarnMessage, expectedSupported, expectedToString);
    }

    @Test
    public void testOverrideSupported()
    {
        addTransformConfig_A2B_X2Y_100_23();

        final TransformConfig secondConfig = TransformConfig.builder()
                .withOverrideSupported(ImmutableSet.of(
                        OverrideSupported.builder() // does not exist
                                .withTransformerName("1")
                                .withSourceMediaType("mimetype/c")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        OverrideSupported.builder() // priority: Default -> 40, maxSourceSizeBytes: not in override, retained as -1 (unlimited default)
                                .withTransformerName("1")
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/b")
                                .withPriority(40)
                                .build(),
                        OverrideSupported.builder() // maxSourceSizeBytes: 100 -> 200, priority: not in override, retained as 23 from original entry
                                .withTransformerName("1")
                                .withSourceMediaType("mimetype/x")
                                .withTargetMediaType("mimetype/y")
                                .withMaxSourceSizeBytes(200)
                                .build(),
                        OverrideSupported.builder() // transformer does not exist
                                .withTransformerName("bad")
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/d")
                                .build()))
                // overrideSupported uses patch semantics: fields not specified in the override are retained from the existing entry
                .build();

        String expectedWarnMessage = "Unable to process \"overrideSupported\": [" +
                "{\"transformerName\": \"1\", \"sourceMediaType\": \"mimetype/c\", \"targetMediaType\": \"mimetype/d\"}, " +
                "{\"transformerName\": \"bad\", \"sourceMediaType\": \"mimetype/a\", \"targetMediaType\": \"mimetype/d\"}]. " +
                "Read from readFromB";
        ImmutableSet<SupportedSourceAndTarget> expectedSupported = ImmutableSet.of(
                supported_X2Y_200_23,
                supported_A2B_default_40);
        String expectedToString = "[" +
                "{\"sourceMediaType\": \"mimetype/a\", \"targetMediaType\": \"mimetype/b\", \"maxSourceSizeBytes\": \"-1\", \"priority\": \"40\"}, " +
                "{\"sourceMediaType\": \"mimetype/x\", \"targetMediaType\": \"mimetype/y\", \"maxSourceSizeBytes\": \"200\", \"priority\": \"23\"}" +
                "]";

        config.addTransformConfig(secondConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        assertEquals(1, registry.warnMessages.size());
        assertEquals(expectedWarnMessage, registry.warnMessages.get(0));

        Set<SupportedSourceAndTarget> supportedSourceAndTargetList = config.buildTransformConfig().getTransformers().get(0).getSupportedSourceAndTargetList();
        assertEquals(expectedSupported, supportedSourceAndTargetList);
        assertEquals(expectedToString, supportedSourceAndTargetList.toString());
    }

    @Test
    public void testDeferredOverrideForPipelineTransformer()
    {
        addEngineConfig(
                stepTransformer("step1", "mimetype/document", "mimetype/pdf"),
                stepTransformer("step2", "mimetype/pdf", "mimetype/image"),
                pipelineTransformer("pipeline1",
                        new TransformStep("step1", "mimetype/pdf"),
                        new TransformStep("step2", null)));
        addOverrideConfig(OverrideSupported.builder()
                .withTransformerName("pipeline1")
                .withSourceMediaType("mimetype/document").withTargetMediaType("mimetype/image")
                .withPriority(40).build());
        config.combineTransformerConfig(registry);

        assertEquals(40, getEntry(getTransformers(), "pipeline1", "mimetype/document", "mimetype/image").getPriority());
    }

    /**
     * An override on a leaf transformer must propagate to ALL pipeline transformers that use it as their first step, not just the first one found.
     */
    @Test
    public void testOverridePropagatedToPipelineParents()
    {
        // step "1": a->b; used as step 0 by both pipeline "4" (a->c) and pipeline "5" (a->d)
        addEngineConfig(
                stepTransformer("1", "mimetype/a", "mimetype/b", 50, 1000L),
                stepTransformer("2", "mimetype/b", "mimetype/c"),
                stepTransformer("3", "mimetype/b", "mimetype/d"),
                pipelineTransformer("4", new TransformStep("1", "mimetype/b"), new TransformStep("2", null)),
                pipelineTransformer("5", new TransformStep("1", "mimetype/b"), new TransformStep("3", null)));
        addOverrideConfig(OverrideSupported.builder()
                .withTransformerName("1")
                .withSourceMediaType("mimetype/a").withTargetMediaType("mimetype/b")
                .withPriority(10).withMaxSourceSizeBytes(500L).build());
        config.combineTransformerConfig(registry);

        assertEquals(0, registry.warnMessages.size());

        List<Transformer> transformers = getTransformers();
        assertEntry(transformers, "1", "mimetype/a", "mimetype/b", 10, 500L);
        assertEntry(transformers, "4", "mimetype/a", "mimetype/c", 10, 500L);
        assertEntry(transformers, "5", "mimetype/a", "mimetype/d", 10, 500L);
    }

    /**
     * An override whose source media type does not match any entry on the named transformer produces a warning and leaves both the direct entry and any synthesised pipeline entries unchanged.
     */
    @Test
    public void testOverrideWithNoMatchingSourceProducesWarning()
    {
        addEngineConfig(
                stepTransformer("1", "mimetype/a", "mimetype/b", 50, 1000L),
                stepTransformer("2", "mimetype/b", "mimetype/c"),
                pipelineTransformer("3", new TransformStep("1", "mimetype/b"), new TransformStep("2", null)));
        addOverrideConfig(OverrideSupported.builder()
                .withTransformerName("1")
                .withSourceMediaType("mimetype/x").withTargetMediaType("mimetype/b") // "mimetype/x" does not exist on "1"
                .withPriority(10).build());
        config.combineTransformerConfig(registry);

        assertEquals(1, registry.warnMessages.size());
        assertEquals("Unable to process \"overrideSupported\": [" +
                "{\"transformerName\": \"1\", \"sourceMediaType\": \"mimetype/x\", \"targetMediaType\": \"mimetype/b\", \"priority\": \"10\"}]. " +
                "Read from readFromB", registry.warnMessages.get(0));

        List<Transformer> transformers = getTransformers();
        assertEntry(transformers, "1", "mimetype/a", "mimetype/b", 50, 1000L);
        assertEntry(transformers, "3", "mimetype/a", "mimetype/c", 50, 1000L);
    }

    /**
     * An override on a standalone transformer (not the first step of any pipeline) applies correctly to the direct entry. propagateOverrideToPipelineParents is a no-op and no warning is produced.
     */
    @Test
    public void testOverrideOnStandaloneTransformerHasNoPipelineImpact()
    {
        addEngineConfig(stepTransformer("1", "mimetype/a", "mimetype/b", 50, 1000L));
        addOverrideConfig(OverrideSupported.builder()
                .withTransformerName("1")
                .withSourceMediaType("mimetype/a").withTargetMediaType("mimetype/b")
                .withPriority(10).build()); // size not in override → retained
        config.combineTransformerConfig(registry);

        assertEquals(0, registry.warnMessages.size());
        assertEntry(getTransformers(), "1", "mimetype/a", "mimetype/b", 10, 1000L);
    }

    /**
     * When a wildcard-generated pipeline has multiple synthesised final targets (because the second step supports several output formats), an override on step 0 must propagate its constraint to every one of those entries — not just the first one found.
     *
     * <p>
     * Mirrors the "33 entries" scenario: appleIWorks (pages→jpeg) is the first step of a pipeline whose second step can produce png, gif, and pdf from jpeg. Capping pages→jpeg at 786432 bytes must also cap pages→png, pages→gif, and pages→pdf, because the size constraint lives at step 0 regardless of the final output format.
     */
    @Test
    public void testOverridePropagatesMaxSizeToAllWildcardTargetsWithinSinglePipeline()
    {
        // step "2" produces three final outputs (b→c, b→d, b→e) from the intermediate
        addEngineConfig(
                stepTransformer("1", "mimetype/a", "mimetype/b", 50, 1000L),
                stepTransformerMultiTarget("2", "mimetype/b", "mimetype/c", "mimetype/d", "mimetype/e"),
                pipelineTransformer("3", new TransformStep("1", "mimetype/b"), new TransformStep("2", null)));
        addOverrideConfig(OverrideSupported.builder()
                .withTransformerName("1")
                .withSourceMediaType("mimetype/a").withTargetMediaType("mimetype/b")
                .withMaxSourceSizeBytes(786432L).build());
        config.combineTransformerConfig(registry);

        assertEquals(0, registry.warnMessages.size());
        assertEquals(0, registry.errorMessages.size());

        List<Transformer> transformers = getTransformers();
        assertEquals(786432L, getEntry(transformers, "1", "mimetype/a", "mimetype/b").getMaxSourceSizeBytes());
        assertEquals(786432L, getEntry(transformers, "3", "mimetype/a", "mimetype/c").getMaxSourceSizeBytes(),
                "pipeline a→c must inherit the maxSourceSizeBytes cap");
        assertEquals(786432L, getEntry(transformers, "3", "mimetype/a", "mimetype/d").getMaxSourceSizeBytes(),
                "pipeline a→d must inherit the maxSourceSizeBytes cap");
        assertEquals(786432L, getEntry(transformers, "3", "mimetype/a", "mimetype/e").getMaxSourceSizeBytes(),
                "pipeline a→e must inherit the maxSourceSizeBytes cap");
        assertEquals(3, getTransformerByName(transformers, "3").getSupportedSourceAndTargetList().size());
    }

    /**
     * An override on a transformer that participates in a pipeline but is NOT the first step must apply only to the direct entry on that transformer. The synthesised pipeline entries must remain unchanged because their source-size constraint comes from step 0, not a later step.
     */
    @Test
    public void testOverrideOnNonFirstStepDoesNotPropagateToParentPipeline()
    {
        // step "2" has two entries (b→c and b→d) — the override targets b→c only
        final Transformer step2 = Transformer.builder().withTransformerName("2")
                .withSupportedSourceAndTargetList(new HashSet<>(Set.of(
                        SupportedSourceAndTarget.builder()
                                .withSourceMediaType("mimetype/b").withTargetMediaType("mimetype/c")
                                .withPriority(50).withMaxSourceSizeBytes(1000L).build(),
                        SupportedSourceAndTarget.builder()
                                .withSourceMediaType("mimetype/b").withTargetMediaType("mimetype/d")
                                .withPriority(50).withMaxSourceSizeBytes(1000L).build())))
                .build();
        addEngineConfig(
                stepTransformer("1", "mimetype/a", "mimetype/b", 50, 1000L),
                step2,
                pipelineTransformer("3", new TransformStep("1", "mimetype/b"), new TransformStep("2", null)));
        // Override targets step "2" which is NOT the first step of pipeline "3"
        addOverrideConfig(OverrideSupported.builder()
                .withTransformerName("2")
                .withSourceMediaType("mimetype/b").withTargetMediaType("mimetype/c")
                .withMaxSourceSizeBytes(500L).build());
        config.combineTransformerConfig(registry);

        assertEquals(0, registry.warnMessages.size());
        assertEquals(0, registry.errorMessages.size());

        List<Transformer> transformers = getTransformers();
        assertEquals(500L, getEntry(transformers, "2", "mimetype/b", "mimetype/c").getMaxSourceSizeBytes());
        // Pipeline entries inherit from step1 (first step) — override on step2 must not touch them
        assertEquals(1000L, getEntry(transformers, "3", "mimetype/a", "mimetype/c").getMaxSourceSizeBytes(),
                "pipeline a→c must not be affected by an override on a non-first step");
        assertEquals(1000L, getEntry(transformers, "3", "mimetype/a", "mimetype/d").getMaxSourceSizeBytes(),
                "pipeline a→d must not be affected by an override on a non-first step");
    }

    // ---- Transformer factory helpers ----

    private static Transformer stepTransformer(String name, String src, String tgt)
    {
        return Transformer.builder().withTransformerName(name)
                .withSupportedSourceAndTargetList(new HashSet<>(Set.of(
                        SupportedSourceAndTarget.builder()
                                .withSourceMediaType(src).withTargetMediaType(tgt).build())))
                .build();
    }

    private static Transformer stepTransformer(String name, String src, String tgt, int priority, long maxSourceSizeBytes)
    {
        return Transformer.builder().withTransformerName(name)
                .withSupportedSourceAndTargetList(new HashSet<>(Set.of(
                        SupportedSourceAndTarget.builder()
                                .withSourceMediaType(src).withTargetMediaType(tgt)
                                .withPriority(priority).withMaxSourceSizeBytes(maxSourceSizeBytes).build())))
                .build();
    }

    private static Transformer stepTransformerMultiTarget(String name, String src, String... targets)
    {
        Set<SupportedSourceAndTarget> entries = new HashSet<>();
        for (String tgt : targets)
        {
            entries.add(SupportedSourceAndTarget.builder()
                    .withSourceMediaType(src).withTargetMediaType(tgt).build());
        }
        return Transformer.builder().withTransformerName(name)
                .withSupportedSourceAndTargetList(entries).build();
    }

    private static Transformer pipelineTransformer(String name, TransformStep... steps)
    {
        return Transformer.builder().withTransformerName(name)
                .withTransformerPipeline(List.of(steps)).build();
    }

    // ---- Config registration helpers ----

    private void addEngineConfig(Transformer... transformers)
    {
        config.addTransformConfig(TransformConfig.builder()
                .withTransformers(ImmutableList.copyOf(transformers)).build(),
                READ_FROM_A, BASE_URL_A, registry);
    }

    private void addOverrideConfig(OverrideSupported... overrides)
    {
        config.addTransformConfig(TransformConfig.builder()
                .withOverrideSupported(ImmutableSet.copyOf(overrides)).build(),
                READ_FROM_B, BASE_URL_B, registry);
    }

    private List<Transformer> getTransformers()
    {
        return config.buildTransformConfig().getTransformers();
    }

    // ---- Assertion helpers ----

    private void assertEntry(List<Transformer> transformers, String name, String src, String tgt,
            int expectedPriority, long expectedMaxSize)
    {
        SupportedSourceAndTarget entry = getEntry(transformers, name, src, tgt);
        assertEquals(expectedPriority, entry.getPriority());
        assertEquals(expectedMaxSize, entry.getMaxSourceSizeBytes());
    }

    private static Transformer getTransformerByName(List<Transformer> transformers, String name)
    {
        return transformers.stream().filter(t -> name.equals(t.getTransformerName())).findFirst().orElseThrow();
    }

    private SupportedSourceAndTarget getEntry(List<Transformer> transformers,
            String transformerName, String src, String tgt)
    {
        return transformers.stream()
                .filter(t -> transformerName.equals(t.getTransformerName()))
                .findFirst().orElseThrow()
                .getSupportedSourceAndTargetList().stream()
                .filter(e -> src.equals(e.getSourceMediaType()) && tgt.equals(e.getTargetMediaType()))
                .findFirst().orElseThrow();
    }

    private void addTransformConfig_A2B_X2Y_100_23()
    {
        config.addTransformConfig(transformConfig_A2B_X2Y_100_23, READ_FROM_A, BASE_URL_A, registry);
        TransformConfig resultConfig = config.buildTransformConfig();
        assertEquals(1, resultConfig.getTransformers().size());
        assertEquals(2, resultConfig.getTransformers().get(0).getSupportedSourceAndTargetList().size());
    }

    private void addTransformConfig(TransformConfig secondConfig, String expectedWarnMessage,
            Set<SupportedSourceAndTarget> expectedSupported, String expectedToString)
    {
        config.addTransformConfig(secondConfig, READ_FROM_B, BASE_URL_B, registry);
        TransformConfig resultConfig = config.buildTransformConfig();

        assertEquals(1, registry.warnMessages.size());
        assertEquals(expectedWarnMessage, registry.warnMessages.get(0));

        Set<SupportedSourceAndTarget> supportedSourceAndTargetList = resultConfig.getTransformers().get(0).getSupportedSourceAndTargetList();
        assertTrue(supportedSourceAndTargetList.equals(expectedSupported));

        assertEquals(expectedToString, supportedSourceAndTargetList.toString());
    }
}
