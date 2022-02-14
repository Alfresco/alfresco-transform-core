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
package org.alfresco.transformer;

import static org.alfresco.transform.client.util.RequestParamMap.DIRECT_ACCESS_URL;
import static org.alfresco.transform.client.util.RequestParamMap.ENDPOINT_TRANSFORM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

/**
 * Super class for testing controllers with a server. Includes tests for the AbstractTransformerController itself.
 * Note: Currently uses json rather than HTML as json is returned by this spring boot test harness.
 */
public abstract class AbstractHttpRequestTest
{
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    protected abstract String getTransformerName();

    protected abstract String getSourceExtension();

    @Test
    public void testPageExists()
    {
        String result = restTemplate.getForObject("http://localhost:" + port + "/", String.class);

        String title = getTransformerName() + ' ' + "Test Transformation";
        assertTrue("\"" + title + "\" should be part of the page title", result.contains(title));
    }

    @Test
    public void logPageExists()
    {
        String result = restTemplate.getForObject("http://localhost:" + port + "/log",
            String.class);

        String title = getTransformerName() + ' ' + "Log";
        assertTrue("\"" + title + "\" should be part of the page title", result.contains(title));
    }

    @Test
    public void errorPageExists()
    {
        String result = restTemplate.getForObject("http://localhost:" + port + "/error",
            String.class);

        String title = getTransformerName() + ' ' + "Error Page";
        assertTrue("\"" + title + "\" should be part of the page title",
            result.contains("Error Page"));
    }

    @Test
    public void noFileError()
    {
        LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add("targetExtension", ".tmp");

        assertTransformError(false,
                getTransformerName() + " - Required request part 'file' is not present",
                parameters);
    }

    @Test
    public void noTargetExtensionError()
    {
        assertMissingParameter("targetExtension");
    }

    private void assertMissingParameter(String name)
    {
        assertTransformError(true,
            getTransformerName() + " - Request parameter '" + name + "' is missing", null);
    }

    protected void assertTransformError(boolean addFile,
                                        String errorMessage,
                                        LinkedMultiValueMap<String, Object> additionalParams)
    {
        LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        if (addFile)
        {
            parameters.add("file",
                new org.springframework.core.io.ClassPathResource("quick." + getSourceExtension()));
        }
        if (additionalParams != null)
        {
            parameters.addAll(additionalParams);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MULTIPART_FORM_DATA);
        HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters,
            headers);

        sendTranformationRequest(entity, errorMessage);
    }

    @Test
    public void httpTransformRequestDirectAccessUrlNotFoundTest()
    {
        String directUrl = "https://expired/direct/access/url";

        LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add("targetExtension", ".tmp");
        parameters.add(DIRECT_ACCESS_URL, directUrl);

        assertTransformError(false,
                getTransformerName() + " - Direct Access Url not found.",
                parameters);

    }

    protected void sendTranformationRequest(
        final HttpEntity<LinkedMultiValueMap<String, Object>> entity, final String errorMessage)
    {
        final ResponseEntity<String> response = restTemplate.exchange(ENDPOINT_TRANSFORM, POST, entity,
            String.class, "");
        assertEquals(errorMessage, getErrorMessage(response.getBody()));
    }

    // Strip out just the error message from the returned json content body
    // Had been expecting the Error page to be returned, but we end up with the json in this test harness.
    // Is correct if run manually, so not worrying too much about this.
    private String getErrorMessage(String content)
    {
        String message = "";
        int i = content.indexOf("\"message\":\"");
        if (i != -1)
        {
            int j = content.indexOf("\",\"path\":", i);
            if (j != -1)
            {
                message = content.substring(i + 11, j);
            }
        }
        return message;
    }
}
