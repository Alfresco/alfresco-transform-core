/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.registry;

import com.google.common.collect.ImmutableMap;
import org.alfresco.transform.common.TransformException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_ENCODING;
import static org.alfresco.transform.common.RequestParamMap.TIMEOUT;
import static org.alfresco.transform.registry.TransformRegistryHelper.retrieveTransformListBySize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TransformRegistryHelperTest
{
    @Test
    public void testListBySize()
    {
        // This test was inspired by a failure to pick libreoffice over textToPdf despite the fact libreoffice has a
        // higher priority.
        SupportedTransform libreoffice = new SupportedTransform("libreoffice", emptySet(), -1, 50);
        SupportedTransform   textToPdf = new SupportedTransform("textToPdf",   emptySet(), 100,55);

        assertOrder(asList(libreoffice, textToPdf), asList(libreoffice));
        assertOrder(asList(textToPdf, libreoffice), asList(libreoffice));

        // * If multiple transforms with the same priority can support the same size, the one with the highest size
        //   limit (or no limit) is used.
        // * Transforms with a higher priority (lower numerically) are used up to their size limit in preference to
        //   lower priority transforms. These lower priority transforms will be used above that limit.
        // * If there are multiple transforms with the same priority and size limit, the last one defined is used to
        //   allow extensions to override standard transforms.
        // * In each of the above cases, it is possible for supplied transforms not to be returned from
        //   retrieveTransformListBySize as they will never be used. However this method is currently only used
        //   by (1) AbstractTransformRegistry.findTransformerName which filters out transformers that cannot support a
        //   given size and then uses the lowest element and (2) AbstractTransformRegistry.findMaxSize and gets the last
        //   element without filtering and returns its size limit. So there are opportunities to change the code so that
        //   it does not actually have to remove transformers that will not be used.

        // Test transforms
        SupportedTransform     p45  = new SupportedTransform(    "p45",  emptySet(),  -1, 45);
        SupportedTransform     p50  = new SupportedTransform(    "p50",  emptySet(),  -1, 50);
        SupportedTransform     p55  = new SupportedTransform(    "p55",  emptySet(),  -1, 55);
        SupportedTransform s100p45  = new SupportedTransform("s100p45",  emptySet(), 100, 45);
        SupportedTransform s100p50  = new SupportedTransform("s100p50",  emptySet(), 100, 50);
        SupportedTransform s100p55  = new SupportedTransform("s100p55",  emptySet(), 100, 55);
        SupportedTransform s200p50  = new SupportedTransform("s200p50",  emptySet(), 200, 50);
        SupportedTransform s200p50b = new SupportedTransform("s200p50b", emptySet(), 200, 50);
        SupportedTransform s200p55  = new SupportedTransform("s200p55",  emptySet(), 200, 55);
        SupportedTransform s300p45  = new SupportedTransform("s300p45",  emptySet(), 300, 45);
        SupportedTransform s300p50  = new SupportedTransform("s300p50",  emptySet(), 300, 50);
        SupportedTransform s300p55  = new SupportedTransform("s300p55",  emptySet(), 300, 55);

        // Just considers the priority
        assertOrder(asList(p50), asList(p50));
        assertOrder(asList(p45, p50), asList(p45));
        assertOrder(asList(p50, p55), asList(p50));
        assertOrder(asList(p50, p45), asList(p45));
        assertOrder(asList(p45, p50, p55), asList(p45));
        assertOrder(asList(p50, p55, p45), asList(p45));
        assertOrder(asList(p50, p45, p55), asList(p45));

        // Just considers the priority as the size limit is the same
        assertOrder(asList(s100p45, s100p50, s100p55), asList(s100p45));
        assertOrder(asList(s100p50, s100p45, s100p55), asList(s100p45));

        // Just considers size as the priority is the same
        assertOrder(asList(s100p50), asList(s100p50));
        assertOrder(asList(s100p50, s200p50), asList(s200p50));
        assertOrder(asList(s200p50, s100p50), asList(s200p50));
        assertOrder(asList(s100p50, s200p50, s300p50), asList(s300p50));
        assertOrder(asList(s200p50, s100p50, s300p50), asList(s300p50));
        assertOrder(asList(s300p50, s200p50, s100p50), asList(s300p50));

        // Just considers the order in which they were defined as the priority and size limit are the same.
        assertOrder(asList(s200p50, s200p50b), asList(s200p50b));
        assertOrder(asList(s200p50b, s200p50), asList(s200p50));

        // Combinations of priority and a size limit (always set)
        assertOrder(asList(s100p45, s100p50, s200p50, s200p55, s300p45, s300p50, s300p55), asList(s300p45));
        assertOrder(asList(s200p50, s300p55, s300p45, s100p45, s100p50, s300p50, s200p55), asList(s300p45));
        assertOrder(asList(s100p45, s200p50, s300p55), asList(s100p45, s200p50, s300p55));
        assertOrder(asList(s200p50, s100p45, s300p55), asList(s100p45, s200p50, s300p55));

        // Combinations of priority and a size limit or no size limit
        assertOrder(asList(p45, s100p50, s200p50, s300p55), asList(p45));
        assertOrder(asList(s100p50, s200p50, s300p55, p45), asList(p45));
        assertOrder(asList(p55, s100p50, s200p50, s300p55), asList(s200p50, p55));
        assertOrder(asList(p50, s100p50, s200p50, s300p55), asList(p50));
        assertOrder(asList(s100p50, s200p50, s300p55, p50), asList(p50));
    }

    private void assertOrder(List<SupportedTransform> transformsInLoadOrder, List<SupportedTransform> expectedList)
    {
        AtomicInteger transformerCount = new AtomicInteger(0);
        TransformCache data = new TransformCache();
        transformsInLoadOrder.forEach(t->data.appendTransform("text/plain", "application/pdf", t,
                "transformer"+transformerCount.getAndIncrement(), null));

        List<SupportedTransform> supportedTransforms = retrieveTransformListBySize(data,
                "text/plain", "application/pdf", null, null);

        // Check the values used.
        String transformerName = findTransformerName(supportedTransforms, 1);
        long maxSize = findMaxSize(supportedTransforms);
        String expectedTransformerName = expectedList.get(0).getName();
        long expectedMaxSourceSizeBytes = findMaxSize(expectedList);
        assertEquals(expectedList, supportedTransforms);
        assertEquals(expectedTransformerName, transformerName);
        assertEquals(expectedMaxSourceSizeBytes, maxSize);

        // If the above two pass, we don't really need the following one, but if it is wrong it might indicate
        // something is wrong, where the sourceSizeInBytes is not just 1.
        assertEquals(expectedList, supportedTransforms);
    }

    // Similar to the method in AbstractTransformRegistry
    private String findTransformerName(List<SupportedTransform> supportedTransforms, final long sourceSizeInBytes)
    {
        return supportedTransforms
                .stream()
                .filter(t -> t.getMaxSourceSizeBytes() == -1 ||
                        t.getMaxSourceSizeBytes() >= sourceSizeInBytes)
                .findFirst()
                .map(SupportedTransform::getName)
                .orElse(null);
    }

    // Similar to the method in AbstractTransformRegistry
    private long findMaxSize(List<SupportedTransform> supportedTransforms)
    {
        return supportedTransforms.isEmpty() ? 0 :
                supportedTransforms.get(supportedTransforms.size() - 1).getMaxSourceSizeBytes();
    }

    @Test
    public void buildTransformListSourceMimeTypeNullErrorTest()
    {
        TransformCache data = new TransformCache();

        assertThrows(TransformException.class, () ->
        {
            retrieveTransformListBySize(data, null, "application/pdf", null, null);
        });
    }

    @Test
    public void buildTransformListTargetMimeTypeNullErrorTest()
    {
        TransformCache data = new TransformCache();

        assertThrows(TransformException.class, () ->
        {
            retrieveTransformListBySize(data, "text/plain", null, null, null);
        });
    }

    @Test
    public void filterTimeoutTest()
    {
        // Almost identical to buildTransformListTargetMimeTypeNullErrorTest
        TransformCache data = new TransformCache();

        assertThrows(TransformException.class, () ->
        {
            retrieveTransformListBySize(data, "text/plain", null,
                new HashMap<>(ImmutableMap.of(TIMEOUT, "1234")), null);
        });
    }

    @Test
    public void filterSourceEncodingTest()
    {
        // Almost identical to buildTransformListTargetMimeTypeNullErrorTest
        TransformCache data = new TransformCache();

        assertThrows(TransformException.class, () ->
        {
            retrieveTransformListBySize(data, "text/plain", null,
                new HashMap<>(ImmutableMap.of(SOURCE_ENCODING, "UTF-8")), null);
        });
    }
}
