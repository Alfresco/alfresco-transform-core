package org.alfresco.transform.misc;

import org.alfresco.transform.base.LivenessReadinessProbeTest;

public class MiscLivenessReadinessProbeTest extends LivenessReadinessProbeTest {
    @Override
    protected ImagesForTests getImageForTest() {
        return new ImagesForTests("misc", "alfresco-transform-misc", "text/plain", "text/plain", "original.txt");
    }
}
