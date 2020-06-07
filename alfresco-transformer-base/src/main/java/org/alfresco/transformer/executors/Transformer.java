package org.alfresco.transformer.executors;

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
import org.alfresco.transform.exceptions.TransformException;

import java.io.File;
import java.util.Map;

import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_METADATA_EMBED;
import static org.alfresco.transformer.util.MimetypeMap.MIMETYPE_METADATA_EXTRACT;
import static org.alfresco.transformer.util.RequestParamMap.TRANSFORM_NAME_PARAMETER;

/**
 * Basic interface for executing transforms and metadata extract or embed actions.
 *
 * @author adavis
 */
public interface Transformer
{
    /**
     * @return A unique transformer id,
     *
     */
    String getTransformerId();

    default void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions, File sourceFile, File targetFile) throws TransformException
    {
        final String transformName = transformOptions.remove(TRANSFORM_NAME_PARAMETER);
        if (MIMETYPE_METADATA_EXTRACT.equals(targetMimetype))
        {
            extractMetadata(transformName, sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile);
        }
        else if (MIMETYPE_METADATA_EMBED.equals(targetMimetype))
        {
            embedMetadata(transformName, sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile);
        }
        else
        {
            transform(transformName, sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile);
        }
    }

    default void transform(String transformName, String sourceMimetype, String targetMimetype,
                           Map<String, String> transformOptions,
                           File sourceFile, File targetFile) throws TransformException
    {
    }

    default void extractMetadata(String transformName, String sourceMimetype, String targetMimetype,
                                 Map<String, String> transformOptions,
                                 File sourceFile, File targetFile) throws TransformException
    {
    }

    /**
     * @deprecated The content repository has no non test embed metadata implementations.
     *             This code exists in case there are custom implementations, that need to be converted to T-Engines.
     *             It is simply a copy and paste from the content repository and has received limited testing.
     */
    default void embedMetadata(String transformName, String sourceMimetype, String targetMimetype,
                               Map<String, String> transformOptions,
                               File sourceFile, File targetFile) throws TransformException
    {
    }
}
