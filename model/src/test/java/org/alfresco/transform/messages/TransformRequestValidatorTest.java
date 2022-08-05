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
package org.alfresco.transform.messages;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_IMAGE_PNG;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.alfresco.transform.client.model.TransformRequest;
import org.junit.jupiter.api.Test;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.Errors;

/**
 * TransformRequestValidatorTest
 * <p/>
 * Unit test that checks the Transform request validation.
 */
public class TransformRequestValidatorTest
{
    private TransformRequestValidator validator = new TransformRequestValidator();

    @Test
    public void testSupports()
    {
        assertTrue(validator.supports(TransformRequest.class));
    }

    @Test
    public void testNullRequest()
    {
        Errors errors = new DirectFieldBindingResult(null, "request");
        validator.validate(null, errors);

        assertEquals(1, errors.getAllErrors().size());
        assertEquals("request cannot be null",
            errors.getAllErrors().iterator().next().getDefaultMessage());
    }

    @Test
    public void testMissingId()
    {
        TransformRequest request = new TransformRequest();
        Errors errors = new DirectFieldBindingResult(request, "request");

        validator.validate(request, errors);

        assertFalse(errors.getAllErrors().isEmpty());
        assertEquals("requestId cannot be null or empty",
            errors.getAllErrors().iterator().next().getDefaultMessage());
    }

    @Test
    public void testMissingSourceSize()
    {
        TransformRequest request = new TransformRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setSourceReference(UUID.randomUUID().toString());
        Errors errors = new DirectFieldBindingResult(request, "request");

        validator.validate(request, errors);

        assertFalse(errors.getAllErrors().isEmpty());
        assertEquals("sourceSize cannot be null or have its value smaller than 0",
            errors.getAllErrors().iterator().next().getDefaultMessage());
    }

    @Test
    public void testMissingSourceMediaType()
    {
        TransformRequest request = new TransformRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setSourceReference(UUID.randomUUID().toString());
        request.setSourceSize(32L);
        Errors errors = new DirectFieldBindingResult(request, "request");

        validator.validate(request, errors);

        assertFalse(errors.getAllErrors().isEmpty());
        assertEquals("sourceMediaType cannot be null or empty",
            errors.getAllErrors().iterator().next().getDefaultMessage());
    }

    @Test
    public void testMissingTargetMediaType()
    {
        TransformRequest request = new TransformRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setSourceReference(UUID.randomUUID().toString());
        request.setSourceSize(32L);
        request.setSourceMediaType(MIMETYPE_PDF);
        Errors errors = new DirectFieldBindingResult(request, "request");

        validator.validate(request, errors);

        assertFalse(errors.getAllErrors().isEmpty());
        assertEquals("targetMediaType cannot be null or empty",
            errors.getAllErrors().iterator().next().getDefaultMessage());
    }

    @Test
    public void testMissingTargetExtension()
    {
        TransformRequest request = new TransformRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setSourceReference(UUID.randomUUID().toString());
        request.setSourceSize(32L);
        request.setSourceMediaType(MIMETYPE_PDF);
        request.setTargetMediaType(MIMETYPE_IMAGE_PNG);
        Errors errors = new DirectFieldBindingResult(request, "request");

        validator.validate(request, errors);

        assertFalse(errors.getAllErrors().isEmpty());
        assertEquals("targetExtension cannot be null or empty",
            errors.getAllErrors().iterator().next().getDefaultMessage());
    }

    @Test
    public void testMissingClientData()
    {
        TransformRequest request = new TransformRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setSourceReference(UUID.randomUUID().toString());
        request.setSourceSize(32L);
        request.setSourceMediaType(MIMETYPE_PDF);
        request.setTargetMediaType(MIMETYPE_IMAGE_PNG);
        request.setTargetExtension("png");
        Errors errors = new DirectFieldBindingResult(request, "request");

        validator.validate(request, errors);

        assertFalse(errors.getAllErrors().isEmpty());
        assertEquals("clientData cannot be null or empty",
            errors.getAllErrors().iterator().next().getDefaultMessage());
    }

    @Test
    public void testMissingSchema()
    {
        TransformRequest request = new TransformRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setSourceReference(UUID.randomUUID().toString());
        request.setSourceSize(32L);
        request.setSourceMediaType(MIMETYPE_PDF);
        request.setTargetMediaType(MIMETYPE_IMAGE_PNG);
        request.setTargetExtension("png");
        request.setClientData("ACS");
        request.setSchema(-1);
        Errors errors = new DirectFieldBindingResult(request, "request");

        validator.validate(request, errors);

        assertFalse(errors.getAllErrors().isEmpty());
        assertEquals("schema cannot be less than 0",
            errors.getAllErrors().iterator().next().getDefaultMessage());
    }

    @Test
    public void testCompleteTransformRequest()
    {
        TransformRequest request = new TransformRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setSourceReference(UUID.randomUUID().toString());
        request.setSourceSize(32L);
        request.setSourceMediaType(MIMETYPE_PDF);
        request.setTargetMediaType(MIMETYPE_IMAGE_PNG);
        request.setTargetExtension("png");
        request.setClientData("ACS");
        Errors errors = new DirectFieldBindingResult(request, "request");

        validator.validate(request, errors);

        assertTrue(errors.getAllErrors().isEmpty());
    }
}
