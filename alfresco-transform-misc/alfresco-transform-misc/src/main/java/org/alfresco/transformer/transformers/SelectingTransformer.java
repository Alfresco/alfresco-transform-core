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
package org.alfresco.transformer.transformers;

import com.google.common.collect.ImmutableMap;
import org.alfresco.transformer.executors.Transformer;
import org.alfresco.transformer.logging.LogEntry;
import org.alfresco.transformer.metadataExtractors.HtmlMetadataExtractor;
import org.alfresco.transformer.metadataExtractors.RFC822MetadataExtractor;

import java.io.File;
import java.util.Map;
import java.util.StringJoiner;

import static org.alfresco.transformer.util.RequestParamMap.TRANSFORM_NAME_PARAMETER;

/**
 * The SelectingTransformer selects a registered {@link SelectableTransformer}
 * and delegates the transformation to its implementation.
 *
 * @author eknizat
 */
public class SelectingTransformer implements Transformer
{
    private static final String ID = "misc";

    public static final String LICENCE =
            "This transformer uses libraries from Apache. See the license at http://www.apache.org/licenses/LICENSE-2.0. or in /Apache\\\\ 2.0.txt\\n" +
            "Additional libraries used:\n" +
            "* htmlparser http://htmlparser.sourceforge.net/license.html";

    private final Map<String, SelectableTransformer> transformers = ImmutableMap
        .<String, SelectableTransformer>builder()
        .put("appleIWorks", new AppleIWorksContentTransformer())
        .put("html", new HtmlParserContentTransformer())
        .put("string", new StringExtractingContentTransformer())
        .put("textToPdf", new TextToPdfContentTransformer())
        .put("rfc822", new EMLTransformer())
        .put("ooXmlThumbnail", new OOXMLThumbnailContentTransformer())
        .put("HtmlMetadataExtractor", new HtmlMetadataExtractor())
        .put("RFC822MetadataExtractor", new RFC822MetadataExtractor())
        .build();

    @Override
    public String getTransformerId()
    {
        return ID;
    }

    @Override
    public void transform(String transformName, String sourceMimetype, String targetMimetype,
                           Map<String, String> transformOptions,
                           File sourceFile, File targetFile) throws Exception
    {
        final SelectableTransformer transformer = transformers.get(transformName);
        logOptions(sourceFile, targetFile, transformOptions);
        transformer.transform(sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile);
    }

    public void extractMetadata(String transformName, String sourceMimetype, String targetMimetype,
                                Map<String, String> transformOptions,
                                File sourceFile, File targetFile) throws Exception
    {
        final SelectableTransformer transformer = transformers.get(transformName);
        logOptions(sourceFile, targetFile, transformOptions);
        transformer.extractMetadata(sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile);
    }

    private static void logOptions(File sourceFile, File targetFile, Map<String, String> parameters)
    {
        StringJoiner sj = new StringJoiner(" ");
        parameters.forEach((k, v) ->
        {
            if (!TRANSFORM_NAME_PARAMETER.equals(k))
            {
                sj.add("--" + k + "=" + v);
            }
        }); // keeping the existing style used in other T-Engines
        sj.add(getExtension(sourceFile));
        sj.add(getExtension(targetFile));
        LogEntry.setOptions(sj.toString());
    }

    private static String getExtension(File file)
    {
        final String name = file.getName();
        int i = name.lastIndexOf('.');
        return i == -1 ? "???" : name.substring(i + 1);
    }
}
