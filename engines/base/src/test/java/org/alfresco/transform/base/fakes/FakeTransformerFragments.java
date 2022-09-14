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
package org.alfresco.transform.base.fakes;

import org.alfresco.transform.base.TransformManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Returns lines in the supplied input as a sequence of transform result fragments.
 * - If the current line is {@code "Null"} no output is made and {@code null} is passed as the {@code index} to
 *   {@link TransformManager#respondWithFragment(Integer, boolean)}. The {code finished} parameter is unset.
 * - If {@code "Finished"}, the text is written and the {code finished} parameter is set.
 * - If the current line is {@code "NullFinished"} no output is made and {@code null} is passed as the {@code index} to
 *   {@code respondWithFragment}. The {code finished} parameter is set.
 * - If {@code "Ignored"} it will be written to the output, but the {@code respondWithFragment} method will not be
 *   called, so should be ignored if the final line.
 * If the input is "WithoutFragments", {@code respondWithFragment} is not called.
 */
public class FakeTransformerFragments extends AbstractFakeTransformer
{
    @Override
    public void transform(String sourceMimetype, InputStream inputStream, String targetMimetype,
        OutputStream outputStream, Map<String, String> transformOptions, TransformManager transformManager)
        throws Exception
    {
        String input = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        String[] lines = input.split("\n");
        if ("WithoutFragments".equals(input))
        {
            write(outputStream, input);
        }
        else
        {
            for (int i = 0; i < lines.length; i++)
            {
                String line = lines[i];
                Integer index = "Null".equals(line) || "NullFinished".equals(line) ? null : i;
                boolean finished = "Finished".equals(line) || "NullFinished".equals(line);
                if (index != null)
                {
                    write(outputStream, line);
                }
                if (!"Ignored".equals(line)) {
                    outputStream = transformManager.respondWithFragment(index, finished);
                }
            }
        }
    }

    private void write(OutputStream outputStream, String text) throws IOException
    {
        if (outputStream != null)
        {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            outputStream.write(bytes, 0, bytes.length);
        }
    }
}
