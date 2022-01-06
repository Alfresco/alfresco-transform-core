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

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.FFmpegOptionsBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.alfresco.transformer.util.RequestParamMap.TIMEOUT;
import static org.alfresco.transformer.util.RequestParamMap.TIME_OFFSET;
import static org.alfresco.transformer.util.Util.stringToLong;

/**
 * CommandExecutor implementation for running FFmpeg transformations. It runs the
 * transformation logic as a separate Shell process.
 * 
 * @author janv
 */
// TODO PoC for FFmpeg
public class FFmpegCommandExecutor extends AbstractCommandExecutor
{
    private static String ID = "ffmpeg";

    // TODO PoC for FFmpeg
    public static final String LICENCE = "TODO: This transformer uses FFmpeg. See the license at ...";

    private final String EXE;

    private final int FRAMES_NUM_1 = 1;

    public FFmpegCommandExecutor(String exe)
    {
        if (exe == null || exe.isEmpty())
        {
            throw new IllegalArgumentException("FFmpegCommandExecutor EXE variable cannot be null or empty");
        }
        this.EXE = exe;
        super.transformCommand = createTransformCommand();
        super.checkCommand = createCheckCommand();
    }

    @Override
    public String getTransformerId()
    {
        return ID;
    }

    @Override
    protected RuntimeExec createTransformCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();

        // TODO PoC for FFmpeg - check against Gytheio: -y SPLIT:${sourceOptions} -i ${source} SPLIT:${targetOptions} ${target}
        commandsAndArguments.put(".*",
              new String[]{EXE, "-y", "-i", "${source}", "SPLIT:${options}", "${target}"});

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

    @Override
    public void transform(String transformName, String sourceMimetype, String targetMimetype,
                          Map<String, String> transformOptions,
                          File sourceFile, File targetFile) throws TransformException
    {
        FFmpegOptionsBuilder optionsBuilder = FFmpegOptionsBuilder.builder();

        String timeOffset = transformOptions.get(TIME_OFFSET);
        if (timeOffset != null)
        {
            // TODO check target mimetype (to be supported image formats) for "single frame" option
            optionsBuilder.withTimeOffset(transformOptions.get(TIME_OFFSET));
            optionsBuilder.withFramesNum(FRAMES_NUM_1);
        }

        final String options = optionsBuilder.build();

        Long timeout = stringToLong(transformOptions.get(TIMEOUT));

        run(options, sourceFile, targetFile, timeout);
    }
}
