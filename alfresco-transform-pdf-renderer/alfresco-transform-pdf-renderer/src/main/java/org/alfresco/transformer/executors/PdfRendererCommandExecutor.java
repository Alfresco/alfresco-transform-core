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

import java.util.HashMap;
import java.util.Map;

/**
 * CommandExecutor implementation for running PDF Renderer transformations. It runs the
 * transformation logic as a separate Shell process.
 */
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
