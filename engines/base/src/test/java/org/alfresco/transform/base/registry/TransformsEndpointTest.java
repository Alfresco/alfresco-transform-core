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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.alfresco.transform.base.fakes.FakeTransformEngineWithOneCustomTransformer;

@AutoConfigureMockMvc
@SpringBootTest(classes = {org.alfresco.transform.base.Application.class},
        properties = {"management.endpoints.web.exposure.include=health,transforms",
                // No broker in a unit test, so the JMS contributor would report DOWN and mask
                // what this test is actually about: that the registry does not affect health.
                "management.health.jms.enabled=false"})
public class TransformsEndpointTest
{
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TransformRegistry transformRegistry;
    @Autowired
    private TransformsEndpoint transformsEndpoint;
    @Autowired
    private List<TransformConfigSource> transformConfigSources;
    @Autowired
    private TransformConfigFromTransformEngines transformConfigFromTransformEngines;

    @AfterEach
    public void after()
    {
        transformConfigSources.clear();
        ReflectionTestUtils.setField(transformConfigFromTransformEngines, "transformEngines", Collections.emptyList());
        transformRegistry.retrieveConfig();
    }

    private void emptyRegistry()
    {
        transformConfigSources.clear();
        ReflectionTestUtils.setField(transformConfigFromTransformEngines, "transformEngines", Collections.emptyList());
        transformConfigFromTransformEngines.initTransformEngineConfig();
        transformRegistry.retrieveConfig();
    }

    private void populatedRegistry()
    {
        ReflectionTestUtils.setField(transformConfigFromTransformEngines, "transformEngines", ImmutableList.of(
                new FakeTransformEngineWithOneCustomTransformer()));
        transformConfigFromTransformEngines.initTransformEngineConfig();
        transformRegistry.retrieveConfig();
    }

    @Test
    public void reportsNotReadyWhileTheRegistryIsEmpty()
    {
        emptyRegistry();

        assertEquals(false, transformsEndpoint.transforms().get("ready"));
        assertEquals(0, transformsEndpoint.transforms().get("transformCount"));
    }

    @Test
    public void reportsReadyOnceTheRegistryIsPopulated()
    {
        populatedRegistry();

        assertEquals(true, transformsEndpoint.transforms().get("ready"));
        assertEquals(transformRegistry.getTransformCount(), transformsEndpoint.transforms().get("transformCount"));
        assertTrue((Integer) transformsEndpoint.transforms().get("transformCount") > 0);
    }

    @Test
    public void reportsNotReadyAgainIfTheRegistryIsEmptied()
    {
        populatedRegistry();
        assertEquals(true, transformsEndpoint.transforms().get("ready"));
        
        emptyRegistry();

        assertEquals(false, transformsEndpoint.transforms().get("ready"));
    }

    @Test
    public void isServedOverHttpAsJson() throws Exception
    {
        populatedRegistry();

        final MvcResult result = mockMvc.perform(get("/actuator/transforms")).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        final String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("\"ready\":true"), body);
        assertTrue(body.contains("\"transformCount\""), body);
    }
    
    @Test
    public void doesNotChangeWhatTheHealthEndpointsReport() throws Exception
    {
        emptyRegistry();
        assertFalse(transformRegistry.isReadyForTransformRequests());

        for (String endpoint : List.of("/actuator/health", "/actuator/health/readiness", "/actuator/health/liveness"))
        {
            final MvcResult result = mockMvc.perform(get(endpoint)).andReturn();
            assertEquals(200, result.getResponse().getStatus(),
                    endpoint + " must stay UP with an empty registry");
            assertTrue(result.getResponse().getContentAsString().contains("\"status\":\"UP\""),
                    endpoint + " -> " + result.getResponse().getContentAsString());
        }
    }
}
