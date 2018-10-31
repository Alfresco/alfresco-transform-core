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
import static org.springframework.http.HttpStatus.OK;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.transformer.executors.LibreOfficeJavaExecutor;
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
 * Controller for the Docker based LibreOffice transformer.
 *
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
public class LibreOfficeController extends AbstractTransformerController
{
    private static final Log logger = LogFactory.getLog(LibreOfficeController.class);

    @Autowired
    private LibreOfficeJavaExecutor javaExecutor;
    
    @Autowired
    public LibreOfficeController()
    {
        logger.info("-------------------------------------------------------------------------------------------------------------------------------------------------------");
        Arrays.stream(ENTERPRISE_LICENCE.split("\\n")).forEach(logger::info);
        logger.info("This transformer uses LibreOffice from The Document Foundation. See the license at https://www.libreoffice.org/download/license/ or in /libreoffice.txt");
        logger.info("-------------------------------------------------------------------------------------------------------------------------------------------------------");
    }

    @Override
    public String getTransformerName()
    {
        return "LibreOffice";
    }

    @Override
    public String version()
    {
        return "LibreOffice available";
    }

    @Override
    public ProbeTestTransform getProbeTestTransform()
    {
        // See the Javadoc on this method and Probes.md for the choice of these values.
        return new ProbeTestTransform(this, logger, "quick.doc", "quick.pdf",
                11817, 1024, 150, 10240, 60*30+1, 60*15+20)
        {
            @Override
            protected void executeTransformCommand(File sourceFile, File targetFile)
            {
                javaExecutor.call(sourceFile, targetFile);
            }
        };
    }

    //todo: the "timeout" request parameter is ignored; the timeout is preset at JodConverter creation
    @PostMapping(value = "/transform", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> transform(HttpServletRequest request,
                                              @RequestParam("file") MultipartFile sourceMultipartFile,
                                              @RequestParam("targetExtension") String targetExtension,
                                              @RequestParam(value = "timeout", required = false) Long timeout,
                                              @RequestParam(value = "testDelay", required = false) Long testDelay)
    {
        String targetFilename = createTargetFileName(sourceMultipartFile.getOriginalFilename(), targetExtension);
        getProbeTestTransform().incrementTransformerCount();
        File sourceFile = createSourceFile(request, sourceMultipartFile);
        File targetFile = createTargetFile(request, targetFilename);
        // Both files are deleted by TransformInterceptor.afterCompletion

        javaExecutor.call(sourceFile, targetFile);

        final ResponseEntity<Resource> body = createAttachment(targetFilename, targetFile);
        LogEntry.setTargetSize(targetFile.length());
        long time = LogEntry.setStatusCodeAndMessage(OK.value(), "Success");
        time += LogEntry.addDelay(testDelay);
        getProbeTestTransform().recordTransformTime(time);
        return body;
    }

    @Override
    public void processTransform(File sourceFile, File targetFile,
        Map<String, String> transformOptions, Long timeout)
    {
        javaExecutor.call(sourceFile, targetFile);
    }
}
