/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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
package org.alfresco.transformer;

import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_WORDPROCESSING;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_PDF;

import java.util.UUID;

import org.alfresco.transform.client.model.TransformRequest;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Lucian Tuca
 * created on 15/01/2019
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = {"activemq.url=nio://localhost:61616"})
public class AlfrescoPdfRendererQueueTransformServiceIT extends AbstractQueueTransformServiceIT
{
    @Override
    protected TransformRequest buildRequest()
    {
        return TransformRequest
            .builder()
            .withRequestId(UUID.randomUUID().toString())
            .withSourceMediaType(MIMETYPE_OPENXML_WORDPROCESSING)
            .withTargetMediaType(MIMETYPE_PDF)
            .withTargetExtension("pdf")
            .withSchema(1)
            .withClientData("ACS")
            .withSourceReference(UUID.randomUUID().toString())
            .withSourceSize(32L).build();
    }
}
