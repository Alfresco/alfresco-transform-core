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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * @author Cezar Leahu
 */
public class TransformationIT
{
    private static final Logger logger = LoggerFactory.getLogger(TransformationIT.class);
    private static final String ENGINE_URL = "http://localhost:8090";

    @Test
    public void testPdfToPng() throws Exception
    {
        sendTRequest("quick.pdf", "png");
    }

    private static void sendTRequest(final String sourceFile, final String targetExtension)
    {
        final RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MULTIPART_FORM_DATA);
        //headers.setAccept(MULTIPART_FORM_DATA_VALUE);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ClassPathResource(sourceFile));
        body.add("targetExtension", targetExtension);

        final HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        final ResponseEntity<Resource> response = restTemplate.postForEntity(
            ENGINE_URL + "/transform",
            entity, Resource.class);

        logger.info("Response: {}", response);
    }
}
