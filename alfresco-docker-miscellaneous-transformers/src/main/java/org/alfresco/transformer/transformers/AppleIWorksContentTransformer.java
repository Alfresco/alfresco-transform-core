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

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.transform.client.model.Mimetype;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Content transformer which wraps the HTML Parser library for
 * parsing HTML content.
 *
 * <p>
 * This code is based on a class of the same name originally implemented in alfresco-repository.
 * </p>
 *
 * <p>
 * Since HTML Parser was updated from v1.6 to v2.1, META tags
 * defining an encoding for the content via http-equiv=Content-Type
 * will ONLY be respected if the encoding of the content item
 * itself is set to ISO-8859-1.
 * </p>
 *
 * <p>
 * Tika Note - could be converted to use the Tika HTML parser,
 *  but we'd potentially need a custom text handler to replicate
 *  the current settings around links and non-breaking spaces.
 * </p>
 *
 * @see <a href="http://htmlparser.sourceforge.net/">http://htmlparser.sourceforge.net</a>
 * @see org.htmlparser.beans.StringBean
 * @see <a href="http://sourceforge.net/tracker/?func=detail&aid=1644504&group_id=24399&atid=381401">HTML Parser</a>
 *
 * @author Derek Hulley
 * @author eknizat
 */
@Component
public class AppleIWorksContentTransformer extends AbstractJavaTransformer
{
    private static final Logger logger = LoggerFactory.getLogger(AppleIWorksContentTransformer.class);

    public static final String SOURCE_MIMETYPE = "sourceMimetype";
    public static final String SOURCE_EXTENSION = "sourceExtension";
    public static final String TARGET_MIMETYPE = "targetMimetype";
    public static final String TARGET_EXTENSION = "targetExtension";

    public static final List<String> REQUIRED_OPTIONS = Arrays.asList(
            SOURCE_MIMETYPE,
            SOURCE_EXTENSION,
            TARGET_MIMETYPE,
            TARGET_EXTENSION);


    // Apple's zip entry names for previews in iWorks have changed over time.
    private static final List<String> PDF_PATHS = Arrays.asList(
            "QuickLook/Preview.pdf");  // iWorks 2008/9
    private static final List<String> JPG_PATHS = Arrays.asList(
            "QuickLook/Thumbnail.jpg", // iWorks 2008/9
            "preview.jpg");            // iWorks 2013/14 (720 x 552) We use the best quality image. Others are:
    //                (225 x 173) preview-web.jpg
    //                 (53 x  41) preview-micro.jpg

    private static final List<String> IWORKS_MIMETYPES = Arrays.asList(Mimetype.MIMETYPE_IWORK_KEYNOTE,
            Mimetype.MIMETYPE_IWORK_NUMBERS,
            Mimetype.MIMETYPE_IWORK_PAGES);
    private static final List<String> TARGET_MIMETYPES = Arrays.asList(Mimetype.MIMETYPE_IMAGE_JPEG
// Commented out rather than removed, in case we can get SHARE to fall back to using JPEG when a PDF is not available
//                                                                    ,MimetypeMap.MIMETYPE_PDF
    );

    @Autowired
    public AppleIWorksContentTransformer(SelectingTransformer miscTransformer)
    {
        super(miscTransformer);
    }

    @Override
    public boolean isTransformable(String sourceMimetype, String targetMimetype, Map<String, String> parameters)
    {
        // only support [iWorks] -> JPEG but only if these are embedded in the file.
        // This is because iWorks 13+ files are zip files containing embedded jpeg previews.
        return TARGET_MIMETYPES.contains(targetMimetype) && IWORKS_MIMETYPES.contains(sourceMimetype);
    }

    @Override
    void transformInternal(File sourceFile, File targetFile, Map<String, String> parameters)
    {
        final String sourceMimetype = parameters.get(SOURCE_MIMETYPE);
        final String sourceExtension = parameters.get(SOURCE_EXTENSION);
        final String targetMimetype = parameters.get(TARGET_MIMETYPE);
        final String targetExtension = parameters.get(TARGET_EXTENSION);

        if (logger.isDebugEnabled())
        {
            StringBuilder msg = new StringBuilder();
            msg.append("Transforming from ").append(sourceMimetype)
                    .append(" to ").append(targetMimetype);
            logger.debug(msg.toString());
        }

        // iWorks files are zip (or package) files.
        // If it's not a zip file, the resultant ZipException will be caught as an IOException below.
        try (ZipArchiveInputStream iWorksZip = new ZipArchiveInputStream(new FileInputStream(sourceFile)))
        {
            // Look through the zip file entries for the preview/thumbnail.
            List<String> paths = Mimetype.MIMETYPE_IMAGE_JPEG.equals(targetMimetype) ? JPG_PATHS : PDF_PATHS;
            ZipArchiveEntry entry;
            boolean found = false;
            while ((entry=iWorksZip.getNextZipEntry()) != null)
            {
                String name = entry.getName();
                if (paths.contains(name))
                {
                    Files.copy(iWorksZip, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    found = true;
                    break;
                }
            }

            if (! found)
            {
                throw new AlfrescoRuntimeException("The source " + sourceExtension + " file did not contain a " + targetExtension + " preview");
            }
        }
        catch (IOException e)
        {
            throw new AlfrescoRuntimeException("Unable to transform " + sourceExtension + " file. It should have been a zip format file.", e);
        }
    }

    @Override
    public Set<String> getRequiredOptionNames()
    {
        return new HashSet<>(REQUIRED_OPTIONS);
    }
}
