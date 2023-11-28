package org.alfresco.transform.pdfrenderer;

import org.alfresco.transform.base.LivenessReadinessProbeTest;

public class PdfRendererLivenessReadinessProbeTest extends LivenessReadinessProbeTest {
    @Override
    protected ImagesForTests getImageForTest() {
        return new ImagesForTests("pdf-renderer", "alfresco-pdf-renderer", "application/pdf", "image/png", "test.pdf");
    }
}
