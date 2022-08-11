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
package org.alfresco.transform.base.clients;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import java.io.File;

import org.alfresco.transform.common.TransformException;
import org.alfresco.transform.base.model.FileRefResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Simple Rest client that call Alfresco Shared File Store
 */
@Service
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
            throw new TransformException(e.getStatusCode(), e.getMessage(), e);
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
            headers.setContentType(MULTIPART_FORM_DATA);
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map,
                headers);
            ResponseEntity<FileRefResponse> responseEntity = restTemplate
                .exchange(fileStoreUrl, POST, requestEntity, FileRefResponse.class);
            return responseEntity.getBody();
        }
        catch (HttpClientErrorException e)
        {
            throw new TransformException(e.getStatusCode(), e.getMessage(), e);
        }
    }
}
