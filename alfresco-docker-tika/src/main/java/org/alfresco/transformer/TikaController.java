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

import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transformer.executors.Tika.INCLUDE_CONTENTS;
import static org.alfresco.transformer.executors.Tika.NOT_EXTRACT_BOOKMARKS_TEXT;
import static org.alfresco.transformer.executors.Tika.PDF_BOX;
import static org.alfresco.transformer.executors.Tika.TARGET_ENCODING;
import static org.alfresco.transformer.executors.Tika.TARGET_MIMETYPE;
import static org.alfresco.transformer.executors.Tika.TRANSFORM_NAMES;
import static org.alfresco.transformer.fs.FileManager.createAttachment;
import static org.alfresco.transformer.fs.FileManager.createSourceFile;
import static org.alfresco.transformer.fs.FileManager.createTargetFile;
import static org.alfresco.transformer.fs.FileManager.createTargetFileName;
import static org.alfresco.transformer.logging.StandardMessages.ENTERPRISE_LICENCE;
import static org.alfresco.transformer.util.Util.stringToBoolean;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.executors.TikaJavaExecutor;
import org.alfresco.transformer.logging.LogEntry;
import org.alfresco.transformer.probes.ProbeTestTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for the Docker based Tika transformers.
 *
 * Status Codes:
 *
 * 200 Success
 * 400 Bad Request: Invalid target mimetype &lt;mimetype>
 * 400 Bad Request: Request parameter &lt;name> is missing (missing mandatory parameter)
 * 400 Bad Request: Request parameter &lt;name> is of the wrong type
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

    @Autowired
    private TikaJavaExecutor javaExecutor;

    @Autowired
    public TikaController()
    {
        logger.info("--------------------------------------------------------------------------------------------------------------------------------------------------------------");
        Arrays.stream(ENTERPRISE_LICENCE.split("\\n")).forEach(logger::info);
        logger.info("Tika is from Apache. See the license at http://www.apache.org/licenses/LICENSE-2.0. or in /Apache\\ 2.0.txt");
        logger.info("--------------------------------------------------------------------------------------------------------------------------------------------------------------");
    }

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
                javaExecutor.call(sourceFile, targetFile, PDF_BOX,
                    TARGET_MIMETYPE + MIMETYPE_TEXT_PLAIN, TARGET_ENCODING + "UTF-8");
            }
        };
    }

    @PostMapping(value = "/transform", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> transform(HttpServletRequest request,
        @RequestParam("file") MultipartFile sourceMultipartFile,
        @RequestParam("targetExtension") String targetExtension,
        @RequestParam("targetMimetype") String targetMimetype,
        @RequestParam("targetEncoding") String targetEncoding,

        @RequestParam(value = "timeout", required = false) Long timeout,
        @RequestParam(value = "testDelay", required = false) Long testDelay,

        @RequestParam(value = "transform") String transform,
        @RequestParam(value = "includeContents", required = false) Boolean includeContents,
        @RequestParam(value = "notExtractBookmarksText", required = false) Boolean notExtractBookmarksText)
    {
        if (!TRANSFORM_NAMES.contains(transform))
        {
            throw new TransformException(BAD_REQUEST.value(), "Invalid transform value");
        }

        String targetFilename = createTargetFileName(sourceMultipartFile.getOriginalFilename(), targetExtension);
        getProbeTestTransform().incrementTransformerCount();
        File sourceFile = createSourceFile(request, sourceMultipartFile);
        File targetFile = createTargetFile(request, targetFilename);
        // Both files are deleted by TransformInterceptor.afterCompletion

        // TODO Consider streaming the request and response rather than using temporary files
        // https://www.logicbig.com/tutorials/spring-framework/spring-web-mvc/streaming-response-body.html

        javaExecutor.call(sourceFile, targetFile, transform,
            includeContents != null && includeContents ? INCLUDE_CONTENTS : null,
            notExtractBookmarksText != null && notExtractBookmarksText ? NOT_EXTRACT_BOOKMARKS_TEXT : null,
            TARGET_MIMETYPE + targetMimetype, TARGET_ENCODING + targetEncoding);

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
        logger.debug("Processing request with: sourceFile '{}', targetFile '{}', transformOptions" +
                     " '{}', timeout {} ms", sourceFile, targetFile, transformOptions, timeout);

        String transform = transformOptions.get("transform");
        Boolean includeContents = stringToBoolean("includeContents");
        Boolean notExtractBookmarksText = stringToBoolean("notExtractBookmarksText");
        String targetMimetype = transformOptions.get("targetMimetype");
        String targetEncoding = transformOptions.get("targetEncoding");

        javaExecutor.call(sourceFile, targetFile, transform,
            includeContents != null && includeContents ? INCLUDE_CONTENTS : null,
            notExtractBookmarksText != null && notExtractBookmarksText ? NOT_EXTRACT_BOOKMARKS_TEXT : null,
            TARGET_MIMETYPE + targetMimetype, TARGET_ENCODING + targetEncoding);
    }
}
