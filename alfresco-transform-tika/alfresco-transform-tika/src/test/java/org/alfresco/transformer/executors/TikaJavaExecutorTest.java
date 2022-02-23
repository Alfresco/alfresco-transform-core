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

import static org.alfresco.transformer.executors.Tika.NOT_EXTRACT_BOOKMARKS_TEXT;
import static org.alfresco.transformer.executors.Tika.TARGET_ENCODING;
import static org.alfresco.transformer.executors.Tika.TARGET_MIMETYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TikaJavaExecutorTest {


    @Test
    public void testNotExtractBookmarkTextDefault() throws Exception
    {
        TikaJavaExecutor executorSpyDefaultTrue = spy(new TikaJavaExecutor(true));
        TikaJavaExecutor executorSpyDefaultFalse = spy(new TikaJavaExecutor(false));

        File mockSourceFile = mock(File.class);
        File mockTargetFile = mock(File.class);
        String transformName = "transformName";
        String sourceMimetype = "sourceMimetype";
        String targetMimetype = "targetMimetype";
        String defaultEncoding = "UTF-8";

        // no need to continue execution passed here or check values as we're checking the correct params passed to this method later.
        lenient().doNothing().when(executorSpyDefaultTrue).call(any(), any(), any(), any(), any(), any(), any());
        lenient().doNothing().when(executorSpyDefaultFalse).call(any(), any(), any(), any(), any(), any(), any());

        Map<String, String> transformOptions = new HashMap<String,String>();

        // use empty transformOptions to test defaults
        executorSpyDefaultTrue.transform(transformName, sourceMimetype, targetMimetype, transformOptions,
                mockSourceFile, mockTargetFile);
        executorSpyDefaultFalse.transform(transformName, sourceMimetype, targetMimetype, transformOptions,
                mockSourceFile, mockTargetFile);

        // when default set to true, with no options passed we should get a call method with NOT_EXTRACT_BOOKMARKS_TEXT
        verify(executorSpyDefaultTrue, times(1)).call(mockSourceFile, mockTargetFile, transformName, null,
                NOT_EXTRACT_BOOKMARKS_TEXT, TARGET_MIMETYPE + targetMimetype, TARGET_ENCODING + defaultEncoding);

        // when default set to false, with no options passed we should get a call method without NOT_EXTRACT_BOOKMARKS_TEXT
        verify(executorSpyDefaultFalse, times(1)).call(mockSourceFile, mockTargetFile, transformName, null, null,
                TARGET_MIMETYPE + targetMimetype, TARGET_ENCODING + defaultEncoding);

        // use transforms with notExtractBookmarksText set to true
        clearInvocations(executorSpyDefaultTrue, executorSpyDefaultFalse);
        transformOptions.put("notExtractBookmarksText", "true");
        executorSpyDefaultTrue.transform(transformName, sourceMimetype, targetMimetype, transformOptions,
                mockSourceFile, mockTargetFile);
        executorSpyDefaultFalse.transform(transformName, sourceMimetype, targetMimetype, transformOptions,
                mockSourceFile, mockTargetFile);

        // both call methods should have NOT_EXTRACT_BOOKMARKS_TEXT
        verify(executorSpyDefaultTrue, times(1)).call(mockSourceFile, mockTargetFile, transformName, null,
                NOT_EXTRACT_BOOKMARKS_TEXT, TARGET_MIMETYPE + targetMimetype, TARGET_ENCODING + defaultEncoding);

        verify(executorSpyDefaultFalse, times(1)).call(mockSourceFile, mockTargetFile, transformName, null,
                NOT_EXTRACT_BOOKMARKS_TEXT, TARGET_MIMETYPE + targetMimetype, TARGET_ENCODING + defaultEncoding);

        // use transforms with notExtractBookmarksText set to false
        clearInvocations(executorSpyDefaultTrue, executorSpyDefaultFalse);
        transformOptions.replace("notExtractBookmarksText", "true", "false");
        executorSpyDefaultTrue.transform(transformName, sourceMimetype, targetMimetype, transformOptions, mockSourceFile, mockTargetFile);
        executorSpyDefaultFalse.transform(transformName, sourceMimetype, targetMimetype, transformOptions, mockSourceFile, mockTargetFile);

        // both call methods should have NOT_EXTRACT_BOOKMARKS_TEXT
        verify(executorSpyDefaultTrue, times(1)).call(mockSourceFile, mockTargetFile, transformName, null, null,
                TARGET_MIMETYPE + targetMimetype, TARGET_ENCODING + defaultEncoding);

        verify(executorSpyDefaultFalse, times(1)).call(mockSourceFile, mockTargetFile, transformName, null, null,
                TARGET_MIMETYPE + targetMimetype, TARGET_ENCODING + defaultEncoding);

        // use full set of pdfbox transformOptions just to be safe
        clearInvocations(executorSpyDefaultTrue, executorSpyDefaultFalse);
        transformOptions.put("targetEncoding", "anyEncoding");
        executorSpyDefaultTrue.transform(transformName, sourceMimetype, targetMimetype, transformOptions, mockSourceFile, mockTargetFile);
        executorSpyDefaultFalse.transform(transformName, sourceMimetype, targetMimetype, transformOptions, mockSourceFile, mockTargetFile);

        // both call methods should have NOT_EXTRACT_BOOKMARKS_TEXT but the encoding will change
        verify(executorSpyDefaultTrue, times(1)).call(mockSourceFile, mockTargetFile, transformName, null, null,
                TARGET_MIMETYPE + targetMimetype, TARGET_ENCODING + "anyEncoding");

        verify(executorSpyDefaultFalse, times(1)).call(mockSourceFile, mockTargetFile, transformName, null, null,
                TARGET_MIMETYPE + targetMimetype, TARGET_ENCODING + "anyEncoding");
    }
}
