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
package org.alfresco.transformer.transformers;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.transform.exceptions.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 *
 * The SelectingTransformer selects a registered {@link AbstractJavaTransformer}
 * and delegates the transformation to its implementation.
 *
 * @author eknizat
 */
@Component
public class SelectingTransformer
{
    private static final Logger logger = LoggerFactory.getLogger(SelectingTransformer.class);

    private List<AbstractJavaTransformer> transformers = new LinkedList<>();

    public void register(AbstractJavaTransformer transformer)
    {
        transformers.add(transformer);
    }

    /**
     * Performs a transform using a transformer selected based on the provided sourceMimetype and targetMimetype.
     * @param sourceFile File to transform from
     * @param targetFile File to transform to
     * @param sourceMimetype Mimetype of the source file
     * @param targetMimetype Mimetype of the target file
     * @param parameters Additional parameters required for the transformation. See {@link AbstractJavaTransformer#getRequiredOptionNames()}
     * @throws TransformException
     */
    public void transform(File sourceFile, File targetFile, String sourceMimetype , String targetMimetype,
                          Map<String, String> parameters) throws TransformException
    {
        try
        {
            AbstractJavaTransformer transformer = selectTransformer(sourceMimetype, targetMimetype, parameters);
            transformer.transform(sourceFile, targetFile, parameters);
        }
        catch (IllegalArgumentException e)
        {
            throw new TransformException(BAD_REQUEST.value(), getMessage(e));
        }
        catch (Exception e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(), getMessage(e));
        }
        if (!targetFile.exists() || targetFile.length() == 0)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                    "Transformer failed to create an output file");
        }
    }

    private AbstractJavaTransformer selectTransformer(String sourceMimetype , String targetMimetype,
                                                      Map<String, String> parameters)
    {
        // Check that we have the sourceMimetype and targetMimetype parameters
        if (sourceMimetype == null || sourceMimetype.isEmpty() || targetMimetype == null || targetMimetype.isEmpty())
        {
            throw new IllegalArgumentException("Parameters 'sourceMimetype' and 'targetMimetype' must be provided. " +
                    "sourceMimetype=" + sourceMimetype + " targetMimetype=" + targetMimetype);
        }

        // Select a transformer, logging any errors or throwing an exception if a transformer was not selected.
        String isTransformableErrorMessage = null;
        for (AbstractJavaTransformer transformer : transformers)
        {
            try
            {
                if (transformer.isTransformable(sourceMimetype, targetMimetype, parameters))
                {
                    return transformer;
                }
            }
            catch (Exception e)
            {
                isTransformableErrorMessage = getMessage(e);
                logger.error("Error selecting a transformer for sourceMimetype=" + sourceMimetype
                        + " targetMimetype=" + targetMimetype, e);
            }

        }

        throw new AlfrescoRuntimeException(
                "Could not select a transformer for sourceMimetype=" + sourceMimetype
                        + " targetMimetype=" + targetMimetype
                        + isTransformableErrorMessage == null ? ""
                        : ". There was an error while selecting a transformer with message: "+ isTransformableErrorMessage);
    }

    private static String getMessage(Exception e)
    {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
