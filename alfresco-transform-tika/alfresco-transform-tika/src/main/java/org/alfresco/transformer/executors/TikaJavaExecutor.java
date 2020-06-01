/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

import static java.lang.Boolean.parseBoolean;
import static org.alfresco.transformer.executors.Tika.INCLUDE_CONTENTS;
import static org.alfresco.transformer.executors.Tika.NOT_EXTRACT_BOOKMARKS_TEXT;
import static org.alfresco.transformer.executors.Tika.TARGET_ENCODING;
import static org.alfresco.transformer.executors.Tika.TARGET_MIMETYPE;
import static org.alfresco.transformer.util.RequestParamMap.TRANSFORM_NAME_PARAMETER;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringJoiner;

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.logging.LogEntry;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 * JavaExecutor implementation for running TIKA transformations. It loads the
 * transformation logic in the same JVM (check {@link Tika}).
 */
public class TikaJavaExecutor implements JavaExecutor
{
    public static final String LICENCE = "This transformer uses Tika from Apache. See the license at http://www.apache.org/licenses/LICENSE-2.0. or in /Apache\\ 2.0.txt";

    private final Tika tika;

    public TikaJavaExecutor()
    {
        try
        {
            tika = new Tika();
        }
        catch (SAXException | IOException | TikaException e)
        {
            throw new RuntimeException("Unable to instantiate Tika:  " + e.getMessage());
        }
    }

    @Override
    public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
                          File sourceFile, File targetFile)
    {
        final String transformName = transformOptions.get(TRANSFORM_NAME_PARAMETER);
        final boolean includeContents = parseBoolean(
                transformOptions.getOrDefault("includeContents", "false"));
        final boolean notExtractBookmarksText = parseBoolean(
                transformOptions.getOrDefault("notExtractBookmarksText", "false"));
        final String targetEncoding = transformOptions.getOrDefault("targetEncoding", "UTF-8");

        call(sourceFile, targetFile, transformName,
                includeContents ? INCLUDE_CONTENTS : null,
                notExtractBookmarksText ? NOT_EXTRACT_BOOKMARKS_TEXT : null,
                TARGET_MIMETYPE + targetMimetype, TARGET_ENCODING + targetEncoding);
    }

    @Override
    public void call(File sourceFile, File targetFile, String... args)
        throws TransformException
    {
        args = buildArgs(sourceFile, targetFile, args);
        try
        {
            tika.transform(args);
        }
        catch (IllegalArgumentException e)
        {
            throw new TransformException(BAD_REQUEST.value(), getMessage(e));
        }
        catch (Exception e)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(), getMessage(e));
        }
        if (!targetFile.exists() || targetFile.length() == 0)
        {
            throw new TransformException(INTERNAL_SERVER_ERROR.value(),
                "Transformer failed to create an output file");
        }
    }

    private static String getMessage(Exception e)
    {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private static String[] buildArgs(File sourceFile, File targetFile, String[] args)
    {
        ArrayList<String> methodArgs = new ArrayList<>(args.length + 2);
        StringJoiner sj = new StringJoiner(" ");
        for (String arg : args)
        {
            addArg(methodArgs, sj, arg);
        }

        addFileArg(methodArgs, sj, sourceFile);
        addFileArg(methodArgs, sj, targetFile);

        LogEntry.setOptions(sj.toString());

        return methodArgs.toArray(new String[0]);
    }

    private static void addArg(ArrayList<String> methodArgs, StringJoiner sj, String arg)
    {
        if (arg != null)
        {
            sj.add(arg);
            methodArgs.add(arg);
        }
    }

    private static void addFileArg(ArrayList<String> methodArgs, StringJoiner sj, File arg)
    {
        if (arg != null)
        {
            String path = arg.getAbsolutePath();
            int i = path.lastIndexOf('.');
            String ext = i == -1 ? "???" : path.substring(i + 1);
            sj.add(ext);
            methodArgs.add(path);
        }
    }

    public void extractMetadata(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
                                File sourceFile, File targetFile)
    {
        // TODO
    }

    public void embedMetadata(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions,
                              File sourceFile, File targetFile)
    {
        // TODO
    }
}
