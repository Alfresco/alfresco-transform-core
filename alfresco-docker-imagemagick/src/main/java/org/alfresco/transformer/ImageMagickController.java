/*
 * #%L
 * Alfresco Enterprise Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 * #L%
 */
package org.alfresco.transformer;

import org.alfresco.transformer.base.AbstractTransformerController;
import org.alfresco.transformer.base.LogEntry;
import org.alfresco.transformer.base.TransformException;
import org.alfresco.util.exec.RuntimeExec;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;

/**
 * Controller for the Docker based ImageMagick transformer.
 *
 *
 * Status Codes:
 *
 *   200 Success
 *   400 Bad Request: Invalid cropGravity value (North, NorthEast, East, SouthEast, South, SouthWest, West, NorthWest, Center)
 *   400 Bad Request: Request parameter <name> is missing (missing mandatory parameter)
 *   400 Bad Request: Request parameter <name> is of the wrong type
 *   400 Bad Request: Transformer exit code was not 0 (possible problem with the source file)
 *   400 Bad Request: The source filename was not supplied
 *   500 Internal Server Error: (no message with low level IO problems)
 *   500 Internal Server Error: The target filename was not supplied (should not happen as targetExtension is checked)
 *   500 Internal Server Error: Transformer version check exit code was not 0
 *   500 Internal Server Error: Transformer version check failed to create any output
 *   500 Internal Server Error: Could not read the target file
 *   500 Internal Server Error: The target filename was malformed (should not happen because of other checks)
 *   500 Internal Server Error: Transformer failed to create an output file (the exit code was 0, so there should be some content)
 *   500 Internal Server Error: Filename encoding error
 *   507 Insufficient Storage: Failed to store the source file
 */
@Controller
public class ImageMagickController extends AbstractTransformerController
{
    private static final String ROOT = "/usr/lib64/ImageMagick-7.0.7";
    private static final String DYN = ROOT+"/lib";
    private static final String EXE = "/usr/bin/convert";
    private static final List<String> GRAVITY_VALUES = Arrays.asList(
            "North", "NorthEast", "East", "SouthEast", "South", "SouthWest", "West", "NorthWest", "Center");

    @Autowired
    public ImageMagickController()
    {
        logger = LogFactory.getLog(ImageMagickController.class);
        logger.info("--------------------------------------------------------------------------------------------------------------------------------------------------------------");
        logEnterpriseLicenseMessage();
        logger.info("This transformer uses ImageMagick from ImageMagick Studio LLC. See the license at http://www.imagemagick.org/script/license.php or in /ImageMagick-license.txt");
        logger.info("--------------------------------------------------------------------------------------------------------------------------------------------------------------");
        setTransformCommand(createTransformCommand());
        setCheckCommand(createCheckCommand());
    }

    private static RuntimeExec createTransformCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*", new String[]{EXE, "${source}", "SPLIT:${options}", "-strip", "-quiet", "${target}"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);

        Map<String, String> processProperties = new HashMap<>();
        processProperties.put("MAGICK_HOME", ROOT);
        processProperties.put("DYLD_FALLBACK_LIBRARY_PATH", DYN);
        processProperties.put("LD_LIBRARY_PATH", DYN);
        runtimeExec.setProcessProperties(processProperties);

        Map<String, String> defaultProperties = new HashMap<>();
        defaultProperties.put("options", null);
        runtimeExec.setDefaultProperties(defaultProperties);

        runtimeExec.setErrorCodes("1,2,255,400,405,410,415,420,425,430,435,440,450,455,460,465,470,475,480,485,490,495,499,700,705,710,715,720,725,730,735,740,750,755,760,765,770,775,780,785,790,795,799");

        return runtimeExec;
    }

    private static RuntimeExec createCheckCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*", new String[]{EXE, "-version"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);

        return runtimeExec;
    }

    @PostMapping("/transform")
    public ResponseEntity<Resource> transform(HttpServletRequest request,
                                              @RequestParam("file") MultipartFile sourceMultipartFile,
                                              @RequestParam("targetExtension") String targetExtension,
                                              @RequestParam(value = "timeout", required = false) Long timeout,

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
                                              @RequestParam(value = "maintainAspectRatio", required = false) Boolean maintainAspectRatio)
    {
        if (cropGravity != null)
        {
            cropGravity = cropGravity.trim();
            if (!cropGravity.isEmpty() && !GRAVITY_VALUES.contains(cropGravity))
            {
                throw new TransformException(400, "Invalid cropGravity value");
            }
        }

        String targetFilename = createTargetFileName(sourceMultipartFile, targetExtension);
        File sourceFile = createSourceFile(request, sourceMultipartFile);
        File targetFile = createTargetFile(request, targetFilename);
        // Both files are deleted by TransformInterceptor.afterCompletion

        StringJoiner args = new StringJoiner(" ");
        if (alphaRemove != null && alphaRemove)
        {
            args.add("-alpha");
            args.add(("remove"));
        }
        if (autoOrient != null && autoOrient)
        {
            args.add("-auto-orient");
        }

        if (cropGravity != null || cropWidth != null || cropHeight != null || cropPercentage != null ||
                cropXOffset != null || cropYOffset != null)
        {
            if (cropGravity != null)
            {
                args.add("-gravity");
                args.add(cropGravity);
            }

            StringBuilder crop = new StringBuilder("");
            if (cropWidth != null && cropWidth >= 0)
            {
                crop.append(cropWidth);
            }
            if (cropHeight != null && cropHeight >= 0)
            {
                crop.append('x');
                crop.append(cropHeight);
            }
            if (cropPercentage != null && cropPercentage)
            {
                crop.append('%');
            }
            if (cropXOffset != null)
            {
                if (cropXOffset >= 0)
                {
                    crop.append('+');
                }
                crop.append(cropXOffset);
            }
            if (cropYOffset != null)
            {
                if (cropYOffset >= 0)
                {
                    crop.append('+');
                }
                crop.append(cropYOffset);
            }
            if (crop.length() > 1)
            {
                args.add("-crop");
                args.add(crop);
            }

            args.add("+repage");
        }

        if (resizeHeight != null || resizeWidth != null || resizePercentage !=null || maintainAspectRatio != null)
        {
            args.add(thumbnail != null && thumbnail ? "-thumbnail" : "-resize");
            StringBuilder resize = new StringBuilder("");
            if (resizeWidth != null && resizeWidth >= 0)
            {
                resize.append(resizeWidth);
            }
            if (resizeHeight != null && resizeHeight >= 0)
            {
                resize.append('x');
                resize.append(resizeHeight);
            }
            if (resizePercentage != null && resizePercentage)
            {
                resize.append('%');
            }
            if (allowEnlargement == null || !allowEnlargement)
            {
                resize.append('>');
            }
            if (maintainAspectRatio != null && maintainAspectRatio)
            {
                resize.append('!');
            }
            if (resize.length() > 1)
            {
                args.add(resize);
            }
        }

        String pageRange =
                startPage == null
                        ? endPage == null
                        ? ""
                        : "["+endPage+']'
                        : endPage == null || startPage.equals(endPage)
                        ? "["+startPage+']'
                        : "["+startPage+'-'+endPage+']';

        String options = args.toString();
        LogEntry.setOptions(pageRange+(pageRange.isEmpty() ? "" : " ")+options);

        Map<String, String> properties = new HashMap<String, String>(5);
        properties.put("options", options);
        properties.put("source", sourceFile.getAbsolutePath()+pageRange);
        properties.put("target", targetFile.getAbsolutePath());

        executeTransformCommand(properties, targetFile, timeout);

        return createAttachment(targetFilename, targetFile);
    }
}
