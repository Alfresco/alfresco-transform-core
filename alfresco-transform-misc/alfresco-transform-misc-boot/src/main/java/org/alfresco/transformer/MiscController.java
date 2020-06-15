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

import org.alfresco.transformer.probes.ProbeTestTransform;
import org.alfresco.transformer.transformers.SelectingTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transformer.transformers.HtmlParserContentTransformer.SOURCE_ENCODING;
import static org.alfresco.transformer.util.RequestParamMap.TRANSFORM_NAME_PARAMETER;

@Controller
public class MiscController extends AbstractTransformerController
{
    private static final Logger logger = LoggerFactory.getLogger(MiscController.class);

    private SelectingTransformer transformer = new SelectingTransformer();

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
        return new ProbeTestTransform(this, "quick.html", "quick.txt",
            119, 30, 150, 1024,
            60 * 2 + 1, 60 * 2)
        {
            @Override
            protected void executeTransformCommand(File sourceFile, File targetFile)
            {
                Map<String, String> parameters = new HashMap<>();
                parameters.put(SOURCE_ENCODING, "UTF-8");
                processTransform("html", MIMETYPE_HTML, MIMETYPE_TEXT_PLAIN, parameters, sourceFile, targetFile, null);
            }
        };
    }

    @Deprecated
    public void processTransform(final File sourceFile, final File targetFile,
                                 final String sourceMimetype, final String targetMimetype,
                                 final Map<String, String> transformOptions, final Long timeout)
    {
        processTransform(null, sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile, timeout);
    }

    @Override
    public void processTransform(String transformName, String sourceMimetype, String targetMimetype,
                                    Map<String, String> transformOptions, File sourceFile, File targetFile, Long timeout)
    {
        transformOptions.put(TRANSFORM_NAME_PARAMETER, transformName);
        transformer.transform(sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile);
    }
}
