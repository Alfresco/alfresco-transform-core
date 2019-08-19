/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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
package org.alfresco.transformer.transformers;

import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IWORK_KEYNOTE;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IWORK_NUMBERS;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IWORK_PAGES;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * Converts Apple iWorks files to JPEGs for thumbnailing & previewing.
 * The transformer will only work for iWorks 2013/14 files. Support for iWorks 2008/9 has been dropped as we cannot
 * support both, because the newer format does not contain a PDF. If we say this transformer supports PDF, Share will
 * assume incorrectly that we can convert to PDF and we would only get a preview for the older format and never the
 * newer one. Both formats have the same mimetype.
 *
 * @author Neil Mc Erlean
 * @author eknizat
 * @since 4.0
 */
public class AppleIWorksContentTransformer implements SelectableTransformer
{
    private static final Logger logger = LoggerFactory.getLogger(
        AppleIWorksContentTransformer.class);

    // Apple's zip entry names for previews in iWorks have changed over time.
    private static final List<String> PDF_PATHS = ImmutableList.of(
        "QuickLook/Preview.pdf");  // iWorks 2008/9
    private static final List<String> JPG_PATHS = ImmutableList.of(
        "QuickLook/Thumbnail.jpg", // iWorks 2008/9
        "preview.jpg");            // iWorks 2013/14 (720 x 552) We use the best quality image. Others are:
    //                (225 x 173) preview-web.jpg
    //                 (53 x  41) preview-micro.jpg

    @Override
    public boolean isTransformable(String sourceMimetype, String targetMimetype,
        Map<String, String> parameters)
    {
        return MIMETYPE_IWORK_KEYNOTE.equals(sourceMimetype) ||
               MIMETYPE_IWORK_NUMBERS.equals(sourceMimetype) ||
               MIMETYPE_IWORK_PAGES.equals(sourceMimetype);
    }

    @Override
    public void transform(File sourceFile, File targetFile, Map<String, String> parameters)
    {
        final String sourceMimetype = parameters.get(SOURCE_MIMETYPE);
        final String targetMimetype = parameters.get(TARGET_MIMETYPE);

        logger.debug("Performing IWorks to jpeg transform with sourceMimetype={} targetMimetype={}",
            sourceMimetype, targetMimetype);

        // iWorks files are zip (or package) files.
        // If it's not a zip file, the resultant ZipException will be caught as an IOException below.
        try (ZipArchiveInputStream iWorksZip = new ZipArchiveInputStream(
            new BufferedInputStream(new FileInputStream(sourceFile))))
        {
            // Look through the zip file entries for the preview/thumbnail.
            List<String> paths = MIMETYPE_IMAGE_JPEG.equals(targetMimetype) ? JPG_PATHS : PDF_PATHS;
            ZipArchiveEntry entry;
            boolean found = false;
            while ((entry = iWorksZip.getNextZipEntry()) != null)
            {
                String name = entry.getName();
                if (paths.contains(name))
                {
                    Files.copy(iWorksZip, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    found = true;
                    break;
                }
            }

            if (!found)
            {
                throw new RuntimeException(
                    "The source " + sourceMimetype + " file did not contain a " + targetMimetype + " preview");
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(
                "Unable to transform " + sourceMimetype + " file. It should have been a zip format file.",
                e);
        }
    }
}
