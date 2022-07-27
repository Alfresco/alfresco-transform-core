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
package org.alfresco.transform.aio;

import org.alfresco.transform.base.AbstractBaseTest;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.config.TransformConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;

import static org.alfresco.transform.common.RequestParamMap.CONFIG_VERSION_DEFAULT;
import static org.alfresco.transform.common.RequestParamMap.CONFIG_VERSION_LATEST;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test All-In-One.
 */
public class AIOTest extends AbstractBaseTest
{
    @Autowired
    private String coreVersion;

    @Override
    // Add extra required parameters to the request.
    protected MockHttpServletRequestBuilder mockMvcRequest(String url, MockMultipartFile sourceFile,
        String... params)
    {
        return super.mockMvcRequest(url, sourceFile, params)
                    .param("targetMimetype", targetMimetype)
                    .param("sourceMimetype", sourceMimetype);
    }

    //@Override
    protected void mockTransformCommand(String sourceExtension, String targetExtension, String sourceMimetype,
            boolean readTargetFileBytes) throws IOException
    {
    }

    //@Override
    protected void updateTransformRequestWithSpecificOptions(TransformRequest transformRequest)
    {
    }

    @Test
    public void emptyTest()
    {
        ResponseEntity<TransformConfig> responseEntity = controller.transformConfig(Integer.valueOf(CONFIG_VERSION_DEFAULT));
        responseEntity.getBody().getTransformers().forEach(transformer -> {
            assertNull(transformer.getCoreVersion(), transformer.getTransformerName() +
                    " should have had a null coreValue but was " + transformer.getCoreVersion());
        });
    }

    @Test
    public void emptyTestWithLatestVersion()
    {
        ResponseEntity<TransformConfig> responseEntity = controller.transformConfig(CONFIG_VERSION_LATEST);
        responseEntity.getBody().getTransformers().forEach(transformer -> {
            assertNotNull(transformer.getCoreVersion(), transformer.getTransformerName() +
                    " should have had a coreValue but was null. Should have been " + coreVersion);
        });
    }
}