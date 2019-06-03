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

import org.alfresco.transform.client.model.Mimetype;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Content transformer which wraps the HTML Parser library for
 * parsing HTML content.
 *
 * <p>
 * This code is based on a class of the same name originally implemented in alfresco-repository.
 * </p>
 *
 * <p>
 * Since HTML Parser was updated from v1.6 to v2.1, META tags
 * defining an encoding for the content via http-equiv=Content-Type
 * will ONLY be respected if the encoding of the content item
 * itself is set to ISO-8859-1.
 * </p>
 *
 * <p>
 * Tika Note - could be converted to use the Tika HTML parser,
 *  but we'd potentially need a custom text handler to replicate
 *  the current settings around links and non-breaking spaces.
 * </p>
 *
 * @see <a href="http://htmlparser.sourceforge.net/">http://htmlparser.sourceforge.net</a>
 * @see org.htmlparser.beans.StringBean
 * @see <a href="http://sourceforge.net/tracker/?func=detail&aid=1644504&group_id=24399&atid=381401">HTML Parser</a>
 *
 * @author Derek Hulley
 * @author eknizat
 */
@Component
public class HtmlParserContentTransformer extends AbstractJavaTransformer
{
    public static final String SOURCE_ENCODING = "sourceEncoding";
    public static final String TARGET_ENCODING = "targetEncoding";
    public static final List<String> REQUIRED_OPTIONS = Arrays.asList(SOURCE_ENCODING, TARGET_ENCODING);

    @Autowired
    public HtmlParserContentTransformer(SelectingTransformer miscTransformer)
    {
        super(miscTransformer);
    }

    @Override
    public Set<String> getRequiredOptionNames()
    {
        return new HashSet<>(REQUIRED_OPTIONS);
    }

    @Override
    public boolean isTransformable(String sourceMimetype, String targetMimetype, Map<String, String> parameters)
    {
        if (!Mimetype.MIMETYPE_HTML.equals(sourceMimetype) ||
                !Mimetype.MIMETYPE_TEXT_PLAIN.equals(targetMimetype))
        {
            // only support HTML -> TEXT
            return false;
        }
        else
        {
            return true;
        }
    }

    @Override
    void transformInternal(File sourceFile, File targetFile, Map<String, String> parameters) throws Exception
    {
        // Fetch the encoding of the HTML, as set in the ContentReader
        String sourceEncoding = parameters.get(SOURCE_ENCODING);

        // Get the target encoding
        String targetEncoding = parameters.get(TARGET_ENCODING);

        // Create the extractor
        EncodingAwareStringBean extractor = new EncodingAwareStringBean();
        extractor.setCollapse(false);
        extractor.setLinks(false);
        extractor.setReplaceNonBreakingSpaces(false);
        extractor.setURL(sourceFile, sourceEncoding);
        // get the text
        String text = extractor.getStrings();

        // write it to the writer
        try (OutputStream os = new FileOutputStream(targetFile);
             Writer writer = new BufferedWriter(new OutputStreamWriter(os, targetEncoding)))
        {
            writer.write(text);
            writer.flush();
        }
    }
}
