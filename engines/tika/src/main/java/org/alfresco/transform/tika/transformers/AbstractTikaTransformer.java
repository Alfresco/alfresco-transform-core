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

import org.alfresco.transform.base.CustomTransformer;
import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.logging.LogEntry;
import org.alfresco.transform.common.RequestParamMap;
import org.apache.tika.extractor.DocumentSelector;
import org.apache.tika.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import static java.lang.Boolean.parseBoolean;

public abstract class AbstractTikaTransformer implements CustomTransformer
{
    private static final Logger logger = LoggerFactory.getLogger(AbstractTikaTransformer.class);

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
    public void transform(String sourceMimetype, InputStream inputStream,
            String targetMimetype, OutputStream outputStream,
            Map<String, String> transformOptions, TransformManager transformManager) throws Exception
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
        call(inputStream, outputStream,
                includeContents ? Tika.INCLUDE_CONTENTS : null,
                notExtractBookmarksText ? Tika.NOT_EXTRACT_BOOKMARKS_TEXT : null,
                Tika.TARGET_MIMETYPE + targetMimetype, Tika.TARGET_ENCODING + targetEncoding);
    }

    void call(InputStream inputStream, OutputStream outputStream, String... args)
    {
        Parser parser = getParser();
        DocumentSelector documentSelector = getDocumentSelector();
        logArgs(args);
        tika.transform(parser, documentSelector, inputStream, outputStream, args);
    }

    private void logArgs(String[] args)
    {
        StringJoiner sj = new StringJoiner(" ");
        Arrays.stream(args)
                .filter(Objects::nonNull)
                .forEach(arg -> sj.add(arg));
        LogEntry.setOptions(sj.toString());
    }
}
