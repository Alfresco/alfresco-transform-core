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

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.io.CharConversionException;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipException;
import javax.imageio.IIOException;

import org.alfresco.transform.exceptions.TransformException;

/**
 * For transformers whose library rejected a document, but may have wrapped a failed read in that rejection.
 */
public final class ContentFailures
{
    /** {@link IOException}s that describe the bytes read, never the reading; a failed read beneath them is still found. */
    private static final List<Class<? extends IOException>> CONTENT_IO_FAILURES = List.of(
            ZipException.class, EOFException.class, CharConversionException.class, CharacterCodingException.class,
            IIOException.class);

    private ContentFailures()
    {}

    /**
     * @return a {@code 422} for {@code e}, unless another {@link IOException} caused it: that may be the environment, so is left to the classifier.
     */
    public static RuntimeException unreadableDocument(String message, Throwable e)
    {
        Set<Throwable> seen = new HashSet<>();
        seen.add(e);
        for (Throwable cause = e.getCause(); cause != null && seen.add(cause); cause = cause.getCause())
        {
            if (isFailedRead(cause))
            {
                return new IllegalStateException(message, e);
            }
        }
        return new TransformException(UNPROCESSABLE_ENTITY, message, e);
    }

    private static boolean isFailedRead(Throwable t)
    {
        return t instanceof IOException && CONTENT_IO_FAILURES.stream().noneMatch(type -> type.isInstance(t));
    }
}
