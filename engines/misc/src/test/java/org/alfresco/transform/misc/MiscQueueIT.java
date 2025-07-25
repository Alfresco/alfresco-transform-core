/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.misc;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_HTML;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;

import java.util.UUID;

import org.alfresco.transform.base.messaging.AbstractQueueIT;
import org.alfresco.transform.client.model.TransformRequest;

public class MiscQueueIT extends AbstractQueueIT
{
    @Override
    protected TransformRequest buildRequest()
    {
        return TransformRequest
                .builder()
                .withRequestId(UUID.randomUUID().toString())
                .withSourceMediaType(MIMETYPE_HTML)
                .withTargetMediaType(MIMETYPE_TEXT_PLAIN)
                .withTargetExtension("txt")
                .withSchema(1)
                .withClientData("ACS")
                .withSourceReference(UUID.randomUUID().toString())
                .withSourceSize(32L)
                .withInternalContextForTransformEngineTests()
                .build();
    }
}
