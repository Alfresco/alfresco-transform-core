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
package org.alfresco.transform.base.components;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.alfresco.transform.base.TransformEngine;
import org.alfresco.transform.base.probes.ProbeTestTransform;
import org.alfresco.transform.config.SupportedSourceAndTarget;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.TransformOptionValue;
import org.alfresco.transform.config.Transformer;
import org.springframework.boot.test.context.TestComponent;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_ENCODING;

/**
 * Subclass MUST be named TestTransformEngine\<something>.
 */
@TestComponent
public abstract class AbstractTestTransformEngine implements TransformEngine
{
    @Override public String getTransformEngineName()
    {
        String simpleClassName = getClass().getSimpleName();
        return simpleClassName.substring("TestTransformEngine".length());
    }

    @Override public String getStartupMessage()
    {
        return "Startup";
    }

    @Override public TransformConfig getTransformConfig()
    {
        String docOptions = "docOptions";
        String imageOptions = "imageOptions";
        return TransformConfig.builder()
//            .withTransformOptions(ImmutableMap.of(
//                docOptions, ImmutableSet.of(
//                    new TransformOptionValue(false, "page")),
//                imageOptions, ImmutableSet.of(
//                    new TransformOptionValue(false, "width"),
//                    new TransformOptionValue(false, "height"))))
            .withTransformers(ImmutableList.of(
                Transformer.builder()
                   .withTransformerName("TxT2Pdf")
                   .withSupportedSourceAndTargetList(ImmutableSet.of(
                       SupportedSourceAndTarget.builder()
                           .withSourceMediaType(MIMETYPE_TEXT_PLAIN)
                           .withTargetMediaType(MIMETYPE_PDF)
                           .build()))
//                    .withTransformOptions(ImmutableSet.of(docOptions))
                   .build(),
                Transformer.builder()
                   .withTransformerName("Pdf2Png")
                   .withSupportedSourceAndTargetList(ImmutableSet.of(
                       SupportedSourceAndTarget.builder()
                           .withSourceMediaType(MIMETYPE_PDF)
                           .withTargetMediaType(MIMETYPE_IMAGE_PNG)
                           .build()))
//                   .withTransformOptions(ImmutableSet.of(imageOptions))
                   .build()))
            .build();
    }

    @Override public ProbeTestTransform getLivenessAndReadinessProbeTestTransform()
    {
        return new ProbeTestTransform("quick.html", "quick.txt",
                MIMETYPE_HTML, MIMETYPE_TEXT_PLAIN, ImmutableMap.of(SOURCE_ENCODING, "UTF-8"),
                119, 30, 150, 1024, 60 * 2 + 1, 60 * 2);
    }
}
