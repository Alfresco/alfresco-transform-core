/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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

import org.alfresco.transformer.executors.FFmpegCommandExecutor;
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
 * Controller for the Docker based FFmpeg transformer.
 *
 * Status Codes:
 *
 * 200 Success
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
public class FFmpegController extends AbstractTransformerController
{
    private static final Logger logger = LoggerFactory.getLogger(
            FFmpegController
                    .class);

    // note: subset from Gytheio

    public static final String PREFIX_AUDIO = "audio/";
    public static final String PREFIX_IMAGE = "image/";
    public static final String PREFIX_VIDEO = "video/";

    public static final String MEDIATYPE_IMAGE_SVG = "image/svg+xml";
    public static final String MEDIATYPE_APPLICATION_PHOTOSHOP = "image/vnd.adobe.photoshop";
    public static final String MEDIATYPE_IMG_DWG = "image/vnd.dwg";

    @Value("${transform.core.ffmpeg.exe}")
    private String execPath;

    FFmpegCommandExecutor commandExecutor;

    @PostConstruct
    private void init()
    {
        commandExecutor = new FFmpegCommandExecutor(execPath);
    }

    @Override
    public String getTransformerName()
    {
        return "FFmpeg";
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
        // TODO PoC for FFmpeg
        return new ProbeTestTransform(this, "quick.mp4", "quick.mp3",
            7455, 1024, 150, 10240, 60 * 20 + 1, 60 * 15 - 15)
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
        // note: actual supported transforms are defined by FFmpeg engine config - this is an extra sanity check
        if (! isTransformable(sourceMimetype, targetMimetype))
        {
            throw new UnsupportedOperationException("Unsupported combinations of source/target media types: "+sourceMimetype+","+targetMimetype);
        }

        commandExecutor.transform(sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile);
    }

    /**
     * Determines if the source mimetype is supported by ffmpeg
     *
     * @param mediaType the mimetype to check
     * @return Returns true if ffmpeg can handle the given mimetype format
     */
    private static boolean isSupportedSource(String mediaType)
    {
        return ((mediaType.startsWith(PREFIX_VIDEO) && !(
                mediaType.equals("video/x-rad-screenplay") ||
                        mediaType.equals("video/x-sgi-movie") ||
                        mediaType.equals("video/mpeg2"))) ||
                (mediaType.startsWith(PREFIX_AUDIO) && !(
                        mediaType.equals("audio/vnd.adobe.soundbooth"))) ||
                mediaType.equals("application/mxf"));
    }

    /**
     * Determines if FFmpeg can be made to support the given target mimetype.
     *
     * @param mimetype the mimetype to check
     * @return Returns true if ffmpeg can handle the given mimetype format
     */
    private static boolean isSupportedTarget(String mimetype)
    {
        return ((mimetype.startsWith(PREFIX_VIDEO) && !(
                mimetype.equals("video/x-rad-screenplay") ||
                        mimetype.equals("video/x-sgi-movie") ||
                        mimetype.equals("video/mpeg2"))) ||
                (mimetype.startsWith(PREFIX_IMAGE) && !(
                        mimetype.equals(MEDIATYPE_IMAGE_SVG) ||
                                mimetype.equals(MEDIATYPE_APPLICATION_PHOTOSHOP) ||
                                mimetype.equals(MEDIATYPE_IMG_DWG) ||
                                mimetype.equals("image/vnd.adobe.premiere") ||
                                mimetype.equals("image/x-portable-anymap") ||
                                mimetype.equals("image/x-xpixmap") ||
                                mimetype.equals("image/x-dwt") ||
                                mimetype.equals("image/cgm") ||
                                mimetype.equals("image/ief"))) ||
                (mimetype.startsWith(PREFIX_AUDIO) && !(
                        mimetype.equals("audio/vnd.adobe.soundbooth"))));
    }

    // note: based on Gytheio
    private boolean isTransformable(String sourceMediaType, String targetMediaType)
    {
        if (sourceMediaType.startsWith(PREFIX_AUDIO) && 
                targetMediaType.startsWith(PREFIX_IMAGE))
        {
            // Might be able to support audio to waveform image in the future, but for now...
            return false;
        }
        return (isSupportedSource(sourceMediaType) && isSupportedTarget(targetMediaType));
    }
}
