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
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;

public class FakeTransformEngineWithFragments  extends AbstractFakeTransformEngine
{
    @Override public TransformConfig getTransformConfig()
    {
        String imageOptions = "imageOptions";
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
