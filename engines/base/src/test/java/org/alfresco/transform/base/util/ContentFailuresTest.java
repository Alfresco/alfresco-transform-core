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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import static org.alfresco.transform.base.util.ContentFailures.unreadableDocument;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.util.zip.ZipException;
import javax.imageio.IIOException;

import org.junit.jupiter.api.Test;

import org.alfresco.transform.exceptions.TransformException;

class ContentFailuresTest
{
    @Test
    void testARejectionWithNoCauseIsUnprocessable()
    {
        Exception rejection = new Exception("not well formed");

        TransformException e = assertInstanceOf(TransformException.class, unreadableDocument("bad", rejection));

        assertEquals(UNPROCESSABLE_ENTITY, e.getStatus());
        assertSame(rejection, e.getCause(), "The cause lets the kill switch and the veto see the rejection");
    }

    @Test
    void testARejectionCausedByDamagedBytesIsUnprocessable()
    {
        for (IOException damage : new IOException[]{new ZipException("invalid stored block lengths"),
                new EOFException("Unexpected end of ZLIB input stream"), new MalformedInputException(1)})
        {
            RuntimeException e = unreadableDocument("bad", new Exception("rejected", damage));

            assertEquals(UNPROCESSABLE_ENTITY, assertInstanceOf(TransformException.class, e).getStatus(),
                    damage.getClass().getName());
        }
    }

    /** e.g. jakarta.mail wrapping a failed read in MessagingException. */
    @Test
    void testARejectionCausedByAFailedReadIsLeftToTheClassifier()
    {
        Exception rejection = new Exception("IOException", new IOException("Stale file handle"));

        RuntimeException e = unreadableDocument("bad", rejection);

        assertInstanceOf(IllegalStateException.class, e);
        assertSame(rejection, e.getCause());
    }

    /** ImageIO nests its own exceptions; only what lies beneath them decides. */
    @Test
    void testAFailedReadBeneathAContentTypeIsStillFound()
    {
        Exception rejection = new IIOException("Error reading PNG image data",
                new IIOException("Invalid chunk", new IOException("Input/output error")));

        assertInstanceOf(IllegalStateException.class, unreadableDocument("bad", rejection));
        assertInstanceOf(TransformException.class,
                unreadableDocument("bad", new IIOException("Error reading PNG metadata", new IIOException("Invalid chunk length"))));
    }

    @Test
    void testTheCaughtExceptionItselfIsNotTreatedAsAFailedRead()
    {
        RuntimeException e = unreadableDocument("bad", new IOException("Error reading PNG image data"));

        assertInstanceOf(TransformException.class, e);
    }

    @Test
    void testACyclicCauseChainEnds()
    {
        Exception first = new Exception("first");
        Exception second = new Exception("second", first);
        first.initCause(second);

        assertInstanceOf(TransformException.class, unreadableDocument("bad", first));
    }
}
