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

import java.io.File;
import java.util.Map;

import static org.alfresco.transformer.util.RequestParamMap.TRANSFORM_NAME_PARAMETER;

public class MiscAdapter implements Transformer
{
    private static final String ID = "misc";
    private SelectingTransformer miscSelectingTransformer;

    public MiscAdapter()
    {
        miscSelectingTransformer = new SelectingTransformer();
    }

    @Override
    public void transform(File sourceFile, File targetFile, String sourceMimetype, String targetMimetype, Map<String,
            String> transformOptions)
    {
        String transformerName = transformOptions.get(TRANSFORM_NAME_PARAMETER);
        miscSelectingTransformer.transform(transformerName, sourceFile, targetFile,
                sourceMimetype, targetMimetype, transformOptions);

    }

    @Override
    public String getTransformerId()
    {
        return ID;
    }
}

