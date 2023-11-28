package org.alfresco.transform.aio;

import org.alfresco.transform.base.LivenessReadinessProbeTest;

public class AIOLivenessReadinessProbeTest extends LivenessReadinessProbeTest {
    @Override
    protected LivenessReadinessProbeTest.ImagesForTests getImageForTest() {
        return new ImagesForTests("ats-aio", "alfresco-transform-core-aio", "text/plain", "text/plain", "original.txt");
    }
}
