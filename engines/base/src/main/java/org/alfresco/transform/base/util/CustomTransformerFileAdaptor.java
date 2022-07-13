/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.base.util;

import org.alfresco.transform.base.CustomTransformer;
import org.alfresco.transform.base.TransformManager;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Helper interface for older code that uses Files rather than InputStreams and OutputStreams.
 * If you can, refactor your code to NOT use Files.
 */
public interface CustomTransformerFileAdaptor extends CustomTransformer
{
    @Override
    default void transform(String sourceMimetype, InputStream inputStream,
            String targetMimetype, OutputStream outputStream,
            Map<String, String> transformOptions, TransformManager transformManager) throws Exception
    {
        File sourceFile = transformManager.createSourceFile();
        File targetFile = transformManager.createTargetFile();
        transform(sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile);
    }

    void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
            File sourceFile, File targetFile) throws Exception;
}