/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2026 Alfresco Software Limited
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
package org.alfresco.transform.base.probes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.alfresco.transform.exceptions.TransformException;

class ProbeTransformTest
{
    private static final long DEFAULT_MAX_TRANSFORMS = 1024;
    private static final long DEFAULT_MAX_TRANSFORM_SECONDS = 120;

    private ProbeTransform createProbe(long maxTransforms, long maxTransformSeconds)
    {
        return createProbe(maxTransforms, maxTransformSeconds, name -> null);
    }

    private ProbeTransform createProbe(long maxTransforms, long maxTransformSeconds, UnaryOperator<String> envReader)
    {
        return new ProbeTransform("probe.html", "text/html", "text/plain", Map.of(),
                107, 30, 150, maxTransforms, maxTransformSeconds, 600, envReader);
    }

    private void setTransformCount(ProbeTransform probe, long count)
    {
        ((AtomicLong) ReflectionTestUtils.getField(probe, "transformCount")).set(count);
    }

    @Test
    void whenMaxTransformsIsZeroTransformCountCheckIsDisabled()
    {
        ProbeTransform probe = createProbe(0, DEFAULT_MAX_TRANSFORM_SECONDS);
        setTransformCount(probe, Long.MAX_VALUE);

        assertDoesNotThrow(() -> probe.doTransformOrNothing(true, null));
    }

    @Test
    void whenMaxTransformsIsPositiveTransformCountCheckIsEnforced()
    {
        ProbeTransform probe = createProbe(5, DEFAULT_MAX_TRANSFORM_SECONDS);
        setTransformCount(probe, 6);

        assertThrows(TransformException.class, () -> probe.doTransformOrNothing(true, null));
    }

    @Test
    void whenMaxTransformsEnvVarIsZeroItIsAcceptedAndDisablesCheck()
    {
        ProbeTransform probe = createProbe(DEFAULT_MAX_TRANSFORMS, DEFAULT_MAX_TRANSFORM_SECONDS,
                name -> "maxTransforms".equals(name) ? "0" : null);
        setTransformCount(probe, Long.MAX_VALUE);

        assertDoesNotThrow(() -> probe.doTransformOrNothing(true, null));
    }

    @Test
    void whenMaxTransformsEnvVarIsNegativeItFallsBackToDefault()
    {
        // env var "-1" is invalid, so default (5) is used and the check is enforced
        ProbeTransform probe = createProbe(5, DEFAULT_MAX_TRANSFORM_SECONDS,
                name -> "maxTransforms".equals(name) ? "-1" : null);
        setTransformCount(probe, 6);

        assertThrows(TransformException.class, () -> probe.doTransformOrNothing(true, null));
    }

    @Test
    void whenMaxTransformSecondsIsZeroTimeCheckIsDisabled()
    {
        ProbeTransform probe = createProbe(DEFAULT_MAX_TRANSFORMS, 0);
        probe.recordTransformTime(Long.MAX_VALUE);

        assertDoesNotThrow(() -> probe.doTransformOrNothing(true, null));
    }

    @Test
    void whenMaxTransformSecondsIsPositiveTimeCheckIsEnforced()
    {
        ProbeTransform probe = createProbe(DEFAULT_MAX_TRANSFORMS, 1);
        probe.recordTransformTime(2000);

        assertThrows(TransformException.class, () -> probe.doTransformOrNothing(true, null));
    }

    @Test
    void whenMaxTransformSecondsEnvVarIsZeroItIsAcceptedAndDisablesCheck()
    {
        ProbeTransform probe = createProbe(DEFAULT_MAX_TRANSFORMS, DEFAULT_MAX_TRANSFORM_SECONDS,
                name -> "maxTransformSeconds".equals(name) ? "0" : null);
        probe.recordTransformTime(Long.MAX_VALUE);

        assertDoesNotThrow(() -> probe.doTransformOrNothing(true, null));
    }

    @Test
    void whenMaxTransformSecondsEnvVarIsNegativeItFallsBackToDefault()
    {
        // env var "-1" is invalid, so default (1 second) is used and the check is enforced
        ProbeTransform probe = createProbe(DEFAULT_MAX_TRANSFORMS, 1,
                name -> "maxTransformSeconds".equals(name) ? "-1" : null);
        probe.recordTransformTime(2000);

        assertThrows(TransformException.class, () -> probe.doTransformOrNothing(true, null));
    }
}
