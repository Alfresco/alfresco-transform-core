/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2026 Alfresco Software Limited
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
package org.alfresco.transform.base.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Guards {@code transform-failure-classification-defaults.yaml}, which holds the lists {@link TransformExceptionClassifier} ships with.
 * <p/>
 * Moving them out of the class made them easy to correct, which is the point, and also easy to get wrong in ways the compiler no longer catches: a dropped key, an entry left blank, a list emptied. Each of those fails differently and none of them is loud, so they are checked here instead.
 * <p/>
 * What the entries <i>mean</i> is covered by {@code TransformExceptionClassifierTest} and, against the libraries that raise them, by {@code ClassifierLibraryPinningTest} in {@code engines/tika}.
 */
class ClassificationDefaultsTest
{
    static List<List<String>> shippedLists()
    {
        return List.of(
                ClassificationDefaults.CLASS_NAMES,
                ClassificationDefaults.MESSAGE_FRAGMENTS,
                ClassificationDefaults.MESSAGE_FRAGMENT_CLASS_NAMES,
                ClassificationDefaults.INFRASTRUCTURE_CLASS_NAMES,
                ClassificationDefaults.INFRASTRUCTURE_MESSAGE_FRAGMENTS);
    }

    @ParameterizedTest
    @MethodSource("shippedLists")
    void testEveryShippedListIsPopulated(List<String> shipped)
    {
        assertFalse(shipped.isEmpty(), "An empty list would match nothing, so every failure it covers would be "
                + "reported as 500 and retried for ever - the behaviour the classifier exists to correct.");
    }

    @ParameterizedTest
    @MethodSource("shippedLists")
    void testNoShippedEntryIsBlankOrUntrimmed(List<String> shipped)
    {
        List<String> malformed = shipped.stream()
                .filter(entry -> entry.isBlank() || !entry.equals(entry.trim()))
                .toList();

        assertEquals(List.of(), malformed, "A blank entry is contained by every message, so it would turn every "
                + "failure into a terminal 422; surrounding space would stop an entry matching at all.");
    }

    @ParameterizedTest
    @MethodSource("shippedLists")
    void testNoShippedEntryIsRepeated(List<String> shipped)
    {
        List<String> repeated = shipped.stream()
                .filter(entry -> Collections.frequency(shipped, entry) > 1)
                .distinct()
                .toList();

        assertEquals(List.of(), repeated, "Listed more than once.");
    }

    /**
     * The two absences the file explains at length, pinned so that editing it cannot quietly undo them.
     * <p/>
     * {@code UnsupportedEncodingException} names a charset that is missing from the JVM, and that name comes from the caller's {@code targetEncoding} option rather than the document, so listing it would make one client-side typo permanently discard every request that client sent. {@code TikaException} is what {@code CompositeParser} rethrows everything as, so listing it would make a disk-full or a socket error terminal.
     */
    @Test
    void testTheDeliberatelyUnlistedTypesAreStillUnlisted()
    {
        assertFalse(ClassificationDefaults.CLASS_NAMES.contains("UnsupportedEncodingException"));
        assertFalse(ClassificationDefaults.CLASS_NAMES.contains("TikaException"));
    }

    /**
     * {@code TikaException} is read for a message, which is only safe because the infrastructure veto has already examined the whole chain, but it must never be a content failure by name alone.
     */
    @Test
    void testTikaExceptionIsReadForAMessageRatherThanMatchedByName()
    {
        assertTrue(ClassificationDefaults.MESSAGE_FRAGMENT_CLASS_NAMES.contains("TikaException"));
    }

    @Test
    void testTheClassifierStartsFromTheShippedLists()
    {
        TransformExceptionClassifier classifier = new TransformExceptionClassifier();

        assertEquals(ClassificationDefaults.CLASS_NAMES, classifier.getClassNames());
        assertEquals(ClassificationDefaults.MESSAGE_FRAGMENTS, classifier.getMessageFragments());
        assertEquals(ClassificationDefaults.MESSAGE_FRAGMENT_CLASS_NAMES, classifier.getMessageFragmentClassNames());
        assertEquals(ClassificationDefaults.INFRASTRUCTURE_CLASS_NAMES, classifier.getInfrastructureClassNames());
        assertEquals(ClassificationDefaults.INFRASTRUCTURE_MESSAGE_FRAGMENTS,
                classifier.getInfrastructureMessageFragments());
    }

    @Test
    void testAMissingSectionIsRejectedRatherThanLeavingTheListsEmpty()
    {
        assertThrows(IllegalStateException.class,
                () -> ClassificationDefaults.section(Map.of("transform", Map.of("failure", Map.of()))));
    }

    @Test
    void testAMissingListIsRejected()
    {
        assertThrows(IllegalStateException.class, () -> ClassificationDefaults.list(Map.of(), "class-names"));
    }

    @Test
    void testAnEmptiedListIsRejected()
    {
        Map<String, Object> section = Map.of("class-names", List.of());

        assertThrows(IllegalStateException.class, () -> ClassificationDefaults.list(section, "class-names"));
    }

    @Test
    void testABlankEntryIsRejected()
    {
        Map<String, Object> section = Map.of("message-fragments", List.of("corrupt", "   "));

        assertThrows(IllegalStateException.class, () -> ClassificationDefaults.list(section, "message-fragments"));
    }

    /**
     * A YAML list of numbers or nested maps is a mistake, not an instruction to call {@code toString()} on them.
     */
    @Test
    void testANonTextEntryIsRejected()
    {
        Map<String, Object> section = Map.of("class-names", List.of(42));

        assertThrows(IllegalStateException.class, () -> ClassificationDefaults.list(section, "class-names"));
    }
}
