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
package org.alfresco.transformer.transformers;

import java.io.File;
import java.util.Map;

/**
 * Implemented by transformers used by {@link SelectingTransformer}.
 *
 * @author eknizat
 */
public interface SelectableTransformer
{
    default void transform(String sourceMimetype, String targetMimetype, Map<String, String> parameters,
                   File sourceFile, File targetFile) throws Exception
    {
    }

    default void extractMetadata(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
                                 File sourceFile, File targetFile) throws Exception
    {
    }

    default void embedMetadata(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
                               File sourceFile, File targetFile) throws Exception
    {
    }
}
