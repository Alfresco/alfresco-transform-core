/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2021 - 2022 Alfresco Software Limited
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.alfresco.transform.config.SupportedSourceAndTarget;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.TransformStep;
import org.alfresco.transform.config.Transformer;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Test the CombinedTransformConfig, extended by both T-Router and ACS repository.
 *
 * @author adavis
 */
public class CombinedTransformConfigTest
{
    private static final Transformer TRANSFORMER1_A2B_A2E = Transformer.builder().withTransformerName("1")
            .withSupportedSourceAndTargetList(Set.of(
                    SupportedSourceAndTarget.builder()
                            .withSourceMediaType("mimetype/a")
                            .withTargetMediaType("mimetype/b")
                            .build(),
                    SupportedSourceAndTarget.builder()
                            .withSourceMediaType("mimetype/a")
                            .withTargetMediaType("mimetype/e")
                            .build()))
            .build();

    private static final Transformer TRANSFORMER1_A2B = Transformer.builder().withTransformerName("1")
            .withSupportedSourceAndTargetList(Set.of(
                    SupportedSourceAndTarget.builder()
                            .withSourceMediaType("mimetype/a")
                            .withTargetMediaType("mimetype/b")
                            .build()))
            .build();

    private static final Transformer TRANSFORMER1_A2E = Transformer.builder().withTransformerName("1")
            .withSupportedSourceAndTargetList(Set.of(
                    SupportedSourceAndTarget.builder()
                            .withSourceMediaType("mimetype/a")
                            .withTargetMediaType("mimetype/e")
                            .build()))
            .build();

    private static final Transformer TRANSFORMER1_A2D = Transformer.builder().withTransformerName("1")
            .withSupportedSourceAndTargetList(Set.of(
                    SupportedSourceAndTarget.builder()
                            .withSourceMediaType("mimetype/a")
                            .withTargetMediaType("mimetype/d")
                            .build()))
            .build();

    // Not static as pipelines gets modified.
    private static final Transformer PIPELINE1_2C3 = Transformer.builder().withTransformerName("1")
            .withTransformerPipeline(List.of(
                    new TransformStep("2", "mimetype/c"),
                    new TransformStep("3", null)))
            .build();

    // Not static as pipelines gets modified.
    private static final Transformer PIPELINE1_2C3_COPY = Transformer.builder().withTransformerName("1")
            .withTransformerPipeline(List.of(
                    new TransformStep("2", "mimetype/c"),
                    new TransformStep("3", null)))
            .build();

    private static final Transformer TRANSFORMER2_B2C = Transformer.builder().withTransformerName("2")
            .withSupportedSourceAndTargetList(Set.of(
                    SupportedSourceAndTarget.builder()
                            .withSourceMediaType("mimetype/b")
                            .withTargetMediaType("mimetype/c")
                            .build()))
            .build();

    private static final Transformer TRANSFORMER2_A2C = Transformer.builder().withTransformerName("2")
            .withSupportedSourceAndTargetList(Set.of(
                    SupportedSourceAndTarget.builder()
                            .withSourceMediaType("mimetype/a")
                            .withTargetMediaType("mimetype/c")
                            .build()))
            .build();

    private static final Transformer TRANSFORMER2_B2X = Transformer.builder().withTransformerName("2")
            .withSupportedSourceAndTargetList(Set.of(
                    SupportedSourceAndTarget.builder()
                            .withSourceMediaType("mimetype/b")
                            .withTargetMediaType("mimetype/X")
                            .build()))
            .build();

    private static final Transformer TRANSFORMER3_C2D = Transformer.builder().withTransformerName("3")
            .withSupportedSourceAndTargetList(Set.of(
                    SupportedSourceAndTarget.builder()
                            .withSourceMediaType("mimetype/c")
                            .withTargetMediaType("mimetype/d")
                            .build()))
            .build();

    private static final Transformer TRANSFORMER3_C2D_C2E = Transformer.builder().withTransformerName("3")
            .withSupportedSourceAndTargetList(Set.of(
                    SupportedSourceAndTarget.builder()
                            .withSourceMediaType("mimetype/c")
                            .withTargetMediaType("mimetype/d")
                            .build(),
                    SupportedSourceAndTarget.builder()
                            .withSourceMediaType("mimetype/a")
                            .withTargetMediaType("mimetype/e")
                            .build()))
            .build();

    private static final Transformer TRANSFORMER1 = Transformer.builder().withTransformerName("1").build();

    private static final Transformer TRANSFORMER2 = Transformer.builder().withTransformerName("2").build();

    private static final Transformer TRANSFORMER3 = Transformer.builder().withTransformerName("3").build();

    public static final Transformer TRANSFORMER_WITH_NO_NAME = Transformer.builder().withTransformerName("").build();

    public static final String PASS_THROUGH_NAME = "PassThrough";

    private static final Transformer PASS_THROUGH = Transformer.builder().withTransformerName(PASS_THROUGH_NAME).build();

    private static final Transformer PASS_THROUGH_COPY = Transformer.builder().withTransformerName(PASS_THROUGH_NAME).build();

    // Not static as pipelines get modified.
    private final Transformer PIPELINE4_1B2C3 = Transformer.builder().withTransformerName("4")
            .withTransformerPipeline(List.of(
                    new TransformStep("1", "mimetype/b"),
                    new TransformStep("2", "mimetype/c"),
                    new TransformStep("3", null)))
            .build();

    // Not static as failover transforms get modified.
    private final Transformer FAILOVER1_23 = Transformer.builder().withTransformerName("1")
            .withTransformerFailover(List.of("2", "3"))
            .build();

    // Not static as failover transforms get modified.
    private final Transformer FAILOVER4_123 = Transformer.builder().withTransformerName("4")
            .withTransformerFailover(List.of("1", "2", "3"))
            .build();

    // Not static as the nested pipeline gets modified.
    private final TransformConfig ONE_TRANSFORM = TransformConfig
            .builder()
            .withTransformers(ImmutableList.of(PIPELINE1_2C3))
            .build();

    private static final TransformConfig TWO_TRANSFORMS = TransformConfig
            .builder()
            .withTransformOptions(ImmutableMap.of(
                    "options/1", emptySet(),
                    "options/2", emptySet()))
            .withTransformers(ImmutableList.of(TRANSFORMER2_B2C, TRANSFORMER3_C2D))
            .build();

    private static final String READ_FROM_A = "readFromA";
    private static final String READ_FROM_B = "readFromB";
    private static final String ROUTER_CONFIG_HAS_NO_BASE_URL = null;
    private static final String BASE_URL_B = "baseUrlB";

    private final CombinedTransformConfig config = new CombinedTransformConfig()
    {
        @Override
        protected boolean isPassThroughTransformName(String name)
        {
            return PASS_THROUGH_NAME.equals(name);
        }
    };

    private final TestTransformRegistry registry = new TestTransformRegistry();

    private String expectedWildcardError(String errorReason)
    {
        return "No supported source and target mimetypes could be added to the transformer \"4\" as " +
                errorReason + ". Read from " + READ_FROM_B;
    }

    // Assumes a t-engine transformer named "1" (normally TRANSFORMER1_A2B_A2E) is being overridden by an A2D transform.
    // The overriding transform should be the last element in the tRouterTransformers (or if empty the last element in
    // tEngineTransformers). The override is expected to good unless an error message is provided.
    // A check is made at the end that A2D is possible and that A2B is not possible.
    private void assertOverride(List<Transformer> tEngineTransformers,
                                List<Transformer> tRouterTransformers, String expectedError)
    {
        Transformer expectedTransformer = tRouterTransformers.isEmpty()
                ? tEngineTransformers.get(tEngineTransformers.size() - 1)
                : tRouterTransformers.get(tRouterTransformers.size() - 1);

        final TransformConfig tEngineTransformConfig = TransformConfig.builder()
                .withTransformers(tEngineTransformers)
                .build();
        final TransformConfig tRouterTransformConfig = TransformConfig.builder()
                .withTransformers(tRouterTransformers)
                .build();

        config.addTransformConfig(tEngineTransformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.addTransformConfig(tRouterTransformConfig, READ_FROM_A, ROUTER_CONFIG_HAS_NO_BASE_URL, registry);
        config.combineTransformerConfig(registry);
        config.registerCombinedTransformers(registry);

        TransformConfig transformConfig = config.buildTransformConfig();

        if (expectedError == null)
        {
            assertEquals(0, registry.errorMessages.size());
            int numberOfTEngineTransformers = tEngineTransformers.size();
            assertEquals(numberOfTEngineTransformers, config.buildTransformConfig().getTransformers().size());

            // Check we are using the overriding transformer from the t-router, or if none the last one in the t-engine
            assertEquals(numberOfTEngineTransformers, transformConfig.getTransformers().size());
            Transformer actualTransformer = transformConfig.getTransformers().get(numberOfTEngineTransformers - 1);
            assertEquals(expectedTransformer, actualTransformer);
            assertSame("It should even == the expected transform", expectedTransformer, actualTransformer);

            // Check the baseUrl is that of the original t-engine that will do the work, if the overriding transform
            // is a single step transform.
            List<TransformStep> pipeline = actualTransformer.getTransformerPipeline();
            List<String> failover = actualTransformer.getTransformerFailover();
            boolean isPipeline = pipeline != null && !pipeline.isEmpty();
            boolean isFailover = failover != null && !failover.isEmpty();
            if (!isPipeline && !isFailover)
            {
                assertEquals(BASE_URL_B, registry.transformerBaseUrls.get(actualTransformer));
            }

            // Double check by finding the overriding transformer for A2D but not the overridden transformer for A2B in
            // the registry.
            assertEquals("1", registry.findTransformerName("mimetype/a", -1,
                    "mimetype/d", emptyMap(), null));
            // If the assumption about the overridden transform being A2D was wrong, the following would pass anyway
            assertNull(registry.findTransformerName("mimetype/a", -1,
                    "mimetype/b", emptyMap(), null));
        }
        else
        {
            assertEquals(1, registry.errorMessages.size());
            assertEquals(expectedError, registry.errorMessages.get(0));
        }
    }

    // No specific tests for addTransformConfig or buildTransformConfig as they are used in most
    // other test methods which will fail if they are not working.

    @Test
    public void testClear()
    {
        config.addTransformConfig(TWO_TRANSFORMS, READ_FROM_B, BASE_URL_B, registry);
        assertEquals(2, config.buildTransformConfig().getTransformers().size());
        assertEquals(2, config.buildTransformConfig().getTransformOptions().size());

        config.clear();

        assertEquals(0, config.buildTransformConfig().getTransformers().size());
        assertEquals(0, config.buildTransformConfig().getTransformOptions().size());
    }

    @Test
    public void testCombineTransformerConfigNoOp()
    {
        config.addTransformConfig(TWO_TRANSFORMS, READ_FROM_B, BASE_URL_B, registry);
        config.addTransformConfig(ONE_TRANSFORM, READ_FROM_A, ROUTER_CONFIG_HAS_NO_BASE_URL, registry);
        assertEquals(3, config.buildTransformConfig().getTransformers().size());
        assertEquals(2, config.buildTransformConfig().getTransformOptions().size());

        config.combineTransformerConfig(registry);
        assertEquals(3, config.buildTransformConfig().getTransformers().size());
        assertEquals(2, config.buildTransformConfig().getTransformOptions().size());
    }

    @Test
    public void testTransformersAreRegistered()
    {
        config.addTransformConfig(TWO_TRANSFORMS, READ_FROM_B, BASE_URL_B, registry);
        config.addTransformConfig(ONE_TRANSFORM, READ_FROM_A, ROUTER_CONFIG_HAS_NO_BASE_URL, registry);
        assertEquals(3, config.buildTransformConfig().getTransformers().size());
        assertEquals(2, config.buildTransformConfig().getTransformOptions().size());

        config.registerCombinedTransformers(registry);

        assertEquals(3, registry.registeredCount);
        assertEquals(1, registry.readFromACount);
        assertEquals(2, registry.baseUrlBCount);
    }

    @Test
    public void testInvalidBothPipelineAndFailover()
    {
        final Transformer invalidTransformer = Transformer.builder().withTransformerName("2")
                .withTransformerPipeline(List.of(
                        new TransformStep("1", "mimetype/b"),
                        new TransformStep("2", "mimetype/c"),
                        new TransformStep("3", null)))
                .withTransformerFailover(List.of("1", "2", "3"))
                .build();
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER1_A2B_A2E,
                        TRANSFORMER2_B2C,
                        TRANSFORMER3_C2D,
                        invalidTransformer))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        String expected = "Transformer \"2\" cannot have pipeline and failover sections. Read from readFromB";
        assertEquals(1, registry.errorMessages.size());
        assertEquals(expected, registry.errorMessages.get(0));
    }

    @Test
    public void testInvalidNoName()
    {
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(TRANSFORMER_WITH_NO_NAME))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        String expected = "Transformer names may not be null. Read from readFromB";
        assertEquals(1, registry.errorMessages.size());
        assertEquals(expected, registry.errorMessages.get(0));
    }

    @Test
    public void testInvalidTransformOptionsUnknown()
    {
        final Transformer transformer = Transformer.builder().withTransformerName("1")
                .withTransformOptions(ImmutableSet.of("unknown"))
                .build();
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(transformer))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        String expected = "Transformer \"1\" references \"unknown\" which do not exist. Read from readFromB";
        assertEquals(1, registry.errorMessages.size());
        assertEquals(expected, registry.errorMessages.get(0));
    }

    @Test
    public void testInvalidDuplicateTEngineSingleStepTransform()
    {
        final Transformer transformer = Transformer.builder().withTransformerName("1")
                .build();
        final Transformer identicalTransformer = Transformer.builder().withTransformerName("1")
                .build();
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        transformer,
                        identicalTransformer))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        String expected = "Transformer \"1\" must be a unique name. Read from readFromB";
        assertEquals(1, registry.errorMessages.size());
        assertEquals(expected, registry.errorMessages.get(0));
    }

    @Test
    public void testOverrideSingleStepWithSingleStepInTEngine()
    {
        final Transformer transformerWithSameNameButDifferentDefinition = Transformer.builder().withTransformerName("1")
                .withTransformOptions(ImmutableSet.of("options/1"))
                .build();
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER1,
                        transformerWithSameNameButDifferentDefinition))
                .withTransformOptions(ImmutableMap.of("options/1", emptySet()))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        // Expected: This is the same error as in testInvalidDuplicateTEngineSingleStepTransform
        String expected = "Transformer \"1\" must be a unique name. Read from readFromB";
        assertEquals(1, registry.errorMessages.size());
        assertEquals(expected, registry.errorMessages.get(0));
    }

    @Test
    public void testInvalidSingleStepTransformInRouter()
    {
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(TRANSFORMER1))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_A, ROUTER_CONFIG_HAS_NO_BASE_URL, registry);
        config.combineTransformerConfig(registry);

        String expected = "Single step transformers (such as \"1\") must be defined in a T-Engine rather than in a " +
                "pipeline file, unless they are overriding an existing single step definition. Read from readFromA";
        assertEquals(1, registry.errorMessages.size());
        assertEquals(expected, registry.errorMessages.get(0));
    }

    @Test
    public void testOverrideSingleStepWithSingleStepInTRouter() // i.e. t-router redefines a t-engine single step transform
    {
        assertOverride(
                ImmutableList.of(TRANSFORMER1_A2B_A2E),
                ImmutableList.of(TRANSFORMER1_A2D),
                null);
    }

    @Test
    public void testOverrideMultipleTimesSingleStepWithSingleStepInTRouter() // need to make sure the url is remembered
    {
        assertOverride(
                ImmutableList.of(TRANSFORMER1_A2B_A2E),
                ImmutableList.of(TRANSFORMER1_A2B, TRANSFORMER1_A2E, TRANSFORMER1_A2D),
                null);
    }

    @Test
    public void testOverrideSingleStepWithPipelineInTRouter()
    {
        assertOverride(
                ImmutableList.of(TRANSFORMER1_A2B_A2E, TRANSFORMER2_A2C, TRANSFORMER3_C2D),
                ImmutableList.of(PIPELINE1_2C3),
                null);
    }

    @Test
    public void testOverridePipelineWithPipelineInTRouter()
    {
        assertOverride(
                ImmutableList.of(PIPELINE1_2C3, TRANSFORMER2_A2C, TRANSFORMER3_C2D),
                ImmutableList.of(PIPELINE1_2C3_COPY),
                null);
    }

    @Test
    public void testInvalidOverridePipelineWithSingleStepInTRouter()
    {
        assertOverride(
                ImmutableList.of(PIPELINE1_2C3, TRANSFORMER2_A2C, TRANSFORMER3_C2D),
                ImmutableList.of(TRANSFORMER1_A2D),
                "Single step transformers (such as \"1\") may not override a pipeline or failover " +
                        "transform as there is no T-Engine to perform work. Read from readFromA");
    }

    @Test
    public void testInvalidOverrideFailoverWithSingleStepInTRouter()
    {
        assertOverride(
                ImmutableList.of(FAILOVER1_23, TRANSFORMER2_A2C, TRANSFORMER3_C2D),
                ImmutableList.of(TRANSFORMER1_A2D),
                "Single step transformers (such as \"1\") may not override a pipeline or failover " +
                        "transform as there is no T-Engine to perform work. Read from readFromA");
    }

    @Test
    public void testSinglePassThroughInTRouter()
    {
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(PASS_THROUGH))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_A, ROUTER_CONFIG_HAS_NO_BASE_URL, registry);
        config.combineTransformerConfig(registry);

        assertEquals(0, registry.errorMessages.size());
    }

    @Test
    public void testMultiplePassThroughInTRouter()
    {
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(PASS_THROUGH, PASS_THROUGH_COPY))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_A, ROUTER_CONFIG_HAS_NO_BASE_URL, registry);
        config.combineTransformerConfig(registry);

        String expected = "Pipeline files should not use \"PassThrough\" as a transform name. Read from readFromA";
        assertEquals(1, registry.errorMessages.size());
        assertEquals(expected, registry.errorMessages.get(0));
    }

    @Test
    public void testPassThroughInTEngine()
    {
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(PASS_THROUGH))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        String expected = "T-Engines should not use \"PassThrough\" as a transform name. Read from readFromB";
        assertEquals(1, registry.errorMessages.size());
        assertEquals(expected, registry.errorMessages.get(0));
    }

    @Test
    public void testInvalidOverrideSingleStepWithPipelineInTEngine()
    {
        assertOverride(
                ImmutableList.of(TRANSFORMER1_A2B_A2E, TRANSFORMER2_A2C, TRANSFORMER3_C2D, PIPELINE1_2C3),
                emptyList(),
                "Transformer \"1\" must be a unique name. Read from readFromB");
    }

    @Test
    public void testInvalidOverridePipelineWithSingleStepInTEngine()
    {
        assertOverride(
                ImmutableList.of(TRANSFORMER2_A2C, TRANSFORMER3_C2D, PIPELINE1_2C3, TRANSFORMER1_A2B_A2E),
                emptyList(),
                "Transformer \"1\" must be a unique name. Read from readFromB");
    }

    @Test
    public void testInvalidIndexToRemoveBeforeCurrent()
    {
        // indexToRemove is is before the current i value, so we are removing an overridden transform
        // Code throws an IllegalArgumentException and i is simply decremented to ignore the current value
        final TransformConfig tEngineTransformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER1_A2B_A2E,
                        TRANSFORMER2_B2C,
                        TRANSFORMER3_C2D))
                .build();
        final TransformConfig tRouterTransformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER1_A2D)) // this one triggers the removal of 0
                .build();

        config.addTransformConfig(tEngineTransformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.addTransformConfig(tRouterTransformConfig, READ_FROM_A, ROUTER_CONFIG_HAS_NO_BASE_URL, registry);
        config.combineTransformerConfig(registry);

        assertEquals(3, config.buildTransformConfig().getTransformers().size());
    }

    @Test
    public void testInvalidIndexToRemoveIsCurrent()
    {
        // Code throws an IllegalArgumentException and i is simply decremented to ignore the current value
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER1_A2D,
                        TRANSFORMER2_B2C,
                        TRANSFORMER_WITH_NO_NAME, // discarded
                        TRANSFORMER3_C2D))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        assertEquals(3, config.buildTransformConfig().getTransformers().size());
    }

    @Test
    public void testSortSoNoForwardRefs()
    {
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        PIPELINE1_2C3,
                        TRANSFORMER2_B2C,
                        TRANSFORMER3_C2D))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        // Check order includes forward references after setup
        List<Transformer> transformers = config.buildTransformConfig().getTransformers();
        assertEquals(3, transformers.size());
        assertEquals("1", transformers.get(0).getTransformerName());
        assertEquals("2", transformers.get(1).getTransformerName());
        assertEquals("3", transformers.get(2).getTransformerName());

        config.combineTransformerConfig(registry);

        // Check order changed so there are no forward references after combined
        transformers = config.buildTransformConfig().getTransformers();
        assertEquals(3, transformers.size());
        assertEquals("2", transformers.get(0).getTransformerName());
        assertEquals("3", transformers.get(1).getTransformerName());
        assertEquals("1", transformers.get(2).getTransformerName());

        assertEquals(0, registry.warnMessages.size());
        assertEquals(0, registry.errorMessages.size());
    }

    @Test
    public void testInvalidTransformStepNullIntermediateMimetype()
    {
        final Transformer pipeline = Transformer.builder().withTransformerName("5")
                .withTransformerPipeline(List.of(
                        new TransformStep("1", "mimetype/b"),
                        new TransformStep("2", null), // should not be null
                        new TransformStep("3", null)))
                .build();
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER1_A2B,
                        TRANSFORMER2_B2C,
                        TRANSFORMER3_C2D,
                        pipeline))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        String expected = "No supported source and target mimetypes could be added to the transformer \"5\" as " +
                "intermediate steps should have a target mimetype. Read from readFromB";
        assertEquals(1, registry.errorMessages.size());
        assertEquals(expected, registry.errorMessages.get(0));
    }

    @Test
    public void testInvalidTransformStepNonNullFinalMimetype()
    {
        final Transformer pipeline = Transformer.builder().withTransformerName("5")
                .withTransformerPipeline(List.of(
                        new TransformStep("1", "mimetype/b"),
                        new TransformStep("2", "mimetype/c"),
                        new TransformStep("3", "mimetype/d")))  // the last step's mimetype should be null
                .build();
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER1_A2B,
                        TRANSFORMER2_B2C,
                        TRANSFORMER3_C2D,
                        pipeline))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        String expected = "No supported source and target mimetypes could be added to the transformer \"5\" as " +
                "the final step should not have a target mimetype. Read from readFromB";
        assertEquals(1, registry.errorMessages.size());
        assertEquals(expected, registry.errorMessages.get(0));
    }

    @Test
    public void testIgnorePipelineWithMissingStep()
    {
        final Transformer pipeline = Transformer.builder().withTransformerName("1")
                .withTransformerPipeline(List.of(
                        new TransformStep("2", "mimetype/c"),
                        new TransformStep("3", "mimetype/d"),
                        new TransformStep("4", null)))
                .build();
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER2_B2C,
                        TRANSFORMER3_C2D,
                        pipeline))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        String expected = "Transformer \"1\" ignored as step transforms (\"4\") do not exist. Read from readFromB";
        assertEquals(1, registry.warnMessages.size());
        assertEquals(expected, registry.warnMessages.get(0));
    }

    @Test
    public void testIgnorePipelineWithMissingSteps()
    {
        final Transformer pipeline = Transformer.builder().withTransformerName("1")
                .withTransformerPipeline(List.of(
                        new TransformStep("3", "mimetype/d"),
                        new TransformStep("4", "mimetype/e"),
                        new TransformStep("5", "mimetype/f"),
                        new TransformStep("2", "mimetype/c"),
                        new TransformStep("6", null)))
                .build();
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER2_B2C,
                        TRANSFORMER3_C2D,
                        pipeline))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        String expected = "Transformer \"1\" ignored as step transforms (\"4\", \"5\", \"6\") do not exist. Read from readFromB";
        assertEquals(1, registry.warnMessages.size());
        assertEquals(expected, registry.warnMessages.get(0));
    }

    @Test
    public void testInvalidCircularTransformStep()
    {
        final Transformer pipeline1 = Transformer.builder().withTransformerName("1")
                .withTransformerPipeline(List.of(
                        new TransformStep("2", "mimetype/c"),
                        new TransformStep("4", null)))
                .build();
        final Transformer pipeline4 = Transformer.builder().withTransformerName("4")
                .withTransformerPipeline(List.of(
                        new TransformStep("3", "mimetype/d"),
                        new TransformStep("5", "mimetype/f"),
                        new TransformStep("1", null)))
                .build();
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER2_B2C,
                        TRANSFORMER3_C2D,
                        pipeline1,
                        pipeline4))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        assertEquals(2, registry.warnMessages.size());
        assertEquals("Transformer \"1\" ignored as step transforms (\"4\") do not exist. Read from readFromB",
                registry.warnMessages.get(0));
        assertEquals("Transformer \"4\" ignored as step transforms (\"1\", \"5\") do not exist. Read from readFromB",
                registry.warnMessages.get(1));
    }

    @Test
    public void testWildcardNoOp()
    {
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER1_A2B_A2E,
                        TRANSFORMER2_B2C,
                        TRANSFORMER3_C2D))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);
        config.registerCombinedTransformers(registry);

        assertEquals(0, registry.errorMessages.size());
        assertEquals(3, config.buildTransformConfig().getTransformers().size());
        assertEquals("1", registry.findTransformerName("mimetype/a", -1,
                "mimetype/b", emptyMap(), null));
        assertEquals("2", registry.findTransformerName("mimetype/b", -1,
                "mimetype/c", emptyMap(), null));
        assertEquals("3", registry.findTransformerName("mimetype/c", -1,
                "mimetype/d", emptyMap(), null));
    }

    // It is not possible to test for expectedWildcardError("one of the step transformers is missing") as
    // the sortTransformers() method will have already issued another error.

    @Test
    public void testWildcardPipeline()
    {
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER1_A2B_A2E,
                        TRANSFORMER2_B2C,
                        TRANSFORMER3_C2D,
                        PIPELINE4_1B2C3))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);
        config.registerCombinedTransformers(registry);

        assertEquals(0, registry.errorMessages.size());
        assertEquals(4, config.buildTransformConfig().getTransformers().size());
        assertEquals("4", registry.findTransformerName("mimetype/a", -1,
                "mimetype/d", emptyMap(), null));
        assertEquals("1", registry.findTransformerName("mimetype/a", -1,
                "mimetype/b", emptyMap(), null));
    }

    @Test
    public void testWildcardPipelineExcludeFirstTransformsSupportedFromCartesianProduct() // Exclude more complex path
    {
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER1_A2B_A2E,
                        TRANSFORMER2_B2C,
                        TRANSFORMER3_C2D_C2E,
                        PIPELINE4_1B2C3))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);
        config.registerCombinedTransformers(registry);

        assertEquals(0, registry.errorMessages.size());
        assertEquals(4, config.buildTransformConfig().getTransformers().size());
        assertEquals("4", registry.findTransformerName("mimetype/a", -1,
                "mimetype/d", emptyMap(), null));

        // The pipeline could do A2D and A2E, but as A2E is also supported by the first transformer it is not included.
        assertEquals("1", registry.findTransformerName("mimetype/a", -1,
                "mimetype/e", emptyMap(), null));
    }

    @Test
    public void testPipelineUnsupportedIntermediate()
    {
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER1_A2B_A2E,
                        TRANSFORMER2_B2X,
                        TRANSFORMER3_C2D,
                        PIPELINE4_1B2C3))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        String expected = expectedWildcardError("the step transformer \"2\" does not support \"mimetype/b\" to \"mimetype/c\"");
        assertEquals(1, registry.errorMessages.size());
        assertEquals(expected, registry.errorMessages.get(0));

        // 4: the pipeline is not removed, but will not be used as it has no supported transforms.
        assertEquals(4, config.buildTransformConfig().getTransformers().size());
    }

    @Test
    public void testWildcardFailover()
    {
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER1_A2B_A2E,
                        TRANSFORMER2_B2C,
                        TRANSFORMER3_C2D,
                        FAILOVER4_123))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);
        config.registerCombinedTransformers(registry);

        assertEquals(0, registry.errorMessages.size());
        assertEquals(4, config.buildTransformConfig().getTransformers().size());
        assertEquals("4", registry.findTransformerName("mimetype/a", -1,
                "mimetype/b", emptyMap(), null));
    }

    @Test
    public void testWildcardFailoverNoneSupported()
    {
        final TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(ImmutableList.of(
                        TRANSFORMER1,
                        TRANSFORMER2,
                        TRANSFORMER3,
                        FAILOVER4_123))
                .build();

        config.addTransformConfig(transformConfig, READ_FROM_B, BASE_URL_B, registry);
        config.combineTransformerConfig(registry);

        String expected = expectedWildcardError("the step transforms don't support any");
        assertEquals(1, registry.errorMessages.size());
        assertEquals(expected, registry.errorMessages.get(0));
    }
}
