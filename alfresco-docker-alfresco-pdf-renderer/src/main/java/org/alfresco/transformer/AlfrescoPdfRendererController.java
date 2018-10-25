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

import static org.alfresco.transformer.fs.FileManager.createAttachment;
import static org.alfresco.transformer.fs.FileManager.createSourceFile;
import static org.alfresco.transformer.fs.FileManager.createTargetFile;
import static org.alfresco.transformer.fs.FileManager.createTargetFileName;
import static org.alfresco.transformer.logging.StandardMessages.ENTERPRISE_LICENCE;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.StringJoiner;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.transformer.executors.PdfRendererCommandExecutor;
import org.alfresco.transformer.logging.LogEntry;
import org.alfresco.transformer.probes.ProbeTestTransform;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for the Docker based alfresco-pdf-renderer transformer.
 *
 * Status Codes:
 *
 *   200 Success
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
public class AlfrescoPdfRendererController extends AbstractTransformerController
{
    private static final Log logger = LogFactory.getLog(AlfrescoPdfRendererController.class);

    @Autowired
    private PdfRendererCommandExecutor commandExecutor;

    @Autowired
    public AlfrescoPdfRendererController()
    {
        logger.info("-----------------------------------------------------------------------------------------------------------------------------------------------------------");
        Arrays.stream(ENTERPRISE_LICENCE.split("\\n")).forEach(logger::info);
        logger.info("alfresco-pdf-renderer uses the PDFium library from Google Inc. See the license at https://pdfium.googlesource.com/pdfium/+/master/LICENSE or in /pdfium.txt");
        logger.info("-----------------------------------------------------------------------------------------------------------------------------------------------------------");
    }

    @Override
    public String getTransformerName()
    {
        return "Alfresco PDF Renderer";
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
        return new ProbeTestTransform(this, logger, "quick.pdf", "quick.png",
                7455, 1024, 150, 10240, 60*20+1, 60*15-15)
        {
            @Override
            protected void executeTransformCommand(File sourceFile, File targetFile)
            {
                commandExecutor.run("", sourceFile, targetFile, null);
            }
        };
    }

    @Override
    public void processTransform(File sourceFile, File targetFile,
        Map<String, String> transformOptions, Long timeout)
    {
        String page = transformOptions.get("page");
        Integer pageOption = page == null ? null : Integer.parseInt(page);

        String width = transformOptions.get("width");
        Integer widthOption = width == null ? null : Integer.parseInt(width);

        String height = transformOptions.get("height");
        Integer heightOption = height == null ? null : Integer.parseInt(height);

        String allowEnlargement = transformOptions.get("allowEnlargement");
        Boolean allowEnlargementOption =
            allowEnlargement == null ? null : Boolean.parseBoolean(allowEnlargement);

        String maintainAspectRatio = transformOptions.get("maintainAspectRatio");
        Boolean maintainAspectRatioOption =
            maintainAspectRatio == null ? null : Boolean.parseBoolean(maintainAspectRatio);

        String options = buildTransformOptions(pageOption, widthOption, heightOption,
            allowEnlargementOption, maintainAspectRatioOption);

        commandExecutor.run(options, sourceFile, targetFile, timeout);
    }

    @Deprecated
    @PostMapping(value = "/transform", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> transform(HttpServletRequest request,
                                              @RequestParam("file") MultipartFile sourceMultipartFile,
                                              @RequestParam("targetExtension") String targetExtension,
                                              @RequestParam(value = "timeout", required = false) Long timeout,
                                              @RequestParam(value = "testDelay", required = false) Long testDelay,

                                              @RequestParam(value = "page", required = false) Integer page,
                                              @RequestParam(value = "width", required = false) Integer width,
                                              @RequestParam(value = "height", required = false) Integer height,
                                              @RequestParam(value = "allowEnlargement", required = false) Boolean allowEnlargement,
                                              @RequestParam(value = "maintainAspectRatio", required = false) Boolean maintainAspectRatio)
    {
        String targetFilename = createTargetFileName(sourceMultipartFile.getOriginalFilename(), targetExtension);
        getProbeTestTransform().incrementTransformerCount();
        File sourceFile = createSourceFile(request, sourceMultipartFile);
        File targetFile = createTargetFile(request, targetFilename);
        // Both files are deleted by TransformInterceptor.afterCompletion

        String options = buildTransformOptions(page, width, height, allowEnlargement,
            maintainAspectRatio);
        commandExecutor.run(options, sourceFile, targetFile, timeout);
        
        final ResponseEntity<Resource> body = createAttachment(targetFilename, targetFile);
        LogEntry.setTargetSize(targetFile.length());
        long time = LogEntry.setStatusCodeAndMessage(200, "Success");
        time += LogEntry.addDelay(testDelay);
        getProbeTestTransform().recordTransformTime(time);
        return body;
    }


    private static String buildTransformOptions(Integer page,Integer width,Integer height,Boolean 
        allowEnlargement,Boolean maintainAspectRatio)
    {
        StringJoiner args = new StringJoiner(" ");
        if (width != null && width >= 0)
        {
            args.add("--width=" + width);
        }
        if (height != null && height >= 0)
        {
            args.add("--height=" + height);
        }
        if (allowEnlargement != null && allowEnlargement)
        {
            args.add("--allow-enlargement");
        }
        if (maintainAspectRatio != null && maintainAspectRatio)
        {
            args.add("--maintain-aspect-ratio");
        }
        if (page != null && page >= 0)
        {
            args.add("--page=" + page);
        }
        return args.toString();
    }
}
