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
package org.alfresco.transform.base.executors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import static org.alfresco.transform.base.executors.RuntimeExec.ExecutionResult;
import static org.alfresco.transform.base.fs.FileManager.assertWithinTempDir;

import java.io.File;
import java.util.Map;

import org.alfresco.transform.exceptions.TransformException;

public abstract class AbstractCommandExecutor implements CommandExecutor
{
    protected RuntimeExec transformCommand = createTransformCommand();
    protected RuntimeExec checkCommand = createCheckCommand();

    protected abstract RuntimeExec createTransformCommand();

    protected abstract RuntimeExec createCheckCommand();

    @Override
    public void run(Map<String, String> properties, File targetFile, Long timeout)
    {
        targetFile = assertWithinTempDir(targetFile);
        timeout = timeout != null && timeout > 0 ? timeout : 0;
        final ExecutionResult result = transformCommand.execute(properties, timeout);

        if (result.getExitValue() != 0 && result.getStdErr() != null && result.getStdErr().length() > 0)
        {
            throw new TransformException(BAD_REQUEST, "Transformer exit code was not 0: \n" + result.getStdErr());
        }

        if (!targetFile.exists() || targetFile.length() == 0)
        {
            // Left as a retryable 500 on purpose. A command line tool that produced nothing may have been given a
            // document it cannot read, but it may equally have run out of temporary space, been killed by the
            // container, or hit a font it could not load. Nothing here can tell those apart, and getting it wrong in
            // the terminal direction loses a document that a retry would have converted. What the message can do is
            // carry the evidence, so an operator can see which it was.
            throw new TransformException(INTERNAL_SERVER_ERROR,
                    "Transformer failed to create an output file" + describe(result));
        }
    }

    /**
     * @return what the tool reported, so the cause of an empty output is visible rather than guessed at. Both streams are included because which one a tool explains itself on varies.
     */
    private static String describe(ExecutionResult result)
    {
        StringBuilder sb = new StringBuilder(". Exit code ").append(result.getExitValue());
        appendIfPresent(sb, ", stderr: ", result.getStdErr());
        appendIfPresent(sb, ", stdout: ", result.getStdOut());
        return sb.toString();
    }

    private static void appendIfPresent(StringBuilder sb, String label, String output)
    {
        if (output != null && !output.isBlank())
        {
            sb.append(label).append(output.strip());
        }
    }
}
