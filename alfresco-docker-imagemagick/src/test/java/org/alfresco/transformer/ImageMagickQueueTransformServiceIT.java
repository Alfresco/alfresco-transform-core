/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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

import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IMAGE_JPEG;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_IMAGE_PNG;

import java.util.UUID;

import org.alfresco.transform.client.model.TransformRequest;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Lucian Tuca
 * created on 15/01/2019
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = {"activemq.url=nio://localhost:61616"})
public class ImageMagickQueueTransformServiceIT extends AbstractQueueTransformServiceIT
{
    @Override
    protected TransformRequest buildRequest()
    {
        return TransformRequest
            .builder()
            .withRequestId(UUID.randomUUID().toString())
            .withSourceMediaType(MIMETYPE_IMAGE_PNG)
            .withTargetMediaType(MIMETYPE_IMAGE_JPEG)
            .withTargetExtension("jpeg")
            .withSchema(1)
            .withClientData("ACS")
            .withSourceReference(UUID.randomUUID().toString())
            .withSourceSize(32L).build();
    }
}
