/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transformer.fs.FileManager.createAttachment;
import static org.alfresco.transformer.fs.FileManager.createSourceFile;
import static org.alfresco.transformer.fs.FileManager.createTargetFile;
import static org.alfresco.transformer.fs.FileManager.createTargetFileName;
import static org.alfresco.transformer.util.RequestParamMap.SOURCE_ENCODING;
import static org.alfresco.transformer.util.RequestParamMap.SOURCE_EXTENSION;
import static org.alfresco.transformer.util.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transformer.util.RequestParamMap.TARGET_EXTENSION;
import static org.alfresco.transformer.util.RequestParamMap.TARGET_MIMETYPE;
import static org.alfresco.transformer.util.RequestParamMap.TEST_DELAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.transform.client.model.config.TransformConfig;
import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.logging.LogEntry;
import org.alfresco.transformer.probes.ProbeTestTransform;
import org.alfresco.transformer.transformers.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AIOController extends AbstractTransformerController
{
    private static final Logger logger = LoggerFactory.getLogger(AIOController.class);

    @Autowired
    private  AIOTransformRegistry transformRegistry;

    @Override
    public String getTransformerName()
    {
        return "All in One Transformer";
    }

    @Override
    public String version()
    {
        return getTransformerName() + " available";
    }

    @Override
    public void processTransform(File sourceFile, File targetFile, String sourceMimetype, String targetMimetype,
            Map<String, String> transformOptions, Long timeout) 
    {

        final String transform = getTransformerName(sourceFile, sourceMimetype, targetMimetype, transformOptions);
        transformInternal( transform, sourceFile, targetFile, MIMETYPE_HTML, MIMETYPE_TEXT_PLAIN, transformOptions);

    }

    // TODO Currently uses the Misc probeTest. The implementation will need to be changed such that the test can be selected based on the required transform
    @Override
    public ProbeTestTransform getProbeTestTransform() 
    {
        // HtmlParserContentTransformer html -> text
        // See the Javadoc on this method and Probes.md for the choice of these values.
        return new ProbeTestTransform(this, "quick.html", "quick.txt",
            119, 30, 150, 1024,
            60 * 2 + 1, 60 * 2)
        {
            @Override
            protected void executeTransformCommand(File sourceFile, File targetFile)
            {
                Map<String, String> parameters = new HashMap<>();
                parameters.put(SOURCE_ENCODING, "UTF-8");
                try
                {
                    transformInternal( "misc", sourceFile, targetFile, MIMETYPE_HTML,
                            MIMETYPE_TEXT_PLAIN, parameters);
                }
                catch(Exception e)
                {
                    throw new TransformException(INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
                }
                
            }
        };
    }
    
    @PostMapping(value = "/transform", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> transform(HttpServletRequest request,
        @RequestParam("file") MultipartFile sourceMultipartFile,
        @RequestParam(SOURCE_MIMETYPE) String sourceMimetype,
        @RequestParam(TARGET_MIMETYPE) String targetMimetype,
        @RequestParam(TARGET_EXTENSION) String targetExtension,
        @RequestParam Map<String, String> requestParameters,
        @RequestParam (value = TEST_DELAY, required = false) Long testDelay)
    {

        debugLogTransform("Request parameters: ", sourceMimetype, targetMimetype, targetExtension, requestParameters);

        //Remove all required parameters from request parameters to get the list of options
        List<String> optionsToFilter = Arrays.asList(SOURCE_EXTENSION, TARGET_EXTENSION, TARGET_MIMETYPE,
                SOURCE_MIMETYPE, TEST_DELAY);
        Map<String, String> transformOptions = new HashMap<>(requestParameters);
        transformOptions.keySet().removeAll(optionsToFilter);
        transformOptions.values().removeIf(v -> v.isEmpty());


        final String targetFilename = createTargetFileName(
            sourceMultipartFile.getOriginalFilename(), targetExtension);
        getProbeTestTransform().incrementTransformerCount();
        final File sourceFile = createSourceFile(request, sourceMultipartFile);
        final File targetFile = createTargetFile(request, targetFilename);


        final String transform = getTransformerName(sourceFile, sourceMimetype, targetMimetype, transformOptions);
        try 
        {
            debugLogTransform("Performing transform with parameters: ", sourceMimetype, targetMimetype,
                    targetExtension, transformOptions);
            transformInternal(transform, sourceFile, targetFile, sourceMimetype, targetMimetype, transformOptions);
        } 
        catch (IllegalArgumentException e)
        {
            throw new TransformException(BAD_REQUEST.value(), e.getMessage(), e);
        }
        catch (Exception e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
        }

        final ResponseEntity<Resource> body = createAttachment(targetFilename, targetFile);
        LogEntry.setTargetSize(targetFile.length());
        long time = LogEntry.setStatusCodeAndMessage(OK.value(), "Success");
        time += LogEntry.addDelay(testDelay);
        getProbeTestTransform().recordTransformTime(time);
        return body;
    }

    private void debugLogTransform(String message, String sourceMimetype, String targetMimetype, String targetExtension,
                                   Map<String, String> transformOptions) {
        if (logger.isDebugEnabled())
        {
            logger.debug(
                "{} : sourceMimetype: '{}', targetMimetype: '{}', targetExtension: '{}', transformOptions: '{}'",
                message, sourceMimetype, targetMimetype, targetExtension, transformOptions);
        }
    }

    @Override
    public ResponseEntity<TransformConfig> info()
    {
        TransformConfig transformConfig = new TransformConfig();
        logger.info("GET Transform Config.");
        try
        {
            transformConfig = transformRegistry.getTransformConfig();
        }
        catch (Exception e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
        }

        return new ResponseEntity<>(transformConfig, OK);
    }

    protected void transformInternal(String transformName, File sourceFile, File targetFile, String sourceMimetype, String targetMimetype,
                      Map<String, String> transformOptions)
    {
        logger.debug("Processing request with: sourceFile '{}', targetFile '{}', transformOptions" +
                " '{}', timeout {} ms", sourceFile, targetFile, transformOptions);

        Transformer transformer = transformRegistry.getByTransformName(transformName);


        if (transformer == null)
        {
            new TransformException(INTERNAL_SERVER_ERROR.value(), "No transformer mapping for - transform:"
                    + transformName + " sourceMimetype:" + sourceMimetype + " targetMimetype:" + targetMimetype);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Performing transform '{}' using {}", transformName, transformer.getClass().getSimpleName());
        }

        try
        {
            Map<String, String> optionsWithTransformName = new HashMap<>(transformOptions);
            optionsWithTransformName.put(Transformer.TRANSFORM_NAME_PARAMETER, transformName);
            transformer.transform(sourceFile, targetFile, sourceMimetype, targetMimetype, optionsWithTransformName);

        }
        catch (Exception e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(), "Failed transform - transform:"
                    + transformName + " sourceMimetype:" + sourceMimetype + " targetMimetype:" + targetMimetype);
        }

    }
}
