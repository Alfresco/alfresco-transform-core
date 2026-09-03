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
package org.alfresco.transform.imagemagick;

import static org.alfresco.transform.base.logging.StandardMessages.COMMUNITY_LICENCE;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.alfresco.transform.base.TransformEngine;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.reader.TransformConfigResourceReader;

@Component
public class ImageMagickTransformEngine implements TransformEngine
{
    @Autowired
    private TransformConfigResourceReader transformConfigResourceReader;

    @Override
    public String getTransformEngineName()
    {
        return "0030 ImageMagick";
    }

    @Override
    public String getStartupMessage()
    {
        return COMMUNITY_LICENCE +
                "This transformer uses ImageMagick from ImageMagick Studio LLC. " +
                "See the license at http://www.imagemagick.org/script/license.php or in /ImageMagick-license.txt";
    }

    @Override
    public TransformConfig getTransformConfig()
    {
        return transformConfigResourceReader.read("classpath:imagemagick_engine_config.json");
    }

    @Override
    public ProbeTransform getProbeTransform()
    {
        // The probe's target temp file is created as "probe_target_<n>_probe.jpg"
        // (ProbeTransform#getTargetFile), so the encoder is chosen from the .jpg extension and
        // the probe really performs JPEG -> JPEG, despite the declared target media type.
        //
        // Recalibrated for GraphicsMagick: ImageMagick re-encodes a JPEG source at the source's
        // own quality (probe.jpg is quality 100), giving ~25383 bytes, whereas GraphicsMagick
        // defaults to quality 75, giving a deterministic 7913 bytes for the same command.
        // plusOrMinus stays at the 1024 used by the other binary engines (cf. pdfrenderer's
        // 7455 +/- 1024), which leaves ample headroom for GraphicsMagick version drift.
        return new ProbeTransform("probe.jpg", MIMETYPE_IMAGE_JPEG, MIMETYPE_IMAGE_PNG, Collections.emptyMap(),
                7913, 1024, 150, 1024, 60 * 15 + 1, 60 * 15);
    }
}
