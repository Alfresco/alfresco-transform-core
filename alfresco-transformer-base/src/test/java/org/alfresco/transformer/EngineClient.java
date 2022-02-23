/*
 *  Copyright 2015-2022 Alfresco Software, Ltd.  All rights reserved.
 *
 *  License rights for this program may be obtained from Alfresco Software, Ltd.
 *  pursuant to a written agreement and any use of this program without such an
 *  agreement is prohibited.
 */
package org.alfresco.transformer;

import static java.util.Collections.emptyMap;
import static org.alfresco.transform.client.util.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import java.util.Map;

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
public class EngineClient
{
    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    public static ResponseEntity<Resource> sendTRequest(
        final String engineUrl, final String sourceFile,
        final String sourceMimetype, final String targetMimetype, final String targetExtension)
    {
        return sendTRequest(engineUrl, sourceFile, sourceMimetype, targetMimetype, targetExtension,
            emptyMap());
    }

    public static ResponseEntity<Resource> sendTRequest(
        final String engineUrl, final String sourceFile,
        final String sourceMimetype, final String targetMimetype, final String targetExtension,
        final Map<String, String> transformOptions)
    {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MULTIPART_FORM_DATA);
        //headers.setAccept(ImmutableList.of(MULTIPART_FORM_DATA));

        final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ClassPathResource(sourceFile));
        if (sourceMimetype != null && !sourceMimetype.trim().isEmpty())
        {
            body.add("sourceMimetype", sourceMimetype);
        }
        if (targetMimetype != null && !targetMimetype.trim().isEmpty())
        {
            body.add("targetMimetype", targetMimetype);
        }
        if (targetExtension != null && !targetExtension.trim().isEmpty())
        {
            body.add("targetExtension", targetExtension);
        }
        transformOptions.forEach(body::add);

        final HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        return REST_TEMPLATE.postForEntity(engineUrl + ENDPOINT_TRANSFORM, entity, Resource.class);
    }
}
