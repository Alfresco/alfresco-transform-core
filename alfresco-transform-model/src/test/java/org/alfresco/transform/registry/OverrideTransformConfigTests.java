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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.alfresco.transform.config.AddSupported;
import org.alfresco.transform.config.SupportedDefaults;
import org.alfresco.transform.config.OverrideSupported;
import org.alfresco.transform.config.RemoveSupported;
import org.alfresco.transform.config.SupportedSourceAndTarget;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.Transformer;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the json elements: {@code removeTransformers}, {@code addSupported}, {@code removeSupported},
 * {@code overrideSupported} and {@code supportedDefaults}.
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

    private final SupportedSourceAndTarget supported_A2B__40 = SupportedSourceAndTarget.builder()
            .withSourceMediaType("mimetype/a")
            .withTargetMediaType("mimetype/b")
            .withPriority(40)
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

    private final SupportedSourceAndTarget supported_X2Y_200 = SupportedSourceAndTarget.builder()
            .withSourceMediaType("mimetype/x")
            .withTargetMediaType("mimetype/y")
            .withMaxSourceSizeBytes(200L)
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

    private final TestTransformRegistry registry = new TestTransformRegistry();

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
                        default_1A_200,  // 0: transformer and source media type default
                        default_2A__45,  // 0: transformer and source media type default
                        default_3_400,   // 1: transformer default
                        default_B_400,   // 2: source media type default
                        default_B_500,   // 2: source media type default - overrides the previous value 400 defined in the same config
                        default__500_50, // 3: system wide default - totally overridden by the next lines.
                        default__600,    // 3: system wide default
                        default___50,    // 3: system wide default (combined with the other system default)
                        default___45))   // 3: system wide default - overrides the value 45 defined in the same config
                .build();

        final TransformConfig fourthConfig = TransformConfig.builder()
                .withSupportedDefaults(ImmutableSet.of(
                        SupportedDefaults.builder()  // 3: system wide default
                                .withMaxSourceSizeBytes(-1L)
                                .withPriority(45)
                                .build()))
                .build();

        final TransformConfig fifthConfig = TransformConfig.builder()
                .withSupportedDefaults(ImmutableSet.of(
                        SupportedDefaults.builder()  // 3: system wide default (reset to the default, so removed)
                                .withPriority(50)
                                .build(),
                        SupportedDefaults.builder()  // Invalid as neither priority nor maxSourceSizeBytes are set
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
                                .withTransformerName("1")             // c -> d does not exist
                                .withSourceMediaType("mimetype/c")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        RemoveSupported.builder()
                                .withTransformerName("1")
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/b")
                                .build(),
                        RemoveSupported.builder()                     // transformer does not exist
                                .withTransformerName("bad")
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        RemoveSupported.builder()                     // transform name not set
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        RemoveSupported.builder()                     // source type not set
                                .withTransformerName("1")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        RemoveSupported.builder()                     // target type not set
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
                        AddSupported.builder()                     // duplicates original
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
                        AddSupported.builder()                     // transformer does not exist
                                .withTransformerName("bad")
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        AddSupported.builder()                     // transform name not set
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        AddSupported.builder()                     // source type not set
                                .withTransformerName("1")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        AddSupported.builder()                     // target type not set
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
                        OverrideSupported.builder()                     // does not exist
                                .withTransformerName("1")
                                .withSourceMediaType("mimetype/c")
                                .withTargetMediaType("mimetype/d")
                                .build(),
                        OverrideSupported.builder()                     // size default -> 200 and priority default -> 100
                                .withTransformerName("1")
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/b")
                                .withPriority(40)
                                .build(),
                        OverrideSupported.builder()                     // size 100 -> 200 and change priority to default
                                .withTransformerName("1")
                                .withSourceMediaType("mimetype/x")
                                .withTargetMediaType("mimetype/y")
                                .withMaxSourceSizeBytes(200)
                                .build(),
                        OverrideSupported.builder()                     // transformer does not exist
                                .withTransformerName("bad")
                                .withSourceMediaType("mimetype/a")
                                .withTargetMediaType("mimetype/d")
                                .build()))
                        // OverrideSupported values with missing fields are defaults, so no test values here
                .build();

        String expectedWarnMessage = "Unable to process \"overrideSupported\": [" +
                "{\"transformerName\": \"1\", \"sourceMediaType\": \"mimetype/c\", \"targetMediaType\": \"mimetype/d\"}, " +
                "{\"transformerName\": \"bad\", \"sourceMediaType\": \"mimetype/a\", \"targetMediaType\": \"mimetype/d\"}]. " +
                "Read from readFromB";
        ImmutableSet<SupportedSourceAndTarget> expectedSupported = ImmutableSet.of(
                supported_X2Y_200,
                supported_A2B__40);
        String expectedToString = "[" +
                "{\"sourceMediaType\": \"mimetype/a\", \"targetMediaType\": \"mimetype/b\", \"priority\": \"40\"}, " +
                "{\"sourceMediaType\": \"mimetype/x\", \"targetMediaType\": \"mimetype/y\", \"maxSourceSizeBytes\": \"200\"}" +
                "]";

        addTransformConfig(secondConfig, expectedWarnMessage, expectedSupported, expectedToString);
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

        assertEquals("toString() difference", expectedToString, supportedSourceAndTargetList.toString());
    }
}
