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
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.io.File;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.transformer.logging.LogEntry;
import org.alfresco.transformer.probes.ProbeTestTransform;
import org.alfresco.transformer.transformers.AllInOneTransformer;
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

    //TODO Should these be moved to the AbstractTransformerController or are they present in the transform.client? They are used by most controllers...
    private static final String SOURCE_ENCODING = "sourceEncoding";
    private static final String SOURCE_EXTENSION = "sourceExtension";
    private static final String TARGET_EXTENSION = "targetExtension";
    private static final String TARGET_MIMETYPE = "targetMimetype";
    private static final String SOURCE_MIMETYPE = "sourceMimetype";
    private static final String TEST_DELAY = "testDelay";    
    private static final String[] UNWANTED_OPTIONS = {SOURCE_EXTENSION,
                                                    TARGET_EXTENSION,
                                                    TARGET_MIMETYPE, 
                                                    SOURCE_MIMETYPE, 
                                                    TEST_DELAY                                         
                                                    };

    @Autowired
    private AllInOneTransformer transformer;

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
        debugLogTransform("Processing transform", sourceMimetype, targetMimetype, transformOptions);
        final String transform = getTransformerName(sourceFile, sourceMimetype, targetMimetype, transformOptions);
        transformOptions.put(AllInOneTransformer.TRANSFORM_NAME_PARAMETER, transform);

        transformer.transform(sourceFile, targetFile, sourceMimetype, targetMimetype, transformOptions);        

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
                parameters.put(AllInOneTransformer.TRANSFORM_NAME_PARAMETER, "misc");
                parameters.put(SOURCE_ENCODING, "UTF-8");
                transformer.transform(sourceFile, targetFile, MIMETYPE_HTML,
                MIMETYPE_TEXT_PLAIN, parameters);
            }
        };
    }
    
    @PostMapping(value = "/transform", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> transform(HttpServletRequest request,
        @RequestParam("file") MultipartFile sourceMultipartFile,
        @RequestParam(TARGET_EXTENSION) String targetExtension,
        @RequestParam(TARGET_MIMETYPE) String targetMimetype,
        @RequestParam(SOURCE_MIMETYPE) String sourceMimetype,
        @RequestParam Map<String, String> transformOptions,
        @RequestParam (value = TEST_DELAY, required = false) Long testDelay)
    {

        // TODO - remove this logginng
        debugLogTransform("Entering request with: ", sourceMimetype, targetMimetype,  transformOptions);


        //Using @RequestParam Map<String, String> will gather all text params, including those specified seperately above.
        removeUnwantedOptions(transformOptions, UNWANTED_OPTIONS, true);

        final String targetFilename = createTargetFileName(
            sourceMultipartFile.getOriginalFilename(), targetExtension);
        getProbeTestTransform().incrementTransformerCount();
        final File sourceFile = createSourceFile(request, sourceMultipartFile);
        final File targetFile = createTargetFile(request, targetFilename);

        final String transform = getTransformerName(sourceFile, sourceMimetype, targetMimetype, transformOptions);

        transformOptions.put(AllInOneTransformer.TRANSFORM_NAME_PARAMETER, transform);

        // TODO - remove this logginng
        debugLogTransform("After filtering props request with: ", sourceMimetype, targetMimetype,  transformOptions);

        transformer.transform(sourceFile, targetFile, sourceMimetype, targetMimetype, transformOptions);
    
        final ResponseEntity<Resource> body = createAttachment(targetFilename, targetFile);
        LogEntry.setTargetSize(targetFile.length());
        long time = LogEntry.setStatusCodeAndMessage(OK.value(), "Success");
        time += LogEntry.addDelay(testDelay);
        getProbeTestTransform().recordTransformTime(time);
        return body;
    }

    private void debugLogTransform(String message, String sourceMimetype, String targetMimetype, Map<String, String> transformOptions) {
        if (logger.isDebugEnabled())
        {
            logger.debug(
                "{} : sourceMimetype: '{}', targetMimetype: '{}', transformOptions: '{}'",
                message, sourceMimetype, targetMimetype, transformOptions);
        }
    }

    /**
     * Removes entries from transformOptions that have keys that match a value
     * contained in unwantedStrings.
     * Entries that contain empty strings can optionally be removed.
     * 
     * @param transformOptions
     * @param unwantedStrings
     * @param emptyStrings
     */
    private void removeUnwantedOptions(Map<String, String> transformOptions, String[] unwantedStrings, boolean emptyStrings) 
    {
        for (Iterator<Map.Entry<String, String>> iter = transformOptions.entrySet().iterator();iter.hasNext();) 
        {
            Map.Entry<String, String> entry = iter.next();
            if (entry.getValue().isEmpty() || Arrays.asList(unwantedStrings).contains(entry.getKey())) 
            {
                iter.remove();
                if (logger.isDebugEnabled())
                {
                    logger.debug("Key={} has been removed from the provided RequestParameters and it was passed value={}",
                        entry.getKey(), entry.getValue()
                    );
                }               
            }
        }
    }
}
