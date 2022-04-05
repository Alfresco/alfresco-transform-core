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
package org.alfresco.transform.client.model;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * TransformRequestValidator
 * <p>
 * Transform request validator
 */
public class TransformRequestValidator implements Validator
{
    @Override
    public boolean supports(Class<?> aClass)
    {
        return aClass.isAssignableFrom(TransformRequest.class);
    }

    @Override
    public void validate(Object o, Errors errors)
    {
        final TransformRequest request = (TransformRequest) o;

        if (request == null)
        {
            errors.reject(null, "request cannot be null");
        }
        else
        {
            String requestId = request.getRequestId();
            if (requestId == null || requestId.isEmpty())
            {
                errors.rejectValue("requestId", null, "requestId cannot be null or empty");
            }
            Long sourceSize = request.getSourceSize();
            if (sourceSize == null || sourceSize <= 0)
            {
                errors.rejectValue("sourceSize", null,
                    "sourceSize cannot be null or have its value smaller than 0");
            }
            String sourceMediaType = request.getSourceMediaType();
            if (sourceMediaType == null || sourceMediaType.isEmpty())
            {
                errors.rejectValue("sourceMediaType", null,
                    "sourceMediaType cannot be null or empty");
            }
            String targetMediaType = request.getTargetMediaType();
            if (targetMediaType == null || targetMediaType.isEmpty())
            {
                errors.rejectValue("targetMediaType", null,
                    "targetMediaType cannot be null or empty");
            }
            String targetExtension = request.getTargetExtension();
            if (targetExtension == null || targetExtension.isEmpty())
            {
                errors.rejectValue("targetExtension", null,
                    "targetExtension cannot be null or empty");
            }
            String clientData = request.getClientData();
            if (clientData == null || clientData.isEmpty())
            {
                errors.rejectValue("clientData", String.valueOf(request.getSchema()),
                    "clientData cannot be null or empty");
            }
            if (request.getSchema() < 0)
            {
                errors.rejectValue("schema", String.valueOf(request.getSchema()),
                    "schema cannot be less than 0");
            }
        }
    }
}
