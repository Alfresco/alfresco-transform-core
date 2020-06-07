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
package org.alfresco.transformer;

import org.alfresco.transformer.executors.TikaJavaExecutor;
import org.alfresco.transformer.probes.ProbeTestTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import static org.alfresco.transformer.executors.Tika.PDF_BOX;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_PDF;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transformer.util.RequestParamMap.TRANSFORM_NAME_PARAMETER;

/**
 * Controller for the Docker based Tika transformers.
 *
 * Status Codes:
 *
 * 200 Success
 * 400 Bad Request: Invalid target mimetype <mimetype>
 * 400 Bad Request: Request parameter <name> is missing (missing mandatory parameter)
 * 400 Bad Request: Request parameter <name> is of the wrong type
 * 400 Bad Request: Transformer exit code was not 0 (possible problem with the source file)
 * 400 Bad Request: The source filename was not supplied
 * 500 Internal Server Error: (no message with low level IO problems)
 * 500 Internal Server Error: The target filename was not supplied (should not happen as targetExtension is checked)
 * 500 Internal Server Error: Transformer version check exit code was not 0
 * 500 Internal Server Error: Transformer version check failed to create any output
 * 500 Internal Server Error: Could not read the target file
 * 500 Internal Server Error: The target filename was malformed (should not happen because of other checks)
 * 500 Internal Server Error: Transformer failed to create an output file (the exit code was 0, so there should be some content)
 * 500 Internal Server Error: Filename encoding error
 * 507 Insufficient Storage: Failed to store the source file
 */
@Controller
public class TikaController extends AbstractTransformerController
{
    private static final Logger logger = LoggerFactory.getLogger(TikaController.class);

    private TikaJavaExecutor javaExecutor = new TikaJavaExecutor();

    @Override
    public String getTransformerName()
    {
        return "Tika";
    }

    @Override
    public String version()
    {
        return "Tika available";
    }

    @Override
    public ProbeTestTransform getProbeTestTransform()
    {
        // See the Javadoc on this method and Probes.md for the choice of these values.
        // the livenessPercentage is a little large as Tika does tend to suffer from slow transforms that class with a gc.
        return new ProbeTestTransform(this, "quick.pdf", "quick.txt",
            60, 16, 400, 10240, 60 * 30 + 1, 60 * 15 + 20)
        {
            @Override
            protected void executeTransformCommand(File sourceFile, File targetFile)
            {
                transform(PDF_BOX, MIMETYPE_PDF, MIMETYPE_TEXT_PLAIN, Collections.emptyMap(), sourceFile, targetFile);
            }
        };
    }

    @Override
    protected void transform(String transformName, String sourceMimetype, String targetMimetype,
                             Map<String, String> transformOptions, File sourceFile, File targetFile)
    {
        transformOptions.put(TRANSFORM_NAME_PARAMETER, transformName);
        javaExecutor.transform(sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile);
    }
}
