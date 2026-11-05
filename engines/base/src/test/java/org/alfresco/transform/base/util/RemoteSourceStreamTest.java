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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

/**
 * Distinguishes "the download did not finish" (retryable 500) from "the transform stopped reading" (may be 422).
 */
class RemoteSourceStreamTest
{
    private static RemoteSourceStream stream(byte[] content, long expectedLength)
    {
        return new RemoteSourceStream(new BufferedInputStream(new ByteArrayInputStream(content)), expectedLength);
    }

    private static byte[] bytes(int length)
    {
        byte[] content = new byte[length];
        for (int i = 0; i < length; i++)
        {
            content[i] = (byte) i;
        }
        return content;
    }

    /** Reads to the end a byte at a time, so a test's {@link RemoteSourceStream#mark(int)} stays within the read limit. */
    private static void drain(InputStream in) throws IOException
    {
        int read = in.read();
        while (read != -1)
        {
            read = in.read();
        }
    }

    @Test
    void testAFullyReadSourceHasNotFailed() throws IOException
    {
        try (RemoteSourceStream stream = stream(bytes(10), 10))
        {
            drain(stream);

            assertFalse(stream.sourceDownloadFailed());
        }
    }

    @Test
    void testASourceThatEndedEarlyHasFailed() throws IOException
    {
        try (RemoteSourceStream stream = stream(bytes(6), 10))
        {
            drain(stream);

            assertTrue(stream.sourceDownloadFailed());
            assertTrue(stream.describeFailure().contains("6 of the 10"), stream.describeFailure());
        }
    }

    /** A parser rejecting a bad header early has not been truncated. */
    @Test
    void testAbandoningAGoodSourceEarlyIsNotAFailure() throws IOException
    {
        try (RemoteSourceStream stream = stream(bytes(10), 10))
        {
            assertEquals(0, stream.read());

            assertFalse(stream.sourceDownloadFailed());
        }
    }

    /** Tika's office detector marks, reads, resets and re-reads; a cumulative count would overstate what arrived. */
    @Test
    void testRereadingAfterAResetDoesNotHideATruncatedSource() throws IOException
    {
        try (RemoteSourceStream stream = stream(bytes(6), 10))
        {
            stream.mark(16);
            assertEquals(4, stream.read(new byte[4], 0, 4));
            stream.reset();

            drain(stream);

            assertTrue(stream.sourceDownloadFailed(),
                    "Only 6 of the 10 bytes arrived; re-reading 4 of them must not make up the difference");
            assertTrue(stream.describeFailure().contains("6 of the 10"), stream.describeFailure());
        }
    }

    @Test
    void testRereadingAfterAResetDoesNotInventAFailureOnACompleteSource() throws IOException
    {
        try (RemoteSourceStream stream = stream(bytes(10), 10))
        {
            stream.mark(16);
            assertEquals(4, stream.read(new byte[4], 0, 4));
            stream.reset();

            drain(stream);

            assertFalse(stream.sourceDownloadFailed());
        }
    }

    @Test
    void testAResetAfterTheEndOfTheSourceIsNotTreatedAsTheEnd() throws IOException
    {
        try (RemoteSourceStream stream = stream(bytes(10), 10))
        {
            stream.mark(16);
            drain(stream);
            stream.reset();

            assertFalse(stream.sourceDownloadFailed());
        }
    }

    @Test
    void testSkippedBytesCountAsArrived() throws IOException
    {
        try (RemoteSourceStream stream = stream(bytes(10), 10))
        {
            assertEquals(4, stream.skip(4));
            drain(stream);

            assertFalse(stream.sourceDownloadFailed());
        }
    }

    @Test
    void testAReadThatThrowsIsAFailureHoweverMuchArrived() throws IOException
    {
        try (RemoteSourceStream stream = new RemoteSourceStream(new InputStream() {
            @Override
            public int read() throws IOException
            {
                throw new IOException("Connection reset");
            }
        }, -1))
        {
            assertThrows(IOException.class, stream::read);

            assertTrue(stream.sourceDownloadFailed());
            assertTrue(stream.describeFailure().contains("Connection reset"), stream.describeFailure());
        }
    }

    @Test
    void testAnUnknownExpectedLengthNeverReportsATruncatedSource() throws IOException
    {
        try (RemoteSourceStream stream = stream(bytes(6), -1))
        {
            drain(stream);

            assertFalse(stream.sourceDownloadFailed());
        }
    }
}
