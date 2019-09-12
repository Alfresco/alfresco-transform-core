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

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;

/**
 * Tests TikaController with a server test harness.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class TikaHttpRequestTest extends AbstractHttpRequestTest
{
    @Override
    protected String getTransformerName()
    {
        return "Tika";
    }

    @Override
    protected String getSourceExtension()
    {
        return "pdf";
    }

    // Override method as Tika requires sourceMimetype
    // If not provided then sourceMimetype request parameter error will be thrown.
    @Override
    protected void assertTransformError(boolean addFile, String errorMessage)
    {
        LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        if (addFile)
        {
            parameters.add("file", new ClassPathResource("quick." + getSourceExtension()));
        }
        parameters.add("sourceMimetype", "application/pdf");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MULTIPART_FORM_DATA);
        HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters,
            headers);
        super.sendTranformationRequest(entity, errorMessage);
    }
}
