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
 * Wraps a source that arrives over the network - a Direct Access URL - and remembers whether the download itself failed, as opposed to the content being unreadable.
 * <p/>
 * The distinction matters because the bytes are pulled lazily, <i>inside</i> the transform. A download that dies half way through therefore surfaces as whatever the parser makes of a half file: PDFBox reports {@code "Error: End-of-File, expected line"}, zip-backed formats report {@code EOFException} or {@code ZipException}. Those are indistinguishable, by exception alone, from a genuinely corrupt document - and they are on the classifier's content list, so without this the transform would be reported as {@code 422} and never retried, when a retry is exactly what it needed.
 * <p/>
 * Only facts about the stream are recorded, never anything about how much of it the transform chose to read. A parser that rejects a bad header after 10 bytes has not been truncated; a stream that threw while being read, or that ended before the size the client said the content was, has been.
 * <p/>
 * What is counted is the <i>logical position in the source</i>, not the number of bytes handed out. Those differ, because mark/reset is preserved from the buffered delegate and parsers use it: Tika's office detector marks, reads the first few bytes, resets and reads them again. Summing every read would count those bytes twice and could carry the total past {@link #expectedLength} on a source that was actually truncated, which would report a failed download as a readable one - the exact mistake this class exists to prevent.
 */
public class RemoteSourceStream extends FilterInputStream implements SourceDownloadStatus
{
    /** Size the client said the content is, or a negative value when it did not say. */
    private final long expectedLength;

    /** Logical offset in the source. Advanced by reads and skips, rewound by {@link #reset()}. */
    private long position;

    /** Furthest {@link #position} ever reached, which is how much of the source actually arrived. */
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

    /**
     * Rewinds the logical position as well as the delegate, so bytes read again after a reset are not counted twice. {@link #highWaterMark} is deliberately left where it was: it records how much of the source arrived, which re-reading cannot change.
     */
    @Override
    public synchronized void reset() throws IOException
    {
        in.reset();
        position = markedPosition;
        // A reset undoes the read that hit the end of the source, so what follows is no longer at the end.
        endOfStreamReached = false;
    }
}
