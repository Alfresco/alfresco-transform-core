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
