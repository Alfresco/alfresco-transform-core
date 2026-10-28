/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2026 Alfresco Software Limited
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipException;

import org.springframework.boot.test.context.TestComponent;

import org.alfresco.transform.base.TransformManager;

/**
 * A transformer that always fails, throwing whichever exception the source content names. Used to check that {@code TransformExceptionClassifier} is wired into the real response path, so a document failure is reported as {@code 422} while an infrastructure failure stays {@code 500}.
 */
@TestComponent
public class FakeTransformerFails extends AbstractFakeTransformer
{
    @Override
    public void transform(String sourceMimetype, InputStream inputStream, String targetMimetype,
            OutputStream outputStream, Map<String, String> transformOptions, TransformManager transformManager)
            throws Exception
    {
        final String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        switch (content)
        {
        case "corrupt-zip":
            throw new ZipException("invalid stored block lengths");
        case "corrupt-pdf":
            // The exact failure reported in MNT-25570: a .docx renamed to .pdf.
            throw new IOException("Missing root object specification in trailer.");
        case "infrastructure":
            throw new IllegalStateException("Connection pool shut down");
        default:
            throw new IllegalArgumentException("Unexpected test content: " + content);
        }
    }
}
