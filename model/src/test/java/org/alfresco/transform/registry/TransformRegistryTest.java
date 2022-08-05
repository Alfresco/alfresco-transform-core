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
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.alfresco.transform.registry.TransformRegistryHelper.addToPossibleTransformOptions;
import static org.alfresco.transform.registry.TransformRegistryHelper.optionsMatch;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.transform.config.SupportedSourceAndTarget;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.TransformOption;
import org.alfresco.transform.config.TransformOptionGroup;
import org.alfresco.transform.config.TransformOptionValue;
import org.alfresco.transform.config.Transformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Test the AbstractTransformRegistry, extended by both T-Engines and ACS repository, which need to
 * read JSON config to understand what is supported.
 *
 * @author adavis
 */
public class TransformRegistryTest
{
    protected static final String GIF = "image/gif";
    protected static final String JPEG = "image/jpeg";
    protected static final String PDF = "application/pdf";
    protected static final String DOC = "application/msword";
    protected static final String XLS = "application/vnd.ms-excel";
    protected static final String PPT = "application/vnd.ms-powerpoint";
    protected static final String MSG = "application/vnd.ms-outlook";
    protected static final String TXT = "text/plain";

    protected AbstractTransformRegistry registry;
    protected Map<String, Set<TransformOption>> mapOfTransformOptions;

    @BeforeEach
    public void setUp() throws Exception
    {
        registry = buildTransformServiceRegistryImpl();
        mapOfTransformOptions = new HashMap<>();
    }

    protected AbstractTransformRegistry buildTransformServiceRegistryImpl() throws Exception
    {
        return new AbstractTransformRegistry()
        {
            private TransformCache data = new TransformCache();

            @Override
            protected void logError(String msg)
            {
                System.out.println(msg);
            }

            @Override
            protected void logWarn(String msg)
            {
                System.out.println(msg);
            }

            @Override
            public TransformCache getData()
            {
                return data;
            }
        };
    }

    private void assertAddToPossibleOptions(final TransformOptionGroup transformOptionGroup,
        final Set<String> actualOptionNames, final Set<String> expectedNameSet,
        final Set<String> expectedRequiredSet)
    {
        final Map<String, Boolean> possibleTransformOptions = new HashMap<>();

        addToPossibleTransformOptions(possibleTransformOptions, transformOptionGroup, true,
            buildActualOptions(actualOptionNames));

        assertEquals(expectedNameSet, possibleTransformOptions.keySet());

        possibleTransformOptions.forEach((name, required) -> {
            if (required)
            {
                assertTrue(expectedRequiredSet.contains(name));
            }
            else
            {
                assertFalse(expectedRequiredSet.contains(name));
            }
        });
    }

    // transformOptionNames are upper case if required.
    private void assertIsSupported(final Set<String> actualOptionNames,
        final Set<String> transformOptionNames, final String unsupportedMsg)
    {
        final Map<String, Boolean> transformOptions = transformOptionNames
            .stream()
            .collect(toMap(identity(), name -> name.toUpperCase().equals(name)));

        boolean supported = optionsMatch(transformOptions, buildActualOptions(actualOptionNames));
        if (isBlank(unsupportedMsg))
        {
            assertTrue(supported);
        }
        else
        {
            assertFalse(supported);
        }
    }

    private void assertTransformOptions(Set<TransformOption> setOfTransformOptions) throws Exception
    {
        final Transformer transformer = new Transformer("name", singleton("testOptions"), set(
            SupportedSourceAndTarget.builder()
                .withSourceMediaType(DOC)
                .withTargetMediaType(TXT)
                .build(),
            SupportedSourceAndTarget.builder()
                .withSourceMediaType(XLS)
                .withTargetMediaType(TXT)
                .withMaxSourceSizeBytes(1024000L)
                .build()));
        final TransformConfig transformConfig = TransformConfig
            .builder()
            .withTransformers(singletonList(transformer))
            .withTransformOptions(singletonMap("testOptions", setOfTransformOptions))
            .build();

        registry = buildTransformServiceRegistryImpl();
        CombinedTransformConfig.combineAndRegister(transformConfig, getClass().getName(), getBaseUrl(transformer), registry);

        assertTrue(registry.isSupported(XLS, 1024, TXT, emptyMap(), null));
        assertTrue(registry.isSupported(XLS, 1024000, TXT, null, null));
        assertFalse(registry.isSupported(XLS, 1024001, TXT, emptyMap(), null));
        assertTrue(registry.isSupported(DOC, 1024001, TXT, null, null));
    }

    protected String getBaseUrl(Transformer transformer)
    {
        return "xxx";
    }

    private void assertTransformerName(String sourceMimetype, long sourceSizeInBytes,
        String targetMimetype, Map<String, String> actualOptions, String expectedTransformerName,
        Transformer... transformers) throws Exception
    {
        buildAndPopulateRegistry(transformers);
        String transformerName = registry.findTransformerName(sourceMimetype, sourceSizeInBytes,
            targetMimetype, actualOptions, null);
        assertEquals(expectedTransformerName, transformerName);
    }

    private void assertSupported(final Transformer transformer, final String sourceMimetype,
        final long sourceSizeInBytes, final String targetMimetype,
        final Map<String, String> actualOptions, final String unsupportedMsg) throws Exception
    {
        assertSupported(sourceMimetype, sourceSizeInBytes, targetMimetype, actualOptions,
            unsupportedMsg, transformer);
    }

    private void assertSupported(String sourceMimetype, long sourceSizeInBytes,
        String targetMimetype, Map<String, String> actualOptions, String unsupportedMsg,
        Transformer... transformers) throws Exception
    {
        buildAndPopulateRegistry(transformers);
        assertSupported(sourceMimetype, sourceSizeInBytes, targetMimetype, actualOptions, null,
            unsupportedMsg);
    }

    private void buildAndPopulateRegistry(Transformer[] transformers) throws Exception
    {
        registry = buildTransformServiceRegistryImpl();
        TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(Arrays.asList(transformers))
                .withTransformOptions(mapOfTransformOptions)
                .build();
        CombinedTransformConfig.combineAndRegister(transformConfig, getClass().getName(), "---", registry);
    }

    protected void assertSupported(String sourceMimetype, long sourceSizeInBytes,
        String targetMimetype, Map<String, String> actualOptions, String renditionName,
        String unsupportedMsg)
    {
        boolean supported = registry.isSupported(sourceMimetype, sourceSizeInBytes, targetMimetype,
            actualOptions, renditionName);
        if (unsupportedMsg == null || unsupportedMsg.isEmpty())
        {
            assertTrue(supported);
        }
        else
        {
            assertFalse(supported);
        }
    }

    private static Map<String, String> buildActualOptions(final Set<String> optionNames)
    {
        return optionNames
            .stream()
            .collect(toMap(identity(), name -> "value for " + name));
    }

    @Test
    public void testOptionalGroups()
    {
        final TransformOptionGroup transformOptionGroup =
            new TransformOptionGroup(true, set(
                new TransformOptionValue(false, "1"),
                new TransformOptionValue(true, "2"),
                new TransformOptionGroup(false, set(
                    new TransformOptionValue(false, "3.1"),
                    new TransformOptionValue(false, "3.2"),
                    new TransformOptionValue(false, "3.3"))),
                new TransformOptionGroup(false, set( // OPTIONAL
                    new TransformOptionValue(false, "4.1"),
                    new TransformOptionValue(true, "4.2"),
                    new TransformOptionValue(false, "4.3")))));

        assertAddToPossibleOptions(transformOptionGroup, emptySet(),
            set("1", "2"), set("2"));
        assertAddToPossibleOptions(transformOptionGroup, set("1"),
            set("1", "2"), set("2"));
        assertAddToPossibleOptions(transformOptionGroup, set("2"),
            set("1", "2"), set("2"));
        assertAddToPossibleOptions(transformOptionGroup, set("2", "3.2"),
            set("1", "2", "3.1", "3.2", "3.3"), set("2"));
        assertAddToPossibleOptions(transformOptionGroup, set("2", "4.1"),
            set("1", "2", "4.1", "4.2", "4.3"), set("2", "4.2"));
        assertAddToPossibleOptions(transformOptionGroup, set("2", "4.2"),
            set("1", "2", "4.1", "4.2", "4.3"), set("2", "4.2"));
    }

    @Test
    public void testRequiredGroup()
    {
        TransformOptionGroup transformOptionGroup =
            new TransformOptionGroup(true, set(
                new TransformOptionValue(false, "1"),
                new TransformOptionValue(true, "2"),
                new TransformOptionGroup(false, set(
                    new TransformOptionValue(false, "3.1"),
                    new TransformOptionValue(false, "3.2"),
                    new TransformOptionValue(false, "3.3"))),
                new TransformOptionGroup(true, set(
                    new TransformOptionValue(false, "4.1"),
                    new TransformOptionValue(true, "4.2"),
                    new TransformOptionValue(false, "4.3")))));

        assertAddToPossibleOptions(transformOptionGroup, emptySet(),
            set("1", "2", "4.1", "4.2", "4.3"), set("2", "4.2"));
        assertAddToPossibleOptions(transformOptionGroup, set("1"),
            set("1", "2", "4.1", "4.2", "4.3"), set("2", "4.2"));
        assertAddToPossibleOptions(transformOptionGroup, set("2", "3.2"),
            set("1", "2", "3.1", "3.2", "3.3", "4.1", "4.2", "4.3"), set("2", "4.2"));
        assertAddToPossibleOptions(transformOptionGroup, set("2", "4.1"),
            set("1", "2", "4.1", "4.2", "4.3"), set("2", "4.2"));
        assertAddToPossibleOptions(transformOptionGroup, set("2", "4.2"),
            set("1", "2", "4.1", "4.2", "4.3"), set("2", "4.2"));
    }

    @Test
    public void testNestedGroups()
    {
        TransformOptionGroup transformOptionGroup =
            new TransformOptionGroup(false, set(
                new TransformOptionGroup(false, set(
                    new TransformOptionValue(false, "1"),
                    new TransformOptionGroup(false, set(
                        new TransformOptionValue(false, "1.2"),
                        new TransformOptionGroup(false, set(
                            new TransformOptionValue(false, "1.2.3"))))))),
                new TransformOptionGroup(false, set(
                    new TransformOptionValue(false, "2"),
                    new TransformOptionGroup(false, set(
                        new TransformOptionValue(false, "2.2"),
                        new TransformOptionGroup(false, set(
                            new TransformOptionGroup(false, set(
                                new TransformOptionValue(false, "2.2.1.2"))))))))),
                new TransformOptionGroup(false, set(
                    new TransformOptionValue(true, "3"),
                    new TransformOptionGroup(false, set(
                        new TransformOptionGroup(false, set(
                            new TransformOptionGroup(false, set(
                                new TransformOptionValue(false, "3.1.1.2"))))))))),
                new TransformOptionGroup(false, set(
                    new TransformOptionValue(false, "4"),
                    new TransformOptionGroup(true, set(
                        new TransformOptionGroup(false, set(
                            new TransformOptionGroup(false, set(
                                new TransformOptionValue(false, "4.1.1.2"))))))))),
                new TransformOptionGroup(false, set(
                    new TransformOptionValue(false, "5"),
                    new TransformOptionGroup(false, set(
                        new TransformOptionGroup(true, set(
                            new TransformOptionGroup(false, set(
                                new TransformOptionValue(false, "5.1.1.2"))))))))),
                new TransformOptionGroup(false, set(
                    new TransformOptionValue(false, "6"),
                    new TransformOptionGroup(false, set(
                        new TransformOptionGroup(false, set(
                            new TransformOptionGroup(true, set(
                                new TransformOptionValue(false, "6.1.1.2"))))))))),
                new TransformOptionGroup(false, set(
                    new TransformOptionValue(false, "7"),
                    new TransformOptionGroup(false, set(
                        new TransformOptionGroup(false, set(
                            new TransformOptionGroup(false, set(
                                new TransformOptionValue(true, "7.1.1.2")))))))))
            ));

        assertAddToPossibleOptions(transformOptionGroup, emptySet(),
            emptySet(), emptySet());
        assertAddToPossibleOptions(transformOptionGroup, set("1"),
            set("1"), emptySet());
        assertAddToPossibleOptions(transformOptionGroup, set("1", "7"),
            set("1", "7"), emptySet());
        assertAddToPossibleOptions(transformOptionGroup, set("1", "7.1.1.2"),
            set("1", "7", "7.1.1.2"), set("7.1.1.2"));
        assertAddToPossibleOptions(transformOptionGroup, set("1", "6"),
            set("1", "6"), emptySet());
        assertAddToPossibleOptions(transformOptionGroup, set("1", "6.1.1.2"),
            set("1", "6", "6.1.1.2"), emptySet());
        assertAddToPossibleOptions(transformOptionGroup, set("1", "5"),
            set("1", "5"), emptySet());
        assertAddToPossibleOptions(transformOptionGroup, set("1", "5.1.1.2"),
            set("1", "5", "5.1.1.2"), emptySet());
        assertAddToPossibleOptions(transformOptionGroup, set("1", "4"),
            set("1", "4"), emptySet());
        assertAddToPossibleOptions(transformOptionGroup, set("1", "4.1.1.2"),
            set("1", "4", "4.1.1.2"), emptySet());
        assertAddToPossibleOptions(transformOptionGroup, set("1", "3"),
            set("1", "3"), set("3"));
        assertAddToPossibleOptions(transformOptionGroup, set("1", "3.1.1.2"),
            set("1", "3", "3.1.1.2"), set("3"));

        assertAddToPossibleOptions(transformOptionGroup, set("2"),
            set("2"), emptySet());
        assertAddToPossibleOptions(transformOptionGroup, set("2", "2.2"),
            set("2", "2.2"), emptySet());
        assertAddToPossibleOptions(transformOptionGroup, set("3"),
            set("3"), set("3"));
        assertAddToPossibleOptions(transformOptionGroup, set("3.1.1.2"),
            set("3", "3.1.1.2"), set("3"));
    }

    @Test
    public void testRegistryIsSupportedMethod()
    {
        assertIsSupported(set("a"), set("a", "B", "c"), "required option B is missing");
        assertIsSupported(emptySet(), set("a", "B", "c"), "required option B is missing");
        assertIsSupported(set("B"), set("a", "B", "c"), null);
        assertIsSupported(set("B", "c"), set("a", "B", "c"), null);
        assertIsSupported(set("B", "a", "c"), set("a", "B", "c"), null);

        assertIsSupported(set("B", "d"), set("a", "B", "c"), "there is an extra option d");
        assertIsSupported(set("B", "c", "d"), set("a", "B", "c"), "there is an extra option d");
        assertIsSupported(set("d"), set("a", "B", "c"),
            "required option B is missing and there is an extra option d");

        assertIsSupported(set("a"), set("a", "b", "c"), null);
        assertIsSupported(emptySet(), set("a", "b", "c"), null);
        assertIsSupported(set("a", "b", "c"), set("a", "b", "c"), null);
    }

    @Test
    public void testNoActualOptions() throws Exception
    {
        assertTransformOptions(set(
            new TransformOptionValue(false, "option1"),
            new TransformOptionValue(false, "option2")));
    }

    @Test
    public void testNoTransformOptions() throws Exception
    {
        assertTransformOptions(emptySet());
        assertTransformOptions(null);
    }

    @Test
    public void testSupported() throws Exception
    {
        mapOfTransformOptions.put("options1", set(
            new TransformOptionValue(false, "page"),
            new TransformOptionValue(false, "width"),
            new TransformOptionValue(false, "height")));
        final Transformer transformer = new Transformer("name", singleton("options1"), set(
            SupportedSourceAndTarget.builder()
                .withSourceMediaType(DOC)
                .withTargetMediaType(GIF)
                .withMaxSourceSizeBytes(102400L)
                .build(),
            SupportedSourceAndTarget.builder()
                .withSourceMediaType(DOC)
                .withTargetMediaType(JPEG)
                .build(),
            SupportedSourceAndTarget.builder()
                .withSourceMediaType(MSG)
                .withTargetMediaType(GIF)
                .build()));

        assertSupported(transformer, DOC, 1024, GIF, emptyMap(), null);
        assertSupported(transformer, DOC, 102400, GIF, emptyMap(), null);
        assertSupported(transformer, DOC, 102401, GIF, emptyMap(), "source is too large");
        assertSupported(transformer, DOC, 1024, JPEG, emptyMap(), null);
        assertSupported(transformer, GIF, 1024, DOC, emptyMap(),
            GIF + " is not a source of this transformer");
        assertSupported(transformer, MSG, 1024, GIF, emptyMap(), null);
        assertSupported(transformer, MSG, 1024, JPEG, emptyMap(),
            MSG + " to " + JPEG + " is not supported by this transformer");

        assertSupported(transformer, DOC, 1024, GIF, buildActualOptions(set("page", "width")),
            null);
        assertSupported(transformer, DOC, 1024, GIF,
            buildActualOptions(set("page", "width", "startPage")), "startPage is not an option");
    }

    @Test
    // renditionName used as the cache key, is an alias for a set of actualOptions and the target mimetype.
    // The source mimetype may change.
    public void testCache()
    {
        mapOfTransformOptions.put("options1", set(
            new TransformOptionValue(false, "page"),
            new TransformOptionValue(false, "width"),
            new TransformOptionValue(false, "height")));

        final Transformer transformer = new Transformer("name", singleton("options1"), set(
            SupportedSourceAndTarget.builder()
                .withSourceMediaType(DOC)
                .withTargetMediaType(GIF)
                .withMaxSourceSizeBytes(102400L)
                .build(),
            SupportedSourceAndTarget.builder()
                .withSourceMediaType(MSG)
                .withTargetMediaType(GIF)
                .build()));

        TransformConfig transformConfig = TransformConfig.builder()
                .withTransformers(Collections.singletonList(transformer))
                .withTransformOptions(mapOfTransformOptions)
                .build();
        CombinedTransformConfig.combineAndRegister(transformConfig, getClass().getName(), getBaseUrl(transformer), registry);

        assertSupported(DOC, 1024, GIF, emptyMap(), "doclib", "");
        assertSupported(MSG, 1024, GIF, emptyMap(), "doclib", "");

        assertEquals(102400L, registry.findMaxSize(DOC, GIF, emptyMap(), "doclib"));
        assertEquals(-1L, registry.findMaxSize(MSG, GIF, emptyMap(), "doclib"));

        // check we are now using the cached value.
        final SupportedTransform cachedSupportedTransform = new SupportedTransform("name1",
            emptySet(), 999999L, 0);

        registry.getData()
                .retrieveCached("doclib", DOC)
                .add(cachedSupportedTransform);
        assertEquals(999999L, registry.findMaxSize(DOC, GIF, emptyMap(), "doclib"));
    }

    @Test
    public void testGetTransformerName() throws Exception
    {
        Transformer t1 = newTransformer("transformer1", MSG, GIF, 100, 50);
        Transformer t2 = newTransformer("transformer2", MSG, GIF, 200, 60);
        Transformer t3 = newTransformer("transformer3", MSG, GIF, 200, 40);
        Transformer t4 = newTransformer("transformer4", MSG, GIF, -1, 100);
        Transformer t5 = newTransformer("transformer5", MSG, GIF, -1, 80);

        // Select on size - priority is ignored
        assertTransformerName(MSG, 100, GIF, emptyMap(), "transformer1", t1, t2);
        assertTransformerName(MSG, 150, GIF, emptyMap(), "transformer2", t1, t2);
        assertTransformerName(MSG, 250, GIF, emptyMap(), null, t1, t2);
        // Select on priority - t1, t2 and t4 are discarded.
        //                      t3 is a higher priority and has a larger size than t1 and t2.
        //                      Similar story fo t4 with t5.
        assertTransformerName(MSG, 100, GIF, emptyMap(), "transformer3", t1, t2, t3, t4, t5);
        assertTransformerName(MSG, 200, GIF, emptyMap(), "transformer3", t1, t2, t3, t4, t5);
        // Select on size and priority, t1 and t2 discarded
        assertTransformerName(MSG, 200, GIF, emptyMap(), "transformer3", t1, t2, t3, t4);
        assertTransformerName(MSG, 300, GIF, emptyMap(), "transformer4", t1, t2, t3, t4);
        assertTransformerName(MSG, 300, GIF, emptyMap(), "transformer5", t1, t2, t3, t4, t5);
    }

    private Transformer newTransformer(String transformerName, String sourceMediaType, String targetMediaType,
                                       long maxSourceSizeBytes, int priority)
    {
        return Transformer.builder().withTransformerName(transformerName)
                .withSupportedSourceAndTargetList(ImmutableSet.of(
                        SupportedSourceAndTarget.builder()
                                .withSourceMediaType(sourceMediaType)
                                .withTargetMediaType(targetMediaType)
                                .withMaxSourceSizeBytes(maxSourceSizeBytes)
                                .withPriority(priority)
                                .build()))
                .build();
    }

    @Test
    public void testMultipleTransformers() throws Exception
    {
        mapOfTransformOptions.put("options1", set(
            new TransformOptionValue(false, "page"),
            new TransformOptionValue(false, "width"),
            new TransformOptionValue(false, "height")));
        mapOfTransformOptions.put("options2", set(
            new TransformOptionValue(false, "opt1"),
            new TransformOptionValue(false, "opt2")));
        mapOfTransformOptions.put("options3", new HashSet<>(singletonList(
            new TransformOptionValue(false, "opt1"))));

        Transformer transformer1 = new Transformer("transformer1", singleton("options1"), set(
            SupportedSourceAndTarget.builder()
                .withSourceMediaType(DOC)
                .withTargetMediaType(GIF)
                .withMaxSourceSizeBytes(102400L)
                .build(),
            SupportedSourceAndTarget.builder()
                .withSourceMediaType(DOC)
                .withTargetMediaType(JPEG)
                .build(),
            SupportedSourceAndTarget.builder()
                .withSourceMediaType(MSG)
                .withTargetMediaType(GIF)
                .build()));

        Transformer transformer2 = new Transformer("transformer2", singleton("options2"), set(
            SupportedSourceAndTarget.builder()
                .withSourceMediaType(PDF)
                .withTargetMediaType(GIF)
                .build(),
            SupportedSourceAndTarget.builder()
                .withSourceMediaType(PPT)
                .withTargetMediaType(JPEG)
                .build()));

        Transformer transformer3 = new Transformer("transformer3", singleton("options3"),
            new HashSet(singletonList(SupportedSourceAndTarget.builder()
                    .withSourceMediaType(DOC)
                    .withTargetMediaType(GIF)
                    .build())));

        assertSupported(DOC, 1024, GIF, emptyMap(), null, transformer1);
        assertSupported(DOC, 1024, GIF, emptyMap(), null, transformer1, transformer2);
        assertSupported(DOC, 1024, GIF, emptyMap(), null, transformer1, transformer2,
            transformer3);

        assertSupported(DOC, 102401, GIF, emptyMap(), "source is too large", transformer1);
        assertSupported(DOC, 102401, GIF, emptyMap(), null, transformer1, transformer3);

        assertSupported(PDF, 1024, GIF, emptyMap(), "Only transformer2 supports these mimetypes",
            transformer1);
        assertSupported(PDF, 1024, GIF, emptyMap(), null, transformer1, transformer2);
        assertSupported(PDF, 1024, GIF, emptyMap(), null, transformer1, transformer2,
            transformer3);

        final Map<String, String> actualOptions = buildActualOptions(set("opt1"));
        assertSupported(PDF, 1024, GIF, actualOptions, "Only transformer2/4 supports these options",
            transformer1);
        assertSupported(PDF, 1024, GIF, actualOptions, null, transformer1, transformer2);
        assertSupported(PDF, 1024, GIF, actualOptions, null, transformer1, transformer2,
            transformer3);
        assertSupported(PDF, 1024, GIF, actualOptions,
            "transformer4 supports opt1 but not the source mimetype ", transformer1, transformer3);
    }

    @SafeVarargs
    private static <T> Set<T> set(T... elements)
    {
        if (elements == null || elements.length == 0)
        {
            return emptySet();
        }
        return ImmutableSet.copyOf(elements);
    }
}
