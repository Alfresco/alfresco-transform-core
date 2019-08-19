/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.util.Map;

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.util.exec.RuntimeExec;
import org.alfresco.util.exec.RuntimeExec.ExecutionResult;

/**
 *
 */
public abstract class AbstractCommandExecutor implements CommandExecutor
{
    private final RuntimeExec transformCommand = createTransformCommand();
    private final RuntimeExec checkCommand = createCheckCommand();

    protected abstract RuntimeExec createTransformCommand();

    protected abstract RuntimeExec createCheckCommand();

    @Override
    public void run(Map<String, String> properties, File targetFile, Long timeout)
    {
        timeout = timeout != null && timeout > 0 ? timeout : 0;
        final ExecutionResult result = transformCommand.execute(properties, timeout);

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
        if (checkCommand != null)
        {
            final ExecutionResult result = checkCommand.execute();
            if (result.getExitValue() != 0 && result.getStdErr() != null && result.getStdErr().length() > 0)
            {
                throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                    "Transformer version check exit code was not 0: \n" + result);
            }

            final String version = result.getStdOut().trim();
            if (version.isEmpty())
            {
                throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                    "Transformer version check failed to create any output");
            }
            return version;
        }
        return "Version not checked";
    }
}
