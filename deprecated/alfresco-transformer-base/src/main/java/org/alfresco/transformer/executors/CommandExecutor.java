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
package org.alfresco.transformer.executors;

import org.alfresco.transformer.logging.LogEntry;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated will be removed in a future release. Replaced by alfresco-base-t-engine.
 *
 * Basic interface for executing transformations via Shell commands
 *
 * @author Cezar Leahu
 */
@Deprecated
public interface CommandExecutor extends Transformer
{
    void run(Map<String, String> properties, File targetFile, Long timeout);

    String version();

    default void run(String options, File sourceFile, File targetFile,
        Long timeout)
    {
        LogEntry.setOptions(options);

        Map<String, String> properties = new HashMap<>();
        properties.put("options", options);
        properties.put("source", sourceFile.getAbsolutePath());
        properties.put("target", targetFile.getAbsolutePath());

        run(properties, targetFile, timeout);
    }

    default void run(String options, File sourceFile, String pageRange, File targetFile,
        Long timeout)
    {
        LogEntry.setOptions(pageRange + (pageRange.isEmpty() ? "" : " ") + options);

        Map<String, String> properties = new HashMap<>();
        properties.put("options", options);
        properties.put("source", sourceFile.getAbsolutePath() + pageRange);
        properties.put("target", targetFile.getAbsolutePath());

        run(properties, targetFile, timeout);
    }
}
