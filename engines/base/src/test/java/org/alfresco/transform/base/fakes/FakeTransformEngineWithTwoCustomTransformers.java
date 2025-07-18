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
package org.alfresco.transform.base.fakes;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_ENCODING;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.config.SupportedSourceAndTarget;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.TransformOptionValue;
import org.alfresco.transform.config.TransformStep;
import org.alfresco.transform.config.Transformer;

public class FakeTransformEngineWithTwoCustomTransformers extends AbstractFakeTransformEngine
{
    @Override
    public TransformConfig getTransformConfig()
    {
        String docOptions = "docOptions";
        String imageOptions = "imageOptions";
        return TransformConfig.builder()
                .withTransformOptions(ImmutableMap.of(
                        docOptions, ImmutableSet.of(
                                new TransformOptionValue(false, "page")),
                        imageOptions, ImmutableSet.of(
                                new TransformOptionValue(false, "width"),
                                new TransformOptionValue(false, "height"))))
                .withTransformers(ImmutableList.of(
                        Transformer.builder()
                                .withTransformerName("TxT2Pdf")
                                .withSupportedSourceAndTargetList(ImmutableSet.of(
                                        SupportedSourceAndTarget.builder()
                                                .withSourceMediaType(MIMETYPE_TEXT_PLAIN)
                                                .withTargetMediaType(MIMETYPE_PDF)
                                                .build()))
                                .withTransformOptions(ImmutableSet.of(docOptions))
                                .build(),
                        Transformer.builder()
                                .withTransformerName("Pdf2Png")
                                .withSupportedSourceAndTargetList(ImmutableSet.of(
                                        SupportedSourceAndTarget.builder()
                                                .withSourceMediaType(MIMETYPE_PDF)
                                                .withTargetMediaType(MIMETYPE_IMAGE_PNG)
                                                .build()))
                                .withTransformOptions(ImmutableSet.of(imageOptions))
                                .build(),
                        Transformer.builder()
                                .withTransformerName("Txt2PngViaPdf")
                                .withTransformerPipeline(List.of(
                                        new TransformStep("TxT2Pdf", MIMETYPE_PDF),
                                        new TransformStep("Pdf2Png", null)))
                                .withSupportedSourceAndTargetList(ImmutableSet.of(
                                        SupportedSourceAndTarget.builder()
                                                .withSourceMediaType(MIMETYPE_TEXT_PLAIN)
                                                .withTargetMediaType(MIMETYPE_IMAGE_PNG)
                                                .build()))
                                .withTransformOptions(ImmutableSet.of(imageOptions))
                                .build(),
                        Transformer.builder() // Unavailable until Pdf2Jpg is added
                                .withTransformerName("Txt2JpgViaPdf")
                                .withTransformerPipeline(List.of(
                                        new TransformStep("TxT2Pdf", MIMETYPE_PDF),
                                        new TransformStep("Pdf2Jpg", null)))
                                .withSupportedSourceAndTargetList(ImmutableSet.of(
                                        SupportedSourceAndTarget.builder()
                                                .withSourceMediaType(MIMETYPE_TEXT_PLAIN)
                                                .withTargetMediaType(MIMETYPE_IMAGE_JPEG)
                                                .build()))
                                .withTransformOptions(ImmutableSet.of(imageOptions))
                                .build()))
                .build();
    }

    @Override
    public ProbeTransform getProbeTransform()
    {
        return new ProbeTransform("original.txt", MIMETYPE_TEXT_PLAIN, MIMETYPE_PDF,
                ImmutableMap.of(SOURCE_ENCODING, "UTF-8"), 46, 0,
                150, 1024, 1, 60 * 2);
    }
}
