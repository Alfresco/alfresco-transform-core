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
package org.alfresco.transform.base;

import org.alfresco.transform.base.fakes.FakeTransformEngineWithTwoCustomTransformers;
import org.alfresco.transform.base.fakes.FakeTransformerPdf2Png;
import org.alfresco.transform.base.fakes.FakeTransformerTxT2Pdf;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.LinkedMultiValueMap;

import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.alfresco.transform.common.RequestParamMap.DIRECT_ACCESS_URL;
import static org.alfresco.transform.common.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.alfresco.transform.common.RequestParamMap.SOURCE_MIMETYPE;
import static org.alfresco.transform.common.RequestParamMap.TARGET_MIMETYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

/**
 * Super class with a server test harness, which talks to the TransformController using http.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes={org.alfresco.transform.base.Application.class})
@ContextConfiguration(classes = {
    FakeTransformEngineWithTwoCustomTransformers.class,
    FakeTransformerTxT2Pdf.class,
    FakeTransformerPdf2Png.class})
public class HttpRequestTest
{
    @Autowired
    private TestRestTemplate restTemplate;

    private static final HttpHeaders HEADERS = new HttpHeaders();
    static {
        HEADERS.setContentType(MULTIPART_FORM_DATA);
    }

    @Test
    public void noFileError()
    {
        LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add(SOURCE_MIMETYPE, MIMETYPE_TEXT_PLAIN);
        parameters.add(TARGET_MIMETYPE, MIMETYPE_PDF);

        ResponseEntity<String> response = restTemplate.exchange(ENDPOINT_TRANSFORM, POST,
                new HttpEntity<>(parameters, HEADERS), String.class, "");

        assertTrue(response.getBody().contains("Required request part 'file' is not present"));
    }

    @Test
    public void httpTransformRequestDirectAccessUrlNotFoundTest()
    {
        LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add(DIRECT_ACCESS_URL, "https://expired/direct/access/url");
        parameters.add(SOURCE_MIMETYPE, MIMETYPE_TEXT_PLAIN);
        parameters.add(TARGET_MIMETYPE, MIMETYPE_PDF);
        parameters.add("file", new org.springframework.core.io.ClassPathResource("quick.txt"));

        ResponseEntity<String> response = restTemplate.exchange(ENDPOINT_TRANSFORM, POST,
            new HttpEntity<>(parameters, HEADERS), String.class, "");

        assertTrue(response.getBody().contains("Direct Access Url not found."));
    }

    @Test
    public void transform()
    {
        LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add(SOURCE_MIMETYPE, MIMETYPE_TEXT_PLAIN);
        parameters.add(TARGET_MIMETYPE, MIMETYPE_PDF);
        parameters.add("file", new org.springframework.core.io.ClassPathResource("quick.txt"));

        ResponseEntity<String> response = restTemplate.exchange(ENDPOINT_TRANSFORM, POST,
            new HttpEntity<>(parameters, HEADERS), String.class, "");

        assertEquals("Original Text -> TxT2Pdf()", response.getBody());
    }
}
