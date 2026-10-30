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
 * Proves the classifier is genuinely reconfigurable from Spring configuration, which is the whole reason it is a bean rather than a static utility. Without this, {@code TransformExceptionClassifierTest} would only be exercising the setters.
 * <p/>
 * The lists are deliberately replaced rather than appended to, so this also checks that a built-in default which is <i>not</i> listed here stops matching.
 */
@SpringBootTest(classes = {org.alfresco.transform.base.Application.class},
        properties = {
                "transform.failure.classification.class-names=IllegalArgumentException",
                "transform.failure.classification.message-fragments=Unreadable Glyph Table",
                "transform.failure.classification.infrastructure-class-names=ZipException",
                "transform.failure.classification.infrastructure-message-fragments=Engine Is Unwell"})
class TransformExceptionClassifierBindingTest
{
    @Autowired
    private TransformExceptionClassifier classifier;

    @Test
    void testTheConfiguredClassNameListIsBound()
    {
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IllegalArgumentException("bad glyph table")));
    }

    @Test
    void testTheConfiguredInfrastructureListsAreBoundAndVetoAContentMatch()
    {
        // ZipException is a built-in content rule, but the configured veto must override it.
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new ZipException("invalid stored block lengths")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IllegalArgumentException("engine is unwell, giving up")));
    }

    @Test
    void testTheConfiguredMessageFragmentListIsBoundAndMatchedCaseInsensitively()
    {
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IOException("Unreadable glyph table at offset 12")));
    }

    @Test
    void testABuiltInDefaultNoLongerMatchesOnceTheListIsOverridden()
    {
        // "missing root object specification" is a built-in default, but the configuration above
        // replaced the list, so it must now fall back to the safe 500.
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(
                new IOException("Missing root object specification in trailer.")));
    }
}
