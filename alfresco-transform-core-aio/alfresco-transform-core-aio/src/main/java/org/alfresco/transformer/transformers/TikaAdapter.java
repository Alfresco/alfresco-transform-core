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

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.executors.TikaJavaExecutor;

import java.io.File;
import java.util.Map;

import static java.lang.Boolean.parseBoolean;
import static org.alfresco.transformer.executors.Tika.INCLUDE_CONTENTS;
import static org.alfresco.transformer.executors.Tika.NOT_EXTRACT_BOOKMARKS_TEXT;
import static org.alfresco.transformer.executors.Tika.TARGET_ENCODING;
import static org.alfresco.transformer.executors.Tika.TARGET_MIMETYPE;

public class TikaAdapter extends AbstractTransformer
{
    private static final String CONFIG_PREFIX = "tika";
    private TikaJavaExecutor tikaJavaExecutor;

    public TikaAdapter() throws Exception
    {
        super();
        tikaJavaExecutor = new TikaJavaExecutor();
    }

    @Override
    String getTransformerConfigPrefix()
    {
        return CONFIG_PREFIX;
    }

    @Override
    public void transform(File sourceFile, File targetFile, String sourceMimetype, String targetMimetype, 
            Map<String, String> transformOptions) throws TransformException
    {
        final String transform = transformOptions.get(TRANSFORM_NAME_PARAMETER);

        final boolean includeContents = parseBoolean(
                transformOptions.getOrDefault("includeContents", "false"));
        final boolean notExtractBookmarksText = parseBoolean(
                transformOptions.getOrDefault("notExtractBookmarksText", "false"));
        final String targetEncoding = transformOptions.getOrDefault("targetEncoding", "UTF-8");

        tikaJavaExecutor.call(sourceFile, targetFile, transform,
                includeContents ? INCLUDE_CONTENTS : null,
                notExtractBookmarksText ? NOT_EXTRACT_BOOKMARKS_TEXT : null,
                TARGET_MIMETYPE + targetMimetype, TARGET_ENCODING + targetEncoding);
    }
}
