/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2025 Alfresco Software Limited
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
package org.alfresco.transform.tika;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.params.provider.Arguments;

public final class OoxmlTestDocuments
{
    private OoxmlTestDocuments()
    {}

    /** A document and an XML part of it that Tika reads, for {@link #withMalformedXml}. */
    public static Stream<Arguments> malformedXmlParts()
    {
        return Stream.of(
                Arguments.of("quick.docx", "word/_rels/document.xml.rels"),
                Arguments.of("quick.xlsx", "xl/_rels/workbook.xml.rels"),
                Arguments.of("quick.pptx", "ppt/_rels/presentation.xml.rels"),
                Arguments.of("quick.xlsx", "xl/workbook.xml"));
    }

    /** Replaces one XML part without damaging the ZIP structure or the other entries. */
    public static byte[] withMalformedXml(byte[] original, String part) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        boolean replaced = false;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(original));
                ZipOutputStream out = new ZipOutputStream(bytes))
        {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null)
            {
                byte[] content = in.readAllBytes();
                if (entry.getName().equals(part))
                {
                    content = "<?xml version=\"1.0\"?><broken><unclosed></broken>".getBytes(StandardCharsets.UTF_8);
                    replaced = true;
                }
                out.putNextEntry(new ZipEntry(entry.getName()));
                out.write(content);
                out.closeEntry();
            }
        }
        assertTrue(replaced, "Test document is missing XML part " + part);
        return bytes.toByteArray();
    }
}
