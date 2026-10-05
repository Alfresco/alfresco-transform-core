/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2026 Alfresco Software Limited
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
package org.alfresco.transform.base.registry;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.alfresco.transform.base.fakes.FakeTransformEngineWithOneCustomTransformer;
import org.alfresco.transform.config.TransformConfig;

public class TransformRegistryInitialLoadTest
{
    private TransformRegistry transformRegistry;

    @BeforeEach
    public void before()
    {
        TransformConfigSource source = new AbstractTransformConfigSource("0010 Fake", "0010 Fake", "---") {
            @Override
            public TransformConfig getTransformConfig()
            {
                return new FakeTransformEngineWithOneCustomTransformer().getTransformConfig();
            }
        };
        transformRegistry = new TransformRegistry();
        ReflectionTestUtils.setField(transformRegistry, "coreVersion", "5.1.0");
        ReflectionTestUtils.setField(transformRegistry, "transformConfigSources", List.of(source));
        ReflectionTestUtils.setField(transformRegistry, "initialConfigWaitSeconds", 10L);
    }

    @Test
    public void aRequestBeforeTheInitialLoadWaitsForIt() throws Exception
    {
        CompletableFuture<String> request = CompletableFuture.supplyAsync(
                () -> transformRegistry.findTransformerName(MIMETYPE_PDF, 0, MIMETYPE_IMAGE_JPEG, emptyMap(), null));

        assertThrows(TimeoutException.class, () -> request.get(200, MILLISECONDS));

        transformRegistry.retrieveConfig();

        assertEquals("Pdf2Jpg", request.get(5, SECONDS));
    }

    @Test
    public void theConfigRequestAlsoWaitsForTheInitialLoad() throws Exception
    {
        CompletableFuture<Integer> request = CompletableFuture.supplyAsync(
                () -> transformRegistry.getTransformConfig().getTransformers().size());

        assertThrows(TimeoutException.class, () -> request.get(200, MILLISECONDS));

        transformRegistry.retrieveConfig();

        assertEquals(1, request.get(5, SECONDS));
    }

    @Test
    public void theWaitForTheInitialLoadIsBounded()
    {
        ReflectionTestUtils.setField(transformRegistry, "initialConfigWaitSeconds", 0L);

        assertNull(transformRegistry.findTransformerName(MIMETYPE_PDF, 0, MIMETYPE_IMAGE_JPEG, emptyMap(), null));
    }

    @Test
    public void requestsAfterTheInitialLoadDoNotWait()
    {
        transformRegistry.retrieveConfig();

        assertEquals("Pdf2Jpg",
                transformRegistry.findTransformerName(MIMETYPE_PDF, 0, MIMETYPE_IMAGE_JPEG, emptyMap(), null));
        assertEquals("Pdf2Jpg",
                transformRegistry.getTransformer(MIMETYPE_PDF, 0L, MIMETYPE_IMAGE_JPEG, emptyMap()).getTransformerName());
    }
}
