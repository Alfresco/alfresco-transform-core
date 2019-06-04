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
package org.alfresco.transformer.transformers;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.logging.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_DITA;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IWORK_KEYNOTE;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IWORK_NUMBERS;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IWORK_PAGES;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_JAVASCRIPT;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_PRESENTATION;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_PRESENTATION_ADDIN;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_PRESENTATION_MACRO;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_PRESENTATION_SLIDE;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW_MACRO;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_PRESENTATION_SLIDE_MACRO;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_PRESENTATION_TEMPLATE;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_PRESENTATION_TEMPLATE_MACRO;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_SPREADSHEET;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_SPREADSHEET_ADDIN_MACRO;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_SPREADSHEET_BINARY_MACRO;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_SPREADSHEET_MACRO;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE_MACRO;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_WORDPROCESSING;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_WORDPROCESSING_MACRO;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_WORD_TEMPLATE;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_WORD_TEMPLATE_MACRO;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 *
 * The SelectingTransformer selects a registered {@link JavaTransformer}
 * and delegates the transformation to its implementation.
 *
 */
@Component
public class SelectingTransformer
{
    private static final Logger logger = LoggerFactory.getLogger(SelectingTransformer.class);

    JavaTransformer appleIWorksContentTransformer = new AppleIWorksContentTransformer();
    JavaTransformer htmlParserContentTransformer = new HtmlParserContentTransformer();
    JavaTransformer stringExtractingContentTransformer = new StringExtractingContentTransformer();

    private static final List<String> OOXML_SOURCE_MIMETYPES = Arrays.asList(new String[]{
            MIMETYPE_OPENXML_WORDPROCESSING,
            MIMETYPE_OPENXML_WORDPROCESSING_MACRO,
            MIMETYPE_OPENXML_WORD_TEMPLATE,
            MIMETYPE_OPENXML_WORD_TEMPLATE_MACRO,
            MIMETYPE_OPENXML_PRESENTATION,
            MIMETYPE_OPENXML_PRESENTATION_MACRO,
            MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW,
            MIMETYPE_OPENXML_PRESENTATION_SLIDESHOW_MACRO,
            MIMETYPE_OPENXML_PRESENTATION_TEMPLATE,
            MIMETYPE_OPENXML_PRESENTATION_TEMPLATE_MACRO,
            MIMETYPE_OPENXML_PRESENTATION_ADDIN,
            MIMETYPE_OPENXML_PRESENTATION_SLIDE,
            MIMETYPE_OPENXML_PRESENTATION_SLIDE_MACRO,
            MIMETYPE_OPENXML_SPREADSHEET,
            MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE,
            MIMETYPE_OPENXML_SPREADSHEET_MACRO,
            MIMETYPE_OPENXML_SPREADSHEET_TEMPLATE_MACRO,
            MIMETYPE_OPENXML_SPREADSHEET_ADDIN_MACRO,
            MIMETYPE_OPENXML_SPREADSHEET_BINARY_MACRO});

    /**
     * Performs a transform using a transformer selected based on the provided sourceMimetype
     * @param sourceFile File to transform from
     * @param targetFile File to transform to
     * @param sourceMimetype Mimetype of the source file
     * @param parameters Additional parameters required for the transformation. See {@link AbstractJavaTransformer#getRequiredOptionNames()}
     * @throws TransformException
     */
    public void transform(File sourceFile, File targetFile, String sourceMimetype,
                          Map<String, String> parameters) throws TransformException
    {
        try
        {
            JavaTransformer transformer = selectTransformer(sourceMimetype);
            logOptions(sourceFile, targetFile, parameters);
            transformer.transform(sourceFile, targetFile, parameters);
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

    private JavaTransformer selectTransformer(String sourceMimetype) throws Exception
    {
        // Note: consider moving the selection logic into each transformer if it gets more complex.

        if ( MIMETYPE_IWORK_KEYNOTE.equals(sourceMimetype)
                || MIMETYPE_IWORK_NUMBERS.equals(sourceMimetype)
                || MIMETYPE_IWORK_PAGES.equals(sourceMimetype))
        {
            return appleIWorksContentTransformer;
        }
        else if (MIMETYPE_HTML.equals(sourceMimetype))
        {
            return htmlParserContentTransformer;
        }
        else if ( sourceMimetype.startsWith("text/")
                || MIMETYPE_JAVASCRIPT.equals(sourceMimetype)
                || MIMETYPE_DITA.equals(sourceMimetype)
        )
        {
            return stringExtractingContentTransformer;
        }
        else if (OOXML_SOURCE_MIMETYPES.contains(sourceMimetype))
        {
            throw new UnsupportedOperationException("Transform from OOXML types not implemented.");
        }

        throw new AlfrescoRuntimeException(
                "Could not select a transformer for sourceMimetype=" + sourceMimetype);
    }

    private static String getMessage(Exception e)
    {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private void logOptions(File sourceFile, File targetFile, Map<String, String> parameters)
    {
        StringJoiner sj = new StringJoiner(" ");
        parameters.forEach( (k, v) -> sj.add("--" + k + "=" + v)); // keeping the existing style used in other T-Engines
        sj.add(getExtension(sourceFile));
        sj.add(getExtension(targetFile));
        LogEntry.setOptions(sj.toString());
    }

    private String getExtension(File file)
    {
        String name = file.getName();
        int i = name.lastIndexOf('.');
        String ext = i == -1 ? "???" : name.substring(i + 1);
        return ext;
    }

}
