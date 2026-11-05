/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2026 Alfresco Software Limited
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps a network source (Direct Access URL) and records whether the download failed or was truncated.
 * <p/>
 * A truncated download looks corrupt to the parser; this lets it be retried as {@code 500} instead of failing as {@code 422}.
 * <p/>
 * Tracks the logical position, not bytes handed out, so re-reads after mark/reset are not counted twice.
 */
public class RemoteSourceStream extends FilterInputStream implements SourceDownloadStatus
{
    /** Declared content length, or negative if unknown. */
    private final long expectedLength;

    /** Logical offset in the source; rewound by {@link #reset()}. */
    private long position;

    /** Furthest {@link #position} reached, i.e. how much of the source arrived. */
    private long highWaterMark;

    /** {@link #position} recorded by the last {@link #mark(int)}. */
    private long markedPosition;

    private boolean endOfStreamReached;
    private IOException readFailure;

    public RemoteSourceStream(InputStream inputStream, long expectedLength)
    {
        super(inputStream);
        this.expectedLength = expectedLength;
    }

    @Override
    public boolean sourceDownloadFailed()
    {
        return readFailure != null
                || (endOfStreamReached && expectedLength >= 0 && highWaterMark < expectedLength);
    }

    @Override
    public String describeFailure()
    {
        if (readFailure != null)
        {
            return "the source download failed after " + highWaterMark + " bytes with "
                    + readFailure.getClass().getName() + ": " + readFailure.getMessage();
        }
        return "the source download ended after " + highWaterMark + " of the " + expectedLength + " bytes expected";
    }

    private void advance(long count)
    {
        position += count;
        if (position > highWaterMark)
        {
            highWaterMark = position;
        }
    }

    @Override
    public int read() throws IOException
    {
        try
        {
            int b = in.read();
            if (b == -1)
            {
                endOfStreamReached = true;
            }
            else
            {
                advance(1);
            }
            return b;
        }
        catch (IOException e)
        {
            readFailure = e;
            throw e;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        try
        {
            int count = in.read(b, off, len);
            if (count == -1)
            {
                endOfStreamReached = true;
            }
            else
            {
                advance(count);
            }
            return count;
        }
        catch (IOException e)
        {
            readFailure = e;
            throw e;
        }
    }

    @Override
    public long skip(long n) throws IOException
    {
        try
        {
            long skipped = in.skip(n);
            advance(skipped);
            return skipped;
        }
        catch (IOException e)
        {
            readFailure = e;
            throw e;
        }
    }

    @Override
    public synchronized void mark(int readlimit)
    {
        in.mark(readlimit);
        markedPosition = position;
    }

    /** Rewinds the logical position too; {@link #highWaterMark} is left unchanged. */
    @Override
    public synchronized void reset() throws IOException
    {
        in.reset();
        position = markedPosition;
        // The reset undoes any read that hit the end.
        endOfStreamReached = false;
    }
}
