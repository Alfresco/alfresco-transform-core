/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2022 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * -
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * -
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * -
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * -
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transform.base.http;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.alfresco.transform.base.html.OptionLister;
import org.alfresco.transform.config.TransformOption;
import org.alfresco.transform.config.TransformOptionGroup;
import org.alfresco.transform.config.TransformOptionValue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Used in the html test page.
 */
public class OptionListerTest
{
    OptionLister optionLister = new OptionLister();

    @Test
    public void emptyListTest()
    {
        Map<String, Set<TransformOption>> transformOptionsByName = Collections.emptyMap();

        assertEquals(Collections.emptySet(), optionLister.getOptionNames(transformOptionsByName));
    }

    @Test
    public void singleOptionNameWithSingleValue()
    {
        Map<String, Set<TransformOption>> transformOptionsByName = ImmutableMap.of("Dummy", ImmutableSet.of(
            new TransformOptionValue(true, "startPage")));

        assertEquals(ImmutableSet.of("startPage"), optionLister.getOptionNames(transformOptionsByName));
    }

    @Test
    public void whenOptionNameEndsInOptions_stripIt()
    {
        Map<String, Set<TransformOption>> transformOptionsByName = ImmutableMap.of("DummyOptions", ImmutableSet.of(
            new TransformOptionValue(true, "startPage")));

        assertEquals(ImmutableSet.of("startPage"), optionLister.getOptionNames(transformOptionsByName));
    }

    @Test
    public void singleOptionNameWithASingleRequiredValue()
    {
        Map<String, Set<TransformOption>> transformOptionsByName = ImmutableMap.of("DummyOptions", ImmutableSet.of(
            new TransformOptionValue(true, "startPage")));

        assertEquals(ImmutableSet.of("startPage"), optionLister.getOptionNames(transformOptionsByName));
    }

    @Test
    public void singleOptionNameWithACoupleOfValues()
    {
        Map<String, Set<TransformOption>> transformOptionsByName = ImmutableMap.of("DummyOptions", ImmutableSet.of(
            new TransformOptionValue(false, "startPage"),
            new TransformOptionValue(true, "endPage")));

        assertEquals(ImmutableSet.of("startPage", "endPage"), optionLister.getOptionNames(transformOptionsByName));
    }

    @Test
    public void sortedValues()
    {
        Map<String, Set<TransformOption>> transformOptionsByName = ImmutableMap.of("DummyOptions", ImmutableSet.of(
            new TransformOptionValue(false, "a"),
            new TransformOptionValue(false, "n"),
            new TransformOptionValue(false, "k"),
            new TransformOptionValue(false, "f"),
            new TransformOptionValue(true, "z")));

        assertEquals(ImmutableList.of("a", "f", "k", "n", "z"), new ArrayList<>(optionLister.getOptionNames(transformOptionsByName)));
    }

    @Test
    public void multipleOptionNames()
    {
        Map<String, Set<TransformOption>> transformOptionsByName = ImmutableMap.of("DummyOptions", ImmutableSet.of(
                new TransformOptionValue(false, "startPage"),
                new TransformOptionValue(true, "endPage")),
            "Another", ImmutableSet.of(
                new TransformOptionValue(false, "scale")),
            "YetAnother", ImmutableSet.of(
                new TransformOptionValue(false, "x"),
                new TransformOptionValue(false, "y"),
                new TransformOptionValue(true, "ratio"))
        );

        assertEquals(ImmutableSet.of(
                "startPage",
                "endPage",
                "scale",
                "x",
                "y",
                "ratio"),
            optionLister.getOptionNames(transformOptionsByName));
    }

    @Test
    public void multipleOptionNamesWithDuplicates()
    {
        Map<String, Set<TransformOption>> transformOptionsByName = ImmutableMap.of("DummyOptions", ImmutableSet.of(
                new TransformOptionValue(false, "startPage"),
                new TransformOptionValue(true, "endPage")),
            "Another", ImmutableSet.of(
                new TransformOptionValue(false, "scale")),
            "YetAnother", ImmutableSet.of(
                new TransformOptionValue(false, "x"),
                new TransformOptionValue(false, "y"),
                new TransformOptionValue(true, "scale"))
        );

        assertEquals(ImmutableSet.of(
                "startPage",
                "endPage",
                "scale",
                "x",
                "y"),
            optionLister.getOptionNames(transformOptionsByName));
    }

    @Test
    public void nestedGroups()
    {
        Map<String, Set<TransformOption>> transformOptionsByName = ImmutableMap.of("DummyOptions", ImmutableSet.of(
            new TransformOptionValue(false, "1"),
            new TransformOptionValue(true, "2"),
            new TransformOptionGroup(false, ImmutableSet.of(
                new TransformOptionValue(false, "3.1"),
                new TransformOptionValue(true, "3.2"),
                new TransformOptionValue(false, "3.3"))),
            new TransformOptionGroup(true, ImmutableSet.of(
                new TransformOptionValue(false, "4.1"),
                new TransformOptionGroup(false, ImmutableSet.of(
                    new TransformOptionValue(false, "4.2.1"),
                    new TransformOptionGroup(true, ImmutableSet.of(
                        new TransformOptionValue(false, "4.2.2.1"))),
                    new TransformOptionValue(true, "4.2.3"))),
                new TransformOptionValue(false, "4.3")))));

        assertEquals(ImmutableSet.of(
                "1",
                "2",
                "3.1",
                "3.2",
                "3.3",
                "4.1",
                "4.2.1",
                "4.2.2.1",
                "4.2.3",
                "4.3"),
            optionLister.getOptionNames(transformOptionsByName));
    }
}
