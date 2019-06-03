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

import org.alfresco.transform.client.model.Mimetype;
import org.alfresco.transformer.transformers.SelectingTransformer;
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

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.alfresco.transformer.transformers.HtmlParserContentTransformer.SOURCE_ENCODING;
import static org.alfresco.transformer.transformers.HtmlParserContentTransformer.TARGET_ENCODING;
import static org.alfresco.transformer.fs.FileManager.createAttachment;
import static org.alfresco.transformer.fs.FileManager.createSourceFile;
import static org.alfresco.transformer.fs.FileManager.createTargetFile;
import static org.alfresco.transformer.fs.FileManager.createTargetFileName;
import static org.alfresco.transformer.logging.StandardMessages.ENTERPRISE_LICENCE;
import static org.springframework.http.HttpStatus.OK;

@Controller
public class MiscController extends AbstractTransformerController
{
    private static final Logger logger = LoggerFactory.getLogger(MiscController.class);

    @Autowired
    private SelectingTransformer transformer;

    public MiscController()
    {
        logger.info("--------------------------------------------------------------------------------------------------------------------------------------------------------------");
        Arrays.stream(ENTERPRISE_LICENCE.split("\\n")).forEach(logger::info);
        // TODO Set proper info message.
        logger.info("Some message about the Miscellaneous Transformers");
        logger.info("--------------------------------------------------------------------------------------------------------------------------------------------------------------");
    }

    @Override
    public String getTransformerName()
    {
        return "Miscellaneous Transformers";
    }

    @Override
    public String version()
    {
        return getTransformerName() + " available";
    }

    @Override
    public ProbeTestTransform getProbeTestTransform()
    {
        // HtmlParserContentTransformer html -> text
        // See the Javadoc on this method and Probes.md for the choice of these values.
        //TODO Review arguments.
        return new ProbeTestTransform(this, "quick.html", "quick.txt",
                119, 30, 150, 1024,
                60*15+1,60*15)
        {
            @Override
            protected void executeTransformCommand(File sourceFile, File targetFile)
            {
                Map<String, String> parameters = new HashMap<>();
                parameters.put(SOURCE_ENCODING, "UTF-8");
                parameters.put(TARGET_ENCODING, "UTF-8");
                transformer.transform(sourceFile, targetFile, Mimetype.MIMETYPE_HTML,
                        Mimetype.MIMETYPE_TEXT_PLAIN, parameters);
            }
        };
    }
    @Override
    public void processTransform(File sourceFile, File targetFile, Map<String, String> transformOptions, Long timeout)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Processing request with: sourceFile '{}', targetFile '{}', transformOptions" +
                    " '{}', timeout {} ms", sourceFile, targetFile, transformOptions, timeout);
        }

        String sourceMimetype = transformOptions.get("sourceMimetype");
        String targetMimetype = transformOptions.get("targetMimetype");
        transformer.transform(sourceFile, targetFile, sourceMimetype, targetMimetype,  transformOptions);
    }

    @PostMapping(value = "/transform", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> transform(HttpServletRequest request,
                                              @RequestParam("file") MultipartFile sourceMultipartFile,
                                              @RequestParam(value = "targetExtension") String targetExtension,
                                              @RequestParam(value = "targetMimetype") String targetMimetype,
                                              @RequestParam(value = "sourceMimetype") String sourceMimetype,
                                              @RequestParam(value = "testDelay", required = false) Long testDelay,
                                              @RequestParam Map<String, String> parameters)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Processing request with: sourceMimetype '{}', targetMimetype '{}' , targetExtension '{}' " +
                    ", parameters '{}'", sourceMimetype, targetMimetype, targetExtension, parameters);
        }

        String targetFilename = createTargetFileName(sourceMultipartFile.getOriginalFilename(), targetExtension);
        getProbeTestTransform().incrementTransformerCount();
        File sourceFile = createSourceFile(request, sourceMultipartFile);
        File targetFile = createTargetFile(request, targetFilename);

        transformer.transform(sourceFile, targetFile, sourceMimetype, targetMimetype, parameters);

        final ResponseEntity<Resource> body = createAttachment(targetFilename, targetFile);
        LogEntry.setTargetSize(targetFile.length());
        long time = LogEntry.setStatusCodeAndMessage(OK.value(), "Success");
        time += LogEntry.addDelay(testDelay);
        getProbeTestTransform().recordTransformTime(time);
        return body;
    }
}
