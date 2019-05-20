/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package org.alfresco.transformer.executors;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.transformer.logging.LogEntry;

/**
 * Basic interface for executing transformations via Shell commands
 * 
 * @author Cezar Leahu
 */
public interface CommandExecutor
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
