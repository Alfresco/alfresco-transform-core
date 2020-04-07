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
package org.alfresco.transformer.transformers;

import org.alfresco.transformer.AIOTransformRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

/**
 * Top level transformer managing multiple sub transformers.
 *
 * @author eknizat
 */
public class AllInOneTransformer implements Transformer
{

    private static final Logger logger = LoggerFactory.getLogger(AllInOneTransformer.class);

    /**
     * Represents the mapping between a transform and a transformer, multiple mappings can point to the same transformer.
     */
    AIOTransformRegistry transformRegistry = new AIOTransformRegistry();

    public AllInOneTransformer()
    {

    }

    public void addTransformer(Transformer transformer) throws Exception
    {
        transformRegistry.registerTransformer(transformer);
    }


    @Override
    public void transform(File sourceFile, File targetFile, String sourceMimetype, String targetMimetype,
                          Map<String, String> transformOptions) throws Exception
    {
        String transformName = transformOptions.get(TRANSFORM_NAME_PARAMETER);
        Transformer transformer = transformRegistry.getByTransformName(transformName);

        if (transformer == null)
        {
            throw new Exception("No transformer mapping for : transform:" + transformName + " sourceMimetype:"
                    + sourceMimetype + " targetMimetype:" + targetMimetype);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Performing transform '{}' using {}", transformName, transformer.getClass().getSimpleName());
        }
        transformer.transform(sourceFile, targetFile, sourceMimetype, targetMimetype, transformOptions);
    }

    @Override
    public String getTransformerId()
    {
        return "all-in-one";
    }


    public AIOTransformRegistry getTransformRegistry()
    {
        return transformRegistry;
    }

    public void setTransformRegistry(AIOTransformRegistry transformRegistry)
    {
        this.transformRegistry = transformRegistry;
    }
}
