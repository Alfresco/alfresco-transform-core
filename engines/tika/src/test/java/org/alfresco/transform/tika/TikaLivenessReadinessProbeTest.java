package org.alfresco.transform.tika;

import org.alfresco.transform.base.LivenessReadinessProbeTest;


public class TikaLivenessReadinessProbeTest extends LivenessReadinessProbeTest {
    @Override
    protected ImagesForTests getImageForTest() {
        return new ImagesForTests("libreoffice", "alfresco-libreoffice", "text/plain", "application/pdf", "original.txt");
    }
}
