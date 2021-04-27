/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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

import com.google.common.collect.ImmutableMap;
import org.alfresco.transformer.logging.LogEntry;
import org.alfresco.transformer.metadataExtractors.AbstractTikaMetadataExtractor;
import org.alfresco.transformer.metadataExtractors.DWGMetadataExtractor;
import org.alfresco.transformer.metadataExtractors.MP3MetadataExtractor;
import org.alfresco.transformer.metadataExtractors.MailMetadataExtractor;
import org.alfresco.transformer.metadataExtractors.OfficeMetadataExtractor;
import org.alfresco.transformer.metadataExtractors.OpenDocumentMetadataExtractor;
import org.alfresco.transformer.metadataExtractors.PdfBoxMetadataExtractor;
import org.alfresco.transformer.metadataExtractors.PoiMetadataExtractor;
import org.alfresco.transformer.metadataExtractors.TikaAudioMetadataExtractor;
import org.alfresco.transformer.metadataExtractors.TikaAutoMetadataExtractor;
import org.alfresco.transformer.metadataExtractors.IPTCMetadataExtractor;
import org.alfresco.transformer.util.RequestParamMap;
import org.apache.tika.exception.TikaException;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringJoiner;

import static java.lang.Boolean.parseBoolean;
import static org.alfresco.transformer.executors.Tika.INCLUDE_CONTENTS;
import static org.alfresco.transformer.executors.Tika.TARGET_ENCODING;
import static org.alfresco.transformer.executors.Tika.TARGET_MIMETYPE;

/**
 * JavaExecutor implementation for running TIKA transformations. It loads the
 * transformation logic in the same JVM (check {@link Tika}).
 */
public class TikaJavaExecutor implements JavaExecutor
{
    private boolean notExtractBookmarksTextDefault;
    
    private static final String ID = "tika";

    public static final String LICENCE = 
        "This transformer uses Tika from Apache. See the license at http://www.apache.org/licenses/LICENSE-2.0. or in /Apache\\ 2.0.txt\n" +
        "This transformer uses ExifTool by Phil Harvey. See license at https://exiftool.org/#license. or in /Perl GNU License.txt & /Perl Artistic License.txt";

    private final Tika tika;
    private final Map<String, AbstractTikaMetadataExtractor> metadataExtractor = ImmutableMap
            .<String, AbstractTikaMetadataExtractor>builder()
            .put("DWGMetadataExtractor", new DWGMetadataExtractor())
            .put("MailMetadataExtractor", new MailMetadataExtractor())
            .put("MP3MetadataExtractor", new MP3MetadataExtractor())
            .put("OfficeMetadataExtractor", new OfficeMetadataExtractor())
            .put("OpenDocumentMetadataExtractor", new OpenDocumentMetadataExtractor())
            .put("PdfBoxMetadataExtractor", new PdfBoxMetadataExtractor())
            .put("PoiMetadataExtractor", new PoiMetadataExtractor())
            .put("TikaAudioMetadataExtractor", new TikaAudioMetadataExtractor())
            .put("TikaAutoMetadataExtractor", new TikaAutoMetadataExtractor())
            .put("IPTCMetadataExtractor", new IPTCMetadataExtractor())
            .build();
    private final Map<String, AbstractTikaMetadataExtractor> metadataEmbedder = ImmutableMap
            .<String, AbstractTikaMetadataExtractor>builder()
            .put("SamplePoiMetadataEmbedder", new PoiMetadataExtractor())
            .build();

    public TikaJavaExecutor(boolean notExtractBookmarksTextDefault)
    {
        this.notExtractBookmarksTextDefault = notExtractBookmarksTextDefault;
        try
        {
            tika = new Tika();
        }
        catch (SAXException | IOException | TikaException e)
        {
            throw new RuntimeException("Unable to instantiate Tika:  " + e.getMessage());
        }
    }

    public TikaJavaExecutor() 
    {
        this(false);
    }

    @Override
    public String getTransformerId()
    {
        return ID;
    }

    @Override
    public void transform(String transformName, String sourceMimetype, String targetMimetype,
                          Map<String, String> transformOptions, File sourceFile, File targetFile)
            throws Exception
    {
        final boolean includeContents = parseBoolean(
                transformOptions.getOrDefault(RequestParamMap.INCLUDE_CONTENTS, "false"));
        final boolean notExtractBookmarksText = parseBoolean(
                transformOptions.getOrDefault(RequestParamMap.NOT_EXTRACT_BOOKMARKS_TEXT, String.valueOf(notExtractBookmarksTextDefault)));
        final String targetEncoding = transformOptions.getOrDefault("targetEncoding", "UTF-8");
        if(transformOptions.get(RequestParamMap.NOT_EXTRACT_BOOKMARKS_TEXT)==null && notExtractBookmarksTextDefault)
        {
            LoggerFactory.getLogger(TikaJavaExecutor.class).trace(
                    "notExtractBookmarksText default value has been overridden to {}",
                    notExtractBookmarksTextDefault);
        }
        call(sourceFile, targetFile, transformName,
                includeContents ? INCLUDE_CONTENTS : null,
                notExtractBookmarksText ? Tika.NOT_EXTRACT_BOOKMARKS_TEXT : null,
                TARGET_MIMETYPE + targetMimetype, TARGET_ENCODING + targetEncoding);
    }

    @Override
    public void call(File sourceFile, File targetFile, String... args)
    {
        args = buildArgs(sourceFile, targetFile, args);
        tika.transform(args);
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

    public void extractMetadata(String transformName, String sourceMimetype, String targetMimetype,
                                Map<String, String> transformOptions, File sourceFile, File targetFile)
                            throws Exception
    {
        AbstractTikaMetadataExtractor metadataExtractor = this.metadataExtractor.get(transformName);
        metadataExtractor.extractMetadata(sourceMimetype, transformOptions, sourceFile, targetFile);
    }

    /**
     * @deprecated The content repository's TikaPoweredMetadataExtracter provides no non test implementations.
     *             This code exists in case there are custom implementations, that need to be converted to T-Engines.
     *             It is simply a copy and paste from the content repository and has received limited testing.
     */
    @Override
    @SuppressWarnings("deprecation" )
    public void embedMetadata(String transformName, String sourceMimetype, String targetMimetype,
                              Map<String, String> transformOptions, File sourceFile, File targetFile)
                            throws Exception
    {
        AbstractTikaMetadataExtractor metadataExtractor = this.metadataEmbedder.get(transformName);
        metadataExtractor.embedMetadata(sourceMimetype, targetMimetype, transformOptions, sourceFile, targetFile);
    }
}
