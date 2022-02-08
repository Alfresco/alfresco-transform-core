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
package org.alfresco.transformer.clients;

import org.alfresco.transform.exceptions.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Simple Rest client that call Alfresco Access Url
 */
public class AlfrescoDirectAccessUrlClient
{
    @Autowired
    private RestTemplate restTemplate;

    /**
     * Sending get request for a file via Direct Access Url.
     *
     * @param directUrl Direct Access Url
     * @return ResponseEntity<Resource>
     */
    public ResponseEntity<Resource> getContentViaDirectUrl(String directUrl)
    {
        try
        {
            return restTemplate.getForEntity(directUrl,
                    Resource.class);
        } catch (HttpClientErrorException e)
        {
            throw new TransformException(e.getStatusCode().value(), e.getMessage(), e);
        }
    }
}
