/*
 * #%L
 * Alfresco Enterprise Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ImageMagickQueueTransformServiceTest extends AbstractQueueTransformServiceIT
{
    @Override
    protected TransformRequest buildRequest()
    {
        return TransformRequest.builder()
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
