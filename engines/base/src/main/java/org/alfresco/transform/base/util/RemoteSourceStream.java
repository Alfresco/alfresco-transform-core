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
 */
public class RemoteSourceStream extends FilterInputStream
{
    /** Size the client said the content is, or a negative value when it did not say. */
    private final long expectedLength;

    private long bytesRead;
    private boolean endOfStreamReached;
    private IOException readFailure;

    public RemoteSourceStream(InputStream inputStream, long expectedLength)
    {
        super(inputStream);
        this.expectedLength = expectedLength;
    }

    /**
     * @return {@code true} if the download failed, or ended before all of the content had arrived. A transform that failed on such a stream must be reported as a server error so that it is retried, whatever the parser called the failure.
     */
    public boolean sourceDownloadFailed()
    {
        return readFailure != null
                || (endOfStreamReached && expectedLength >= 0 && bytesRead < expectedLength);
    }

    /**
     * @return a description of why the download is considered incomplete, for logging. Never {@code null} once {@link #sourceDownloadFailed()} is {@code true}.
     */
    public String describeFailure()
    {
        if (readFailure != null)
        {
            return "the source download failed after " + bytesRead + " bytes with "
                    + readFailure.getClass().getName() + ": " + readFailure.getMessage();
        }
        return "the source download ended after " + bytesRead + " of the " + expectedLength + " bytes expected";
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
                bytesRead++;
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
                bytesRead += count;
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
            bytesRead += skipped;
            return skipped;
        }
        catch (IOException e)
        {
            readFailure = e;
            throw e;
        }
    }
}
