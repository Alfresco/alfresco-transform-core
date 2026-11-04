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
 * Proves the {@code additional-*} properties are bound by Spring and merge with the shipped defaults rather than replacing them.
 * <p/>
 * This is the form an engine or an extension is expected to use. {@code TransformExceptionClassifierBindingTest} covers the replacing form, which is for a deployment that needs to remove an entry rather than add one.
 */
@SpringBootTest(classes = {org.alfresco.transform.base.Application.class},
        properties = {
                "transform.failure.classification.additional-class-names=MessagingException",
                "transform.failure.classification.additional-message-fragments=Unreadable Glyph Table",
                "transform.failure.classification.additional-message-fragment-class-names=IllegalStateException",
                "transform.failure.classification.additional-infrastructure-class-names=ArithmeticException",
                "transform.failure.classification.additional-infrastructure-message-fragments=Engine Is Unwell"})
class TransformExceptionClassifierAdditionalBindingTest
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

    /**
     * The point of the whole property. Adding one name must leave every shipped rule working, or an engine that contributed a signature would quietly disable the rest.
     */
    @Test
    void testTheShippedDefaultsStillMatchAlongsideTheAdditions()
    {
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("corrupt entry")));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(
                new IOException("Missing root object specification in trailer.")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IOException("corrupt", new java.net.SocketTimeoutException("Read timed out"))));
    }

    /**
     * Stands in for {@code jakarta.mail.MessagingException}, which {@code engines/base} does not depend on. Matching is by class name, so a local class of the same simple name exercises the real rule.
     */
    private static final class MessagingException extends Exception
    {
        private static final long serialVersionUID = 1L;

        MessagingException(String message)
        {
            super(message);
        }
    }
}
