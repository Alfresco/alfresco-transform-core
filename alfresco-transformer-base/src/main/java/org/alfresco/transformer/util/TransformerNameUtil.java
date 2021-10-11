/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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
package org.alfresco.transformer.util;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.transform.client.registry.TransformServiceRegistry;
import org.alfresco.transform.exceptions.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.alfresco.transformer.util.RequestParamMap.SOURCE_ENCODING;
import static org.springframework.http.HttpStatus.BAD_REQUEST;


public class TransformerNameUtil
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TransformerNameUtil.class);

    public static String getTransformerName(TransformServiceRegistry transformRegistry, String sourceMimetype, long sourceSizeInBytes,
                                            String targetMimetype, String requestTransformName, Map<String, String> transformOptions)
    {
        if (transformNameWasProvidedInTheRequest(requestTransformName))
        {
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info("Using transform name provided in the request: " + requestTransformName);
            }
            return requestTransformName;
        }
        return getTransformerName(transformRegistry, sourceMimetype, sourceSizeInBytes, targetMimetype, transformOptions);
    }

    public static String getTransformerName(TransformServiceRegistry transformRegistry, String sourceMimetype, long sourceSizeInBytes,
                                            String targetMimetype, Map<String, String> transformOptions)
    {
        String transformerName = transformRegistry.findTransformerName(sourceMimetype, sourceSizeInBytes, targetMimetype,
                                                                       getOptionsWithoutEncoding(transformOptions), null);
        if (transformerName == null)
        {
            throw new TransformException(BAD_REQUEST.value(), "No transforms were able to handle the request");
        }
        return transformerName;
    }

    /**
     * Might happen for ACS legacy transformers
     */
    private static boolean transformNameWasProvidedInTheRequest(String requestTransformName)
    {
        return requestTransformName != null && !requestTransformName.isBlank();
    }

    /**
     * Source encoding should not be used to select a transformer
     */
    private static Map<String, String> getOptionsWithoutEncoding(Map<String, String> transformOptions)
    {
        Map<String, String> safeOptions = new HashMap<>(transformOptions);
        safeOptions.remove(SOURCE_ENCODING);
        return safeOptions;
    }

}

