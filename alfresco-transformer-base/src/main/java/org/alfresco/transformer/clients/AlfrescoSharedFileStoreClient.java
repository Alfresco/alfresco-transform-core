/*
 * Copyright 2005-2018 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.transformer.clients;

import java.io.File;

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.model.FileRefResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Simple Rest client that call Alfresco Shared File Store
 */
public class AlfrescoSharedFileStoreClient
{
    @Value("${fileStoreUrl}")
    private String fileStoreUrl;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Retrieves a file from Shared File Store using given file reference
     *
     * @param fileRef File reference
     * @return ResponseEntity<Resource>
     */
    public ResponseEntity<Resource> retrieveFile(String fileRef)
    {
        try
        {
            return restTemplate.getForEntity(fileStoreUrl + "/" + fileRef,
                org.springframework.core.io.Resource.class);
        }
        catch (HttpClientErrorException e)
        {
            throw new TransformException(e.getStatusCode().value(), e.getMessage(), e);
        }
    }

    /**
     * Stores given file in Shared File Store
     *
     * @param file File to be stored
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
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map,
                headers);
            ResponseEntity<FileRefResponse> responseEntity = restTemplate
                .exchange(fileStoreUrl, HttpMethod.POST, requestEntity, FileRefResponse.class);
            return responseEntity.getBody();
        }
        catch (HttpClientErrorException e)
        {
            throw new TransformException(e.getStatusCode().value(), e.getMessage(), e);
        }
    }
}
