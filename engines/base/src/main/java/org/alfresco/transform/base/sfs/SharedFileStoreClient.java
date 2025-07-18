/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2023 Alfresco Software Limited
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
package org.alfresco.transform.base.sfs;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import java.io.File;
import javax.net.ssl.SSLException;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import org.alfresco.transform.base.WebClientBuilderAdjuster;
import org.alfresco.transform.base.model.FileRefResponse;
import org.alfresco.transform.exceptions.TransformException;

/**
 * Simple Rest client that call Alfresco Shared File Store
 */
@Service
public class SharedFileStoreClient
{
    private static final Logger logger = LoggerFactory.getLogger(SharedFileStoreClient.class);

    @Value("${filestore-url}")
    private String url;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WebClientBuilderAdjuster adjuster;

    private WebClient client;

    @PostConstruct
    public void init() throws SSLException
    {
        final WebClient.Builder clientBuilder = WebClient.builder();
        adjuster.adjust(clientBuilder);
        client = clientBuilder.baseUrl(url.endsWith("/") ? url : url + "/")
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .defaultHeader(ACCEPT, APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Retrieves a file from Shared File Store using given file reference
     *
     * @param fileRef
     *            File reference
     * @return ResponseEntity<Resource>
     */
    public ResponseEntity<Resource> retrieveFile(String fileRef)
    {
        try
        {
            return restTemplate.getForEntity(url + "/" + fileRef,
                    org.springframework.core.io.Resource.class);
        }
        catch (HttpClientErrorException e)
        {
            throw new TransformException(HttpStatus.resolve(e.getStatusCode().value()), e.getMessage(), e);
        }
    }

    /**
     * Stores given file in Shared File Store
     *
     * @param file
     *            File to be stored
     * @return A FileRefResponse containing detail about file's reference
     */
    public FileRefResponse saveFile(File file)
    {
        try
        {
            FileSystemResource value = new FileSystemResource(file.getAbsolutePath());
            LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
            map.add("file", value);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MULTIPART_FORM_DATA);
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map,
                    headers);
            ResponseEntity<FileRefResponse> responseEntity = restTemplate
                    .exchange(url, POST, requestEntity, FileRefResponse.class);
            return responseEntity.getBody();
        }
        catch (HttpClientErrorException e)
        {
            throw new TransformException(HttpStatus.resolve(e.getStatusCode().value()), e.getMessage(), e);
        }
    }

    @Async
    public void asyncDelete(final String fileReference)
    {
        try
        {
            logger.debug("                  Deleting intermediate file {}", fileReference);

            client.delete().uri(fileReference)
                    .exchange().block();
        }
        catch (Exception e)
        {
            logger.error("Failed to delete intermediate file {}: {}", fileReference, e.getMessage());
        }
    }
}
