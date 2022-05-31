/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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

import org.alfresco.transformer.executors.ImageMagickCommandExecutor;
import org.alfresco.transformer.probes.ProbeTestTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * Controller for the Docker based ImageMagick transformer.
 *
 *
 * Status Codes:
 *
 * 200 Success
 * 400 Bad Request: Invalid cropGravity value (North, NorthEast, East, SouthEast, South, SouthWest, West, NorthWest, Center)
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
public class  ImageMagickController extends AbstractTransformerController
{
    private static final Logger logger = LoggerFactory.getLogger(ImageMagickController.class);

    @Value("${transform.core.imagemagick.exe}")
    private String EXE;

    @Value("${transform.core.imagemagick.dyn}")
    private String DYN;

    @Value("${transform.core.imagemagick.root}")
    private String ROOT;

    @Value("${transform.core.imagemagick.coders}")
    private String CODERS;

    @Value("${transform.core.imagemagick.config}")
    private String CONFIG;

    ImageMagickCommandExecutor commandExecutor;

    @PostConstruct
    private void init()
    {
        commandExecutor = new ImageMagickCommandExecutor(EXE, DYN, ROOT, CODERS, CONFIG);
    }

    @Override
    public String getTransformerName()
    {
        return "ImageMagick";
    }

    @Override
    public String version()
    {
        return commandExecutor.version();
    }

    @Override
    public ProbeTestTransform getProbeTestTransform()
    {
        // See the Javadoc on this method and Probes.md for the choice of these values.
        return new ProbeTestTransform(this, "quick.jpg", "quick.png",
            35593, 1024, 150, 1024, 60 * 15 + 1, 60 * 15)
        {
            @Override
            protected void executeTransformCommand(File sourceFile, File targetFile)
            {
                transformImpl(null, null, null, Collections.emptyMap(), sourceFile, targetFile);
            }
        };
    }

    @Override
    protected String getTransformerName(final File sourceFile, final String sourceMimetype,
                                        final String targetMimetype, final Map<String, String> transformOptions)
    {
        return null; // does not matter what value is returned, as it is not used because there is only one.
    }

    @Override
    public void transformImpl(String transformName, String sourceMimetype, String targetMimetype,
                                 Map<String, String> transformOptions, File sourceFile, File targetFile)
    {
        commandExecutor.transformExtractOrEmbed(transformName, sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile);
    }
}
