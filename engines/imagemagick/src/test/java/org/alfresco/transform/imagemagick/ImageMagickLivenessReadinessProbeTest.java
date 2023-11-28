package org.alfresco.transform.imagemagick;

import org.alfresco.transform.base.LivenessReadinessProbeTest;

public class ImageMagickLivenessReadinessProbeTest extends LivenessReadinessProbeTest {
    @Override
    protected ImagesForTests getImageForTest() {
        return new ImagesForTests("imagemagick", "alfresco-imagemagick", "image/jpeg", "image/png", "test.jpeg");
    }
}
