package org.alfresco.transform.libreoffice;

import org.alfresco.transform.base.LivenessReadinessProbeTest;

public class LibreOfficeLivenessReadinessProbeTest extends LivenessReadinessProbeTest {
    @Override
    protected ImagesForTests getImageForTest() {
        return new ImagesForTests("tika", "alfresco-tika", "text/plain", "text/plain", "original.txt");
    }
}
