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

import static org.alfresco.transformer.fs.FileManager.createAttachment;
import static org.alfresco.transformer.fs.FileManager.createSourceFile;
import static org.alfresco.transformer.fs.FileManager.createTargetFile;
import static org.alfresco.transformer.fs.FileManager.createTargetFileName;
import static org.alfresco.transformer.util.Util.stringToInteger;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.io.File;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.alfresco.transformer.executors.ImageMagickCommandExecutor;
import org.alfresco.transformer.logging.LogEntry;
import org.alfresco.transformer.probes.ProbeTestTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

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
public class ImageMagickController extends AbstractTransformerController
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
                commandExecutor.run("", sourceFile, "", targetFile, null);
            }
        };
    }

    @PostMapping(value = "/transform", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> transform(HttpServletRequest request,
        @RequestParam("file") MultipartFile sourceMultipartFile,
        @RequestParam("targetExtension") String targetExtension,
        @RequestParam(value = "timeout", required = false) Long timeout,
        @RequestParam(value = "testDelay", required = false) Long testDelay,

        @RequestParam(value = "startPage", required = false) Integer startPage,
        @RequestParam(value = "endPage", required = false) Integer endPage,

        @RequestParam(value = "alphaRemove", required = false) Boolean alphaRemove,
        @RequestParam(value = "autoOrient", required = false) Boolean autoOrient,

        @RequestParam(value = "cropGravity", required = false) String cropGravity,
        @RequestParam(value = "cropWidth", required = false) Integer cropWidth,
        @RequestParam(value = "cropHeight", required = false) Integer cropHeight,
        @RequestParam(value = "cropPercentage", required = false) Boolean cropPercentage,
        @RequestParam(value = "cropXOffset", required = false) Integer cropXOffset,
        @RequestParam(value = "cropYOffset", required = false) Integer cropYOffset,

        @RequestParam(value = "thumbnail", required = false) Boolean thumbnail,
        @RequestParam(value = "resizeWidth", required = false) Integer resizeWidth,
        @RequestParam(value = "resizeHeight", required = false) Integer resizeHeight,
        @RequestParam(value = "resizePercentage", required = false) Boolean resizePercentage,
        @RequestParam(value = "allowEnlargement", required = false) Boolean allowEnlargement,
        @RequestParam(value = "maintainAspectRatio", required = false) Boolean maintainAspectRatio,

        // The commandOptions parameter is supported in ACS 6.0.1 because there may be
        // custom renditions that use it. However the Transform service should
        // not support it as it provides the option to specify arbitrary command
        // options or even the option to run something else on the command line.
        // All Transform service options should be checked as is done for the other
        // request parameters. Setting this option in the rendition's
        // ImageTransformationOptions object is being deprecated for the point where
        // The Transform service is being used for all transforms. In the case of
        // ACS 6.0, this is relatively safe as it requires an AMP to be installed
        // which supplies the commandOptions.
        @RequestParam(value = "commandOptions", required = false) String commandOptions)
    {
        String targetFilename = createTargetFileName(sourceMultipartFile.getOriginalFilename(),
            targetExtension);
        getProbeTestTransform().incrementTransformerCount();
        File sourceFile = createSourceFile(request, sourceMultipartFile);
        File targetFile = createTargetFile(request, targetFilename);
        // Both files are deleted by TransformInterceptor.afterCompletion

        final String options = ImageMagickOptionsBuilder
            .builder()
            .withStartPage(startPage)
            .withEndPage(endPage)
            .withAlphaRemove(alphaRemove)
            .withAutoOrient(autoOrient)
            .withCropGravity(cropGravity)
            .withCropWidth(cropWidth)
            .withCropHeight(cropHeight)
            .withCropPercentage(cropPercentage)
            .withCropXOffset(cropXOffset)
            .withCropYOffset(cropYOffset)
            .withThumbnail(thumbnail)
            .withResizeWidth(resizeWidth)
            .withResizeHeight(resizeHeight)
            .withResizePercentage(resizePercentage)
            .withAllowEnlargement(allowEnlargement)
            .withMaintainAspectRatio(maintainAspectRatio)
            .withCommandOptions(commandOptions)
            .build();

        String pageRange = calculatePageRange(startPage, endPage);

        commandExecutor.run(options, sourceFile, pageRange, targetFile,
            timeout);

        final ResponseEntity<Resource> body = createAttachment(targetFilename, targetFile);
        LogEntry.setTargetSize(targetFile.length());
        long time = LogEntry.setStatusCodeAndMessage(OK.value(), "Success");
        time += LogEntry.addDelay(testDelay);
        getProbeTestTransform().recordTransformTime(time);
        return body;
    }

    @Override
    public void processTransform(final File sourceFile, final File targetFile,
        final String sourceMimetype, final String targetMimetype,
        final Map<String, String> transformOptions, final Long timeout)
    {
        logger.debug("Processing request with: sourceFile '{}', targetFile '{}', transformOptions" +
                     " '{}', timeout {} ms", sourceFile, targetFile, transformOptions, timeout);

        final String options = ImageMagickOptionsBuilder
            .builder()
            .withStartPage(transformOptions.get("startPage"))
            .withEndPage(transformOptions.get("endPage"))
            .withAlphaRemove(transformOptions.get("alphaRemove"))
            .withAutoOrient(transformOptions.get("autoOrient"))
            .withCropGravity(transformOptions.get("cropGravity"))
            .withCropWidth(transformOptions.get("cropWidth"))
            .withCropHeight(transformOptions.get("cropHeight"))
            .withCropPercentage(transformOptions.get("cropPercentage"))
            .withCropXOffset(transformOptions.get("cropXOffset"))
            .withCropYOffset(transformOptions.get("cropYOffset"))
            .withThumbnail(transformOptions.get("thumbnail"))
            .withResizeWidth(transformOptions.get("resizeWidth"))
            .withResizeHeight(transformOptions.get("resizeHeight"))
            .withResizePercentage(transformOptions.get("resizePercentage"))
            .withAllowEnlargement(transformOptions.get("allowEnlargement"))
            .withMaintainAspectRatio(transformOptions.get("maintainAspectRatio"))
            .build();

        final String pageRange = calculatePageRange(
            stringToInteger(transformOptions.get("startPage")),
            stringToInteger(transformOptions.get("endPage")));

        commandExecutor.run(options, sourceFile, pageRange, targetFile,
            timeout);
    }

    private static String calculatePageRange(Integer startPage, Integer endPage)
    {
        return startPage == null
               ? endPage == null
                 ? ""
                 : "[" + endPage + ']'
               : endPage == null || startPage.equals(endPage)
                 ? "[" + startPage + ']'
                 : "[" + startPage + '-' + endPage + ']';
    }
}
