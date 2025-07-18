/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2015 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.config.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import org.alfresco.transform.config.SupportedSourceAndTarget;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.TransformOption;
import org.alfresco.transform.config.TransformOptionGroup;
import org.alfresco.transform.config.TransformOptionValue;
import org.alfresco.transform.config.TransformStep;
import org.alfresco.transform.config.Transformer;

public class TransformConfigReaderJsonTest
{
    @Test
    public void testEmptyRoutesFile() throws Exception
    {
        final Resource resource = new ClassPathResource("config/sample1.json");
        final TransformConfigReader loader = TransformConfigReaderFactory.create(resource);
        TransformConfig transformConfig = loader.load();
        final List<Transformer> transformers = transformConfig.getTransformers();

        assertNotNull(transformers);
        assertEquals(Collections.emptyList(), transformers);
    }

    @Test
    public void testMixedRoutesFile() throws Exception
    {
        final List<Transformer> expected = prepareSample2();

        final Resource resource = new ClassPathResource("config/sample2.json");
        final TransformConfigReader loader = TransformConfigReaderFactory.create(resource);

        TransformConfig transformConfig = loader.load();
        final List<Transformer> transformers = transformConfig.getTransformers();

        assertNotNull(transformers);
        assertEquals(expected.size(), transformers.size());
        assertTrue(expected.containsAll(transformers));
    }

    private List<Transformer> prepareSample2()
    {
        return List.of(
                Transformer.builder()
                        .withTransformerName("CORE_AIO")
                        .withSupportedSourceAndTargetList(ImmutableSet.of(
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("image/gif")
                                        .withTargetMediaType("image/gif")
                                        .build()))
                        .withTransformOptions(ImmutableSet.of("imageMagickOptions"))
                        .build(),
                Transformer.builder()
                        .withTransformerName("IMAGEMAGICK")
                        .withSupportedSourceAndTargetList(ImmutableSet.of(
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("image/gif")
                                        .withTargetMediaType("image/gif")
                                        .build()))
                        .withTransformOptions(ImmutableSet.of("imageMagickOptions"))
                        .build(),
                Transformer.builder()
                        .withTransformerName("CORE_AIO")
                        .withSupportedSourceAndTargetList(ImmutableSet.of(
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("application/msword")
                                        .withTargetMediaType("application/pdf")
                                        .withMaxSourceSizeBytes(18874368L)
                                        .build()))
                        .build(),
                Transformer.builder()
                        .withTransformerName("PDF_RENDERER")
                        .withSupportedSourceAndTargetList(ImmutableSet.of(
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("application/vnd.ms-powerpoint")
                                        .withTargetMediaType("application/pdf")
                                        .withPriority(55)
                                        .withMaxSourceSizeBytes(50331648L)
                                        .build()))
                        .build(),
                Transformer.builder()
                        .withTransformerName("CORE_AIO")
                        .withSupportedSourceAndTargetList(ImmutableSet.of(
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/plain")
                                        .withTargetMediaType("text/plain")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/mediawiki")
                                        .withTargetMediaType("text/plain")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/css")
                                        .withTargetMediaType("text/plain")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/csv")
                                        .withTargetMediaType("text/plain")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/xml")
                                        .withTargetMediaType("text/plain")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/html")
                                        .withTargetMediaType("text/plain")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("application/x-javascript")
                                        .withTargetMediaType("text/plain")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("application/dita+xml")
                                        .withTargetMediaType("text/plain")
                                        .build()))
                        .withTransformOptions(ImmutableSet.of("stringOptions"))
                        .build(),
                Transformer.builder()
                        .withTransformerName("officeToImageViaPdf")
                        .withTransformerPipeline(ImmutableList.of(
                                new TransformStep("libreoffice", "application/pdf"),
                                new TransformStep("pdfToImageViaPng", null)))
                        .withTransformOptions(ImmutableSet.of(
                                "pdfRendererOptions",
                                "imageMagickOptions"))
                        .build(),
                Transformer.builder()
                        .withTransformerName("textToImageViaPdf")
                        .withTransformerPipeline(ImmutableList.of(
                                new TransformStep("libreoffice", "application/pdf"),
                                new TransformStep("pdfToImageViaPng", null)))
                        .withTransformOptions(ImmutableSet.of(
                                "pdfRendererOptions",
                                "imageMagickOptions"))
                        .withSupportedSourceAndTargetList(ImmutableSet.of(
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/plain")
                                        .withTargetMediaType("image/gif")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/plain")
                                        .withTargetMediaType("image/jpeg")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/plain")
                                        .withTargetMediaType("image/tiff")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/plain")
                                        .withTargetMediaType("image/png")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/csv")
                                        .withTargetMediaType("image/gif")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/csv")
                                        .withTargetMediaType("image/jpeg")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/csv")
                                        .withTargetMediaType("image/tiff")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/csv")
                                        .withTargetMediaType("image/png")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/xml")
                                        .withTargetMediaType("image/gif")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/xml")
                                        .withTargetMediaType("image/jpeg")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/xml")
                                        .withTargetMediaType("image/tiff")
                                        .build(),
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("text/xml")
                                        .withTargetMediaType("image/png")
                                        .build()))
                        .withTransformOptions(ImmutableSet.of(
                                "pdfRendererOptions",
                                "imageMagickOptions"))
                        .build());
    }

    @Test
    public void testRouteFileWithTransformOptions() throws Exception
    {
        final List<Transformer> expected = prepareSample5Transformers();
        Map<String, Set<TransformOption>> expectedOptions = prepareSample5Options();

        final Resource resource = new ClassPathResource("config/sample5.json");
        final TransformConfigReader loader = TransformConfigReaderFactory.create(resource);

        TransformConfig transformConfig = loader.load();
        final List<Transformer> transformers = transformConfig.getTransformers();
        Map<String, Set<TransformOption>> transformOptions = transformConfig.getTransformOptions();

        assertNotNull(transformers);
        assertEquals(expected.size(), transformers.size());
        assertTrue(expected.containsAll(transformers));
        assertEquals(expectedOptions, transformOptions);
    }

    private List<Transformer> prepareSample5Transformers()
    {
        return List.of(
                Transformer.builder()
                        .withTransformerName("CORE_AIO")
                        .withSupportedSourceAndTargetList(ImmutableSet.of(
                                SupportedSourceAndTarget.builder()
                                        .withSourceMediaType("image/gif")
                                        .withTargetMediaType("image/gif")
                                        .build()))
                        .withTransformOptions(ImmutableSet.of("imageMagickOptions"))
                        .build());
    }

    public static Map<String, Set<TransformOption>> prepareSample5Options()
    {
        return ImmutableMap.of(
                "imageMagickOptions", ImmutableSet.of(
                        new TransformOptionValue(false, "alphaRemove"),
                        new TransformOptionValue(false, "autoOrient"),
                        new TransformOptionValue(false, "startPage"),
                        new TransformOptionValue(false, "endPage"),
                        new TransformOptionGroup(false, ImmutableSet.of(
                                new TransformOptionValue(false, "cropGravity"),
                                new TransformOptionValue(false, "cropWidth"),
                                new TransformOptionValue(false, "cropHeight"),
                                new TransformOptionValue(false, "cropPercentage"),
                                new TransformOptionValue(false, "cropXOffset"),
                                new TransformOptionValue(false, "cropYOffset"))),
                        new TransformOptionGroup(false, ImmutableSet.of(
                                new TransformOptionValue(false, "thumbnail"),
                                new TransformOptionValue(false, "resizeHeight"),
                                new TransformOptionValue(false, "resizeWidth"),
                                new TransformOptionValue(false, "resizePercentage"),
                                new TransformOptionValue(false, "allowEnlargement"),
                                new TransformOptionValue(false, "maintainAspectRatio")))));
    }
}
