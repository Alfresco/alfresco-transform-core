/*
 * #%L
 * Alfresco Transform Model
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package org.alfresco.transform.exceptions;

import org.springframework.http.HttpStatus;

public class TransformException extends RuntimeException
{
    private final HttpStatus status;

    public TransformException(HttpStatus statusCode, String message)
    {
        super(message);
        this.status = statusCode;
    }

    public TransformException(HttpStatus status, String message, Throwable cause)
    {
        super(message, cause);
        this.status = status;
    }

    @Deprecated
    public TransformException(int statusCode, String message)
    {
        this(HttpStatus.valueOf(statusCode), message);
    }

    @Deprecated
    public TransformException(int statusCode, String message, Throwable cause)
    {
        this(HttpStatus.valueOf(statusCode), message, cause);
    }

    @Deprecated
    public int getStatusCode()
    {
        return status.value();
    }

    public HttpStatus getStatus()
    {
        return status;
    }
}
