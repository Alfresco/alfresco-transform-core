/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2025 Alfresco Software Limited
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
package org.alfresco.transform.base.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.io.IOException;
import java.util.zip.ZipException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Checks {@code contributions} bind from Spring, from more than one contributor, and extend rather than replace the defaults.
 */
@SpringBootTest(classes = {org.alfresco.transform.base.Application.class},
        properties = {
                "transform.failure.classification.contributions.engine.class-names=MessagingException",
                "transform.failure.classification.contributions.engine.message-fragments=Unreadable Glyph Table",
                "transform.failure.classification.contributions.engine.message-fragment-class-names=IllegalStateException",
                "transform.failure.classification.contributions.site.infrastructure-class-names=ArithmeticException",
                "transform.failure.classification.contributions.site.infrastructure-message-fragments=Engine Is Unwell"})
class TransformExceptionClassifierContributionBindingTest
{
    @Autowired
    private TransformExceptionClassifier classifier;

    @Test
    void testAnAddedClassNameIsBound()
    {
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new MessagingException("Missing start boundary")));
    }

    @Test
    void testAnAddedMessageFragmentIsBoundAndMatchedCaseInsensitively()
    {
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IOException("Unreadable glyph table at offset 12")));
    }

    @Test
    void testAnAddedMessageFragmentClassNameWidensWhichTypesAreRead()
    {
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IllegalStateException("the file is corrupt")));
    }

    @Test
    void testAnAddedInfrastructureRuleIsBoundAndStillVetoesAContentMatch()
    {
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IOException("corrupt entry", new ArithmeticException("/ by zero"))));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IOException("corrupt, and engine is unwell")));
    }

    @Test
    void testTheShippedDefaultsStillMatchAlongsideTheAdditions()
    {
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("corrupt entry")));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(
                new IOException("Missing root object specification in trailer.")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IOException("corrupt", new java.net.SocketTimeoutException("Read timed out"))));
    }

    /** Stands in for {@code jakarta.mail.MessagingException}; matching is by simple name. */
    private static final class MessagingException extends Exception
    {
        private static final long serialVersionUID = 1L;

        MessagingException(String message)
        {
            super(message);
        }
    }
}
