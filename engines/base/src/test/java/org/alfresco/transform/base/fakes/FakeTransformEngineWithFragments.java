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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.config.SupportedSourceAndTarget;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.Transformer;

import java.util.Collections;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;

public class FakeTransformEngineWithFragments  extends AbstractFakeTransformEngine
{
    @Override public TransformConfig getTransformConfig()
    {
        return TransformConfig.builder()
            .withTransformers(ImmutableList.of(
                Transformer.builder()
                    .withTransformerName("Fragments")
                    .withSupportedSourceAndTargetList(ImmutableSet.of(
                        SupportedSourceAndTarget.builder()
                            .withSourceMediaType(MIMETYPE_PDF)
                            .withTargetMediaType(MIMETYPE_IMAGE_JPEG)
                            .build()))
                    .build()))
        .build();
    }

    @Override public ProbeTransform getProbeTransform()
    {
        return new ProbeTransform("probe.pdf", MIMETYPE_PDF, MIMETYPE_IMAGE_JPEG, Collections.emptyMap(),
            60, 16, 400, 10240, 60 * 30 + 1, 60 * 15 + 20);
    }
}
