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
package org.alfresco.transform.base.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamLengthRecorder extends FilterOutputStream
{
    private long byteCount;

    public OutputStreamLengthRecorder(OutputStream outputStream)
    {
        super(outputStream);
    }

    public long getLength()
    {
        return byteCount;
    }

    public void setByteCount(long byteCount)
    {
        this.byteCount = byteCount;
    }

    @Override
    public void write(int b) throws IOException
    {
        super.write(b);
        byteCount++;
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException
    {
        super.write(b, off, len);
    }
}
