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
package org.alfresco.transform.base.registry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.alfresco.transform.base.fakes.FakeTransformEngineWithAllInOne;
import org.alfresco.transform.base.fakes.FakeTransformEngineWithOneCustomTransformer;
import org.alfresco.transform.base.fakes.FakeTransformEngineWithTwoCustomTransformers;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.Transformer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AutoConfigureMockMvc
@SpringBootTest(classes={org.alfresco.transform.base.Application.class})
public class TransformRegistryTest
{
    @Autowired
    private TransformRegistry transformRegistry;
    @Autowired
    private List<TransformConfigSource> transformConfigSources;
    @Autowired
    private TransformConfigFromTransformEngines transformConfigFromTransformEngines;
    @Autowired
    private TransformConfigFromFiles transformConfigFromFiles;
    @Autowired
    private TransformConfigFiles transformConfigFiles;
    @Autowired
    private TransformConfigFilesHistoric transformConfigFilesHistoric;

    @AfterEach
    private void after()
    {
        transformConfigSources.clear();
        ReflectionTestUtils.setField(transformConfigFromTransformEngines, "transformEngines", Collections.emptyList());
        ReflectionTestUtils.setField(transformConfigFiles, "files", Collections.emptyMap());
        ReflectionTestUtils.setField(transformConfigFilesHistoric, "additional", Collections.emptyMap());
        ReflectionTestUtils.setField(transformRegistry, "isTRouter", false);
        transformRegistry.retrieveConfig();
    }

    private String getTransformerNames(TransformConfig transformConfig)
    {
        return transformConfig.getTransformers().stream()
              .map(Transformer::getTransformerName)
              .sorted()
              .collect(Collectors.joining(", "));
    }

    @Test
    public void noConfig()
    {
        assertEquals("", getTransformerNames(transformRegistry.getTransformConfig()));
    }

    @Test
    public void singleTransformEngine()
    {
        ReflectionTestUtils.setField(transformConfigFromTransformEngines, "transformEngines", ImmutableList.of(
            new FakeTransformEngineWithOneCustomTransformer()));
        transformConfigFromTransformEngines.initTransformEngineConfig();
        transformRegistry.retrieveConfig();

        assertEquals("Pdf2Jpg", getTransformerNames(transformRegistry.getTransformConfig()));
    }

    @Test
    public void multipleTransformEngines()
    {
        ReflectionTestUtils.setField(transformConfigFromTransformEngines, "transformEngines", ImmutableList.of(
            new FakeTransformEngineWithAllInOne(),
            new FakeTransformEngineWithOneCustomTransformer(),
            new FakeTransformEngineWithTwoCustomTransformers()));
        transformConfigFromTransformEngines.initTransformEngineConfig();
        transformRegistry.retrieveConfig();

        assertEquals("Pdf2Jpg, Pdf2Png, TxT2Pdf, Txt2JpgViaPdf, Txt2PngViaPdf",
            getTransformerNames(transformRegistry.getTransformConfig()));
    }

    @Test
    public void uncombinedConfigFromEngine()
    {
        ReflectionTestUtils.setField(transformConfigFromTransformEngines, "transformEngines", ImmutableList.of(
            new FakeTransformEngineWithAllInOne(),
            new FakeTransformEngineWithTwoCustomTransformers()));
        transformConfigFromTransformEngines.initTransformEngineConfig();
        transformRegistry.retrieveConfig();

        assertEquals("Pdf2Png, TxT2Pdf, Txt2JpgViaPdf, Txt2PngViaPdf",
            getTransformerNames(transformRegistry.getTransformConfig()));

        ReflectionTestUtils.setField(transformRegistry, "isTRouter", true);
        transformConfigFromTransformEngines.initTransformEngineConfig();
        transformRegistry.retrieveConfig();

        assertEquals("Pdf2Png, TxT2Pdf, Txt2PngViaPdf",
            getTransformerNames(transformRegistry.getTransformConfig()));
    }

    @Test
    public void combinedConfigFromRouter()
    {
        ReflectionTestUtils.setField(transformRegistry, "isTRouter", true);
        ReflectionTestUtils.setField(transformConfigFromTransformEngines, "transformEngines", ImmutableList.of(
            new FakeTransformEngineWithAllInOne(),
            new FakeTransformEngineWithTwoCustomTransformers()));
        transformConfigFromTransformEngines.initTransformEngineConfig();
        transformRegistry.retrieveConfig();

        assertEquals("Pdf2Png, TxT2Pdf, Txt2PngViaPdf",
            getTransformerNames(transformRegistry.getTransformConfig()));
    }

    @Test
    public void singleTransformEngineWithAdditionalConfig()
    {
        ReflectionTestUtils.setField(transformConfigFromTransformEngines, "transformEngines", ImmutableList.of(
            new FakeTransformEngineWithOneCustomTransformer()));
        ReflectionTestUtils.setField(transformConfigFiles, "files", ImmutableMap.of(
            "a",   "config/addA2B.json",
            "foo", "config/addB2C.json"));

        transformConfigFromTransformEngines.initTransformEngineConfig();
        transformConfigFromFiles.initFileConfig();
        transformRegistry.retrieveConfig();

        assertEquals("A2B, B2C, Pdf2Jpg", getTransformerNames(transformRegistry.getTransformConfig()));
    }

    @Test
    public void singleTransformEngineWithHistoricAdditionalRoutes()
    {
        ReflectionTestUtils.setField(transformConfigFromTransformEngines, "transformEngines", ImmutableList.of(
            new FakeTransformEngineWithOneCustomTransformer()));
        ReflectionTestUtils.setField(transformConfigFilesHistoric, "additional", ImmutableMap.of(
            "a",   "config/addA2B.json",
            "foo", "config/addB2C.json"));

        transformConfigFromTransformEngines.initTransformEngineConfig();
        transformConfigFromFiles.initFileConfig();
        transformRegistry.retrieveConfig();

        assertEquals("A2B, B2C, Pdf2Jpg", getTransformerNames(transformRegistry.getTransformConfig()));
    }

    @Test
    public void singleTransformEngineWithHistoricTransformerRoutesExternalFile()
    {
        ReflectionTestUtils.setField(transformConfigFromTransformEngines, "transformEngines", ImmutableList.of(
            new FakeTransformEngineWithOneCustomTransformer()));
        ReflectionTestUtils.setField(transformConfigFilesHistoric, "TRANSFORMER_ROUTES_FROM_CLASSPATH",
            "config/removePdf2JpgAndAddA2Z.json"); // checking it is ignored
        ReflectionTestUtils.setField(transformConfigFilesHistoric, "transformerRoutesExternalFile",
            "config/addA2B.json");

        transformConfigFromTransformEngines.initTransformEngineConfig();
        transformConfigFromFiles.initFileConfig();
        transformRegistry.retrieveConfig();

        assertEquals("A2B, Pdf2Jpg", getTransformerNames(transformRegistry.getTransformConfig()));
    }

    @Test
    public void singleTransformEngineWithHistoricTransformerRoutesOnClasspath()
    {
        ReflectionTestUtils.setField(transformConfigFromTransformEngines, "transformEngines", ImmutableList.of(
            new FakeTransformEngineWithOneCustomTransformer()));
        ReflectionTestUtils.setField(transformConfigFilesHistoric, "TRANSFORMER_ROUTES_FROM_CLASSPATH",
            "config/removePdf2JpgAndAddA2Z.json");

        transformConfigFromTransformEngines.initTransformEngineConfig();
        transformConfigFromFiles.initFileConfig();
        transformRegistry.retrieveConfig();

        assertEquals("A2Z", getTransformerNames(transformRegistry.getTransformConfig()));
    }

    @Test
    public void isReadyForTransformRequests()
    {
        transformConfigFromTransformEngines.initTransformEngineConfig();
        transformRegistry.retrieveConfig();
        assertFalse(transformRegistry.isReadyForTransformRequests());

        ReflectionTestUtils.setField(transformConfigFromTransformEngines, "transformEngines", ImmutableList.of(
            new FakeTransformEngineWithOneCustomTransformer()));
        transformConfigFromTransformEngines.initTransformEngineConfig();
        transformRegistry.retrieveConfig();

        assertTrue(transformRegistry.isReadyForTransformRequests());
    }
}
