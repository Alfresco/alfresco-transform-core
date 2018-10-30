package org.alfresco.transformer.executors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.util.Map;

import org.alfresco.transformer.exceptions.TransformException;
import org.alfresco.util.exec.RuntimeExec;

/**
 */
public abstract class AbstractCommandExecutor implements CommandExecutor
{
    private RuntimeExec transformCommand = createTransformCommand();
    private RuntimeExec checkCommand = createCheckCommand();
    
    protected abstract RuntimeExec createTransformCommand();

    protected abstract RuntimeExec createCheckCommand();

    // todo remove these setters and and make the fields final
    public void setTransformCommand(RuntimeExec re) {
        transformCommand = re;
    }
    
    public void setCheckCommand(RuntimeExec re) {
        checkCommand = re;
    }
    
    @Override
    public void run(Map<String, String> properties, File targetFile, Long timeout)
    {
        timeout = timeout != null && timeout > 0 ? timeout : 0;
        RuntimeExec.ExecutionResult result = transformCommand.execute(properties, timeout);

        if (result.getExitValue() != 0 && result.getStdErr() != null && result.getStdErr().length() > 0)
        {
            throw new TransformException(BAD_REQUEST.value(),
                "Transformer exit code was not 0: \n" + result.getStdErr());
        }

        if (!targetFile.exists() || targetFile.length() == 0)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                "Transformer failed to create an output file");
        }
    }

    @Override
    public String version()
    {
        String version = "Version not checked";
        if (checkCommand != null)
        {
            RuntimeExec.ExecutionResult result = checkCommand.execute();
            if (result.getExitValue() != 0 && result.getStdErr() != null && result.getStdErr().length() > 0)
            {
                throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                    "Transformer version check exit code was not 0: \n" + result);
            }

            version = result.getStdOut().trim();
            if (version.isEmpty())
            {
                throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                    "Transformer version check failed to create any output");
            }
        }

        return version;
    }
}
