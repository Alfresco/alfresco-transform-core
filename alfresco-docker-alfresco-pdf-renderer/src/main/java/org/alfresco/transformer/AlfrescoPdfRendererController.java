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
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

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
    private static final String EXE = "/usr/bin/alfresco-pdf-renderer";

    @Autowired
    public AlfrescoPdfRendererController()
    {
        logger = LogFactory.getLog(AlfrescoPdfRendererController.class);
        logger.info("-----------------------------------------------------------------------------------------------------------------------------------------------------------");
        logEnterpriseLicenseMessage();
        logger.info("alfresco-pdf-renderer uses the PDFium library from Google Inc. See the license at https://pdfium.googlesource.com/pdfium/+/master/LICENSE or in /pdfium.txt");
        logger.info("-----------------------------------------------------------------------------------------------------------------------------------------------------------");
        setTransformCommand(createTransformCommand());
        setCheckCommand(createCheckCommand());
    }

    @Override
    protected String getTransformerName()
    {
        return "Alfresco PDF Renderer";
    }

    private static RuntimeExec createTransformCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*", new String[]{EXE, "SPLIT:${options}", "${source}", "${target}"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);

        Map<String, String> defaultProperties = new HashMap<>();
        defaultProperties.put("key", null);
        runtimeExec.setDefaultProperties(defaultProperties);

        runtimeExec.setErrorCodes("1");

        return runtimeExec;
    }

    private static RuntimeExec createCheckCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*", new String[]{EXE, "--version"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);

        return runtimeExec;
    }

    @Override
    protected ProbeTestTransform getProbeTestTransform()
    {
        // See the Javadoc on this method and Probes.md for the choice of these values.
        return new ProbeTestTransform(this,"quick.pdf", "quick.png",
                7455, 1024, 150, 10240, 60*20+1, 60*15-15)
        {
            @Override
            protected void executeTransformCommand(File sourceFile, File targetFile)
            {
                AlfrescoPdfRendererController.this.executeTransformCommand("", sourceFile, targetFile, null);
            }
        };
    }

    @PostMapping("/transform")
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
        String targetFilename = createTargetFileName(sourceMultipartFile, targetExtension);
        File sourceFile = createSourceFile(request, sourceMultipartFile);
        File targetFile = createTargetFile(request, targetFilename);
        // Both files are deleted by TransformInterceptor.afterCompletion

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
        String options = args.toString();
        executeTransformCommand(options, sourceFile, targetFile, timeout);

        return createAttachment(targetFilename, targetFile, testDelay);
    }

    private void executeTransformCommand(String options, File sourceFile, File targetFile, @RequestParam(value = "timeout", required = false) Long timeout)
    {
        LogEntry.setOptions(options);

        Map<String, String> properties = new HashMap<String, String>(5);
        properties.put("options", options);
        properties.put("source", sourceFile.getAbsolutePath());
        properties.put("target", targetFile.getAbsolutePath());

        executeTransformCommand(properties, targetFile, timeout);
    }
}
