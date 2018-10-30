package org.alfresco.transformer.executors;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.util.exec.RuntimeExec;
import org.springframework.stereotype.Component;

/**
 * CommandExecutor implementation for running PDF Renderer transformations. It runs the 
 * transformation logic as a separate Shell process.
 */
@Component
public class PdfRendererCommandExecutor extends AbstractCommandExecutor
{
    private static final String EXE = "/usr/bin/alfresco-pdf-renderer";

    @Override
    protected RuntimeExec createTransformCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*",
            new String[]{EXE, "SPLIT:${options}", "${source}", "${target}"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);

        Map<String, String> defaultProperties = new HashMap<>();
        defaultProperties.put("key", null);
        runtimeExec.setDefaultProperties(defaultProperties);

        runtimeExec.setErrorCodes("1");

        return runtimeExec;
    }

    @Override
    protected RuntimeExec createCheckCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*", new String[]{EXE, "--version"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);
        return runtimeExec;
    }
}
