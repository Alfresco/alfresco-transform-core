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

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.executors.TikaJavaExecutor;
import org.alfresco.transformer.logging.LogEntry;
import org.alfresco.transformer.probes.ProbeTestTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Map;

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
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transformer.util.Util.stringToBoolean;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

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

    @Autowired
    private TikaJavaExecutor javaExecutor;

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

    @PostMapping(value = "/transform", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> transform(HttpServletRequest request,
        @RequestParam("file") MultipartFile sourceMultipartFile,
        @RequestParam("sourceMimetype") String sourceMimetype,
        @RequestParam("targetExtension") String targetExtension,
        @RequestParam("targetMimetype") String targetMimetype,
        @RequestParam("targetEncoding") String targetEncoding,

        @RequestParam(value = "timeout", required = false) Long timeout,
        @RequestParam(value = "testDelay", required = false) Long testDelay,

        @RequestParam(value = "includeContents", required = false) Boolean includeContents,
        @RequestParam(value = "notExtractBookmarksText", required = false) Boolean notExtractBookmarksText)
    {
        String targetFilename = createTargetFileName(sourceMultipartFile.getOriginalFilename(),
            targetExtension);
        getProbeTestTransform().incrementTransformerCount();
        File sourceFile = createSourceFile(request, sourceMultipartFile);
        File targetFile = createTargetFile(request, targetFilename);
        // Both files are deleted by TransformInterceptor.afterCompletion

        // TODO Consider streaming the request and response rather than using temporary files
        // https://www.logicbig.com/tutorials/spring-framework/spring-web-mvc/streaming-response-body.html

        Map<String, String> transformOptions = createTransformOptions(
                "includeContents", includeContents,
                "notExtractBookmarksText", notExtractBookmarksText,
                "targetEncoding", targetEncoding);
        String transform = getTransformerName(sourceFile, sourceMimetype, targetMimetype, transformOptions);
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
    public void processTransform(final File sourceFile, final File targetFile,
        final String sourceMimetype, final String targetMimetype,
        final Map<String, String> transformOptions, final Long timeout)
    {
        logger.debug("Processing request with: sourceFile '{}', targetFile '{}', transformOptions" +
                     " '{}', timeout {} ms", sourceFile, targetFile, transformOptions, timeout);

        final Boolean includeContents = stringToBoolean("includeContents");
        final Boolean notExtractBookmarksText = stringToBoolean("notExtractBookmarksText");
        final String targetEncoding = transformOptions.get("targetEncoding");

        String transform = getTransformerName(sourceFile, sourceMimetype, targetMimetype, transformOptions);
        javaExecutor.call(sourceFile, targetFile, transform,
            includeContents != null && includeContents ? INCLUDE_CONTENTS : null,
            notExtractBookmarksText != null && notExtractBookmarksText ? NOT_EXTRACT_BOOKMARKS_TEXT : null,
            TARGET_MIMETYPE + targetMimetype, TARGET_ENCODING + targetEncoding);
    }
}
