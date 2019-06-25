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
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.openxml4j.opc.PackageRelationshipTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Extracts out Thumbnail JPEGs from OOXML files for thumbnailing & previewing.
 * This transformer will only work for OOXML files where thumbnailing was enabled,
 *  which isn't on by default on Windows, but is more common on Mac.
 *
 * @author Nick Burch
 * @author eknizat
 *
 */
public class OOXMLThumbnailContentTransformer implements SelectableTransformer
{
    private static final Logger logger = LoggerFactory.getLogger(OOXMLThumbnailContentTransformer.class);

    private static final List<String> OOXML_MIMETYPES = Arrays.asList(new String[]{
            Mimetype.MIMETYPE_OPENXML_WORDPROCESSING,
            Mimetype.MIMETYPE_OPENXML_WORDPROCESSING_MACRO,
            Mimetype.MIMETYPE_OPENXML_WORD_TEMPLATE,
            Mimetype.MIMETYPE_OPENXML_WORD_TEMPLATE_MACRO,
            Mimetype.MIMETYPE_OPENXML_PRESENTATION,
            Mimetype.MIMETYPE_OPENXML_PRESENTATION_MACRO,
            Mimetype.MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW,
            Mimetype.MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW_MACRO,
            Mimetype.MIMETYPE_OPENXML_PRESENTATION_TEMPLATE,
            Mimetype.MIMETYPE_OPENXML_PRESENTATION_TEMPLATE_MACRO,
            Mimetype.MIMETYPE_OPENXML_PRESENTATION_ADDIN,
            Mimetype.MIMETYPE_OPENXML_PRESENTATION_SLIDE,
            Mimetype.MIMETYPE_OPENXML_PRESENTATION_SLIDE_MACRO,
            Mimetype.MIMETYPE_OPENXML_SPREADSHEET,
            Mimetype.MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE,
            Mimetype.MIMETYPE_OPENXML_SPREADSHEET_MACRO,
            Mimetype.MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE_MACRO,
            Mimetype.MIMETYPE_OPENXML_SPREADSHEET_ADDIN_MACRO,
            Mimetype.MIMETYPE_OPENXML_SPREADSHEET_BINARY_MACRO});

    @Override
    public boolean isTransformable(String sourceMimetype, String targetMimetype, Map<String, String> parameters)
    {
        // only support [OOXML] -> JPEG
        return Mimetype.MIMETYPE_IMAGE_JPEG.equals(targetMimetype) && OOXML_MIMETYPES.contains(sourceMimetype);
    }

    @Override
    public void transform(File sourceFile, File targetFile, Map<String, String> parameters) throws Exception
    {
        final String sourceMimetype = parameters.get(SOURCE_MIMETYPE);
        final String targetMimetype = parameters.get(TARGET_MIMETYPE);

        if(logger.isDebugEnabled())
        {
            logger.debug("Performing OOXML to jpeg transform with sourceMimetype=" + sourceMimetype
                    + " targetMimetype=" + targetMimetype);
        }

        try (OPCPackage pkg = OPCPackage.open(sourceFile.getPath()))
        {

            // Does it have a thumbnail?
            PackageRelationshipCollection rels = pkg.getRelationshipsByType(PackageRelationshipTypes.THUMBNAIL);
            if (rels.size() > 0)
            {
                // Get the thumbnail part
                PackageRelationship tRel = rels.getRelationship(0);
                PackagePart tPart = pkg.getPart(tRel);

                // Write it to the target
                InputStream tStream = tPart.getInputStream();
                Files.copy(tStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                tStream.close();
            }
            else
            {
                logger.debug("No thumbnail present in file.");
                throw new Exception("No thumbnail present in file, unable to generate " + targetMimetype);
            }
        }
        catch (IOException e)
        {
            throw new AlfrescoRuntimeException("Unable to transform file.", e);
        }
    }

    /*
    // TODO Add this back to engine_config.json when the transformer is fixed for java 11
    {
      "transformerName": "ooxmlThumbnail",
      "supportedSourceAndTargetList": [
        {"sourceMediaType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",    "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.ms-word.document.macroenabled.12",                           "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.openxmlformats-officedocument.wordprocessingml.template",    "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.ms-word.template.macroenabled.12",                           "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.openxmlformats-officedocument.presentationml.presentation",  "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.ms-powerpoint.presentation.macroenabled.12",                 "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.openxmlformats-officedocument.presentationml.slideshow",     "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.ms-powerpoint.slideshow.macroenabled.12",                    "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.openxmlformats-officedocument.presentationml.template",      "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.ms-powerpoint.template.macroenabled.12",                     "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.ms-powerpoint.addin.macroenabled.12",                        "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.openxmlformats-officedocument.presentationml.slide",         "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.ms-powerpoint.slide.macroenabled.12",                        "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",          "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.openxmlformats-officedocument.spreadsheetml.template",       "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.ms-excel.sheet.macroenabled.12",                             "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.ms-excel.template.macroenabled.12",                          "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.ms-excel.addin.macroenabled.12",                             "targetMediaType": "image/jpeg"},
        {"sourceMediaType": "application/vnd.ms-excel.sheet.binary.macroenabled.12",                      "targetMediaType": "image/jpeg"}
      ],
      "transformOptions": [
      ]
    }
     */
}
