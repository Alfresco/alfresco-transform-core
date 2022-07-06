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
package org.alfresco.transform.tika.transformers;

import org.alfresco.transform.base.logging.LogEntry;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.alfresco.transform.common.RequestParamMap;
import org.apache.tika.extractor.DocumentSelector;
import org.apache.tika.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringJoiner;

import static java.lang.Boolean.parseBoolean;

public abstract class GenericTikaTransformer implements CustomTransformerFileAdaptor
{
    private static final Logger logger = LoggerFactory.getLogger(GenericTikaTransformer.class);

    @Value("${transform.core.tika.pdfBox.notExtractBookmarksTextDefault:false}")
    boolean notExtractBookmarksTextDefault;
    @Autowired
    protected Tika tika;

    protected abstract Parser getParser();

    protected DocumentSelector getDocumentSelector()
    {
        return null;
    }

    @Override
    public String getTransformerName()
    {
        String simpleClassName = getClass().getSimpleName();
        return simpleClassName.substring(0, simpleClassName.length()-"Transformer".length());
    }

    @Override
    public void transform(String sourceMimetype, String targetMimetype,
            Map<String, String> transformOptions, File sourceFile, File targetFile)
            throws Exception
    {
        final boolean includeContents = parseBoolean(
                transformOptions.getOrDefault(RequestParamMap.INCLUDE_CONTENTS, "false"));
        final boolean notExtractBookmarksText = parseBoolean(
                transformOptions.getOrDefault(RequestParamMap.NOT_EXTRACT_BOOKMARKS_TEXT, String.valueOf(notExtractBookmarksTextDefault)));
        final String targetEncoding = transformOptions.getOrDefault("targetEncoding", "UTF-8");
        if (transformOptions.get(RequestParamMap.NOT_EXTRACT_BOOKMARKS_TEXT) == null && notExtractBookmarksTextDefault)
        {
            logger.trace("notExtractBookmarksText default value has been overridden to {}", notExtractBookmarksTextDefault);
        }
        String transformerName = getTransformerName();
        call(sourceFile, targetFile, transformerName,
                includeContents ? Tika.INCLUDE_CONTENTS : null,
                notExtractBookmarksText ? Tika.NOT_EXTRACT_BOOKMARKS_TEXT : null,
                Tika.TARGET_MIMETYPE + targetMimetype, Tika.TARGET_ENCODING + targetEncoding);
    }

    void call(File sourceFile, File targetFile, String... args)
    {
        Parser parser = getParser();
        DocumentSelector documentSelector = getDocumentSelector();
        args = buildArgs(sourceFile, targetFile, args);
        tika.transform(parser, documentSelector, args);
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
}
