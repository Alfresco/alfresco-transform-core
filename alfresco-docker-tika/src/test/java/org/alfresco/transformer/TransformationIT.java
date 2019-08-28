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

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

/**
 * @author Cezar Leahu
 */
public class TransformationIT
{
    private static final Logger logger = LoggerFactory.getLogger(TransformationIT.class);
    private static final String ENGINE_URL = "http://localhost:8090";
    private static final Map<String,String> extensionMimetype = ImmutableMap.of("html", "text/html",
                                                                                "txt", "text/plain",
                                                                                "xhtml", "application/xhtml+xml",
                                                                                "xml", "text/xml");

    @Test
    public void testDoc()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.doc", em, extensionMimetype.get(em), "Office");
    }

    @Test
    public void testDocx()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.docx", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testHtml()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.html", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testJar()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.jar", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testJava()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.java", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testKey()
    {
        sendTRequest("quick.key", "html", "text/html", "TikaAuto");
        // Does not work, alfresco-docker-transform-misc can handle this target mimetype, removed from engine_config.json
        // sendTRequest("quick.key", "txt", "text/plain", "TikaAuto");
        sendTRequest("quick.key", "xhtml", "application/xhtml+xml", "TikaAuto");
        sendTRequest("quick.key", "xml", "text/xml", "TikaAuto");

    }

    @Test
    public void testMsg()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.msg", em, extensionMimetype.get(em), "OutlookMsg");
    }

    @Test
    public void testNumbers()
    {
        sendTRequest("quick.numbers", "html", "text/html", "TikaAuto");
        // Does not work, alfresco-docker-transform-misc can handle this target mimetype, removed from engine_config.json
        // sendTRequest("quick.numbers", "txt", "text/plain", "TikaAuto");
        sendTRequest("quick.numbers", "xhtml", "application/xhtml+xml", "TikaAuto");
        sendTRequest("quick.numbers", "xml", "text/xml", "TikaAuto");
    }

    @Test
    public void testOdp()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.odp", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testOds()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.ods", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testOdt()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.odt", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testOtp()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.otp", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testOts()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.ots", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testOtt()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.ott", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testPages()
    {
        sendTRequest("quick.pages", "html", "text/html", "TikaAuto");
        // Does not work, alfresco-docker-transform-misc can handle this target mimetype, removed from engine_config.json
        // sendTRequest("quick.pages", "txt", "text/plain", "TikaAuto");
        sendTRequest("quick.pages", "xhtml", "application/xhtml+xml", "TikaAuto");
        sendTRequest("quick.pages", "xml", "text/xml", "TikaAuto");
    }

    @Test
    public void testPdf()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.pdf", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testPpt()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.ppt", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testPptx()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.pptx", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testSxw()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.sxw", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testTxt()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.txt", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testVsd()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.vsd", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testXls()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.xls", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testXslx()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.xslx", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testZip()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.zip", em, extensionMimetype.get(em), "TikaAuto");
    }

    @Test
    public void testZipArchive()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.zip", em, extensionMimetype.get(em), "Archive");
    }

    @Test
    public void testJarArchive()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.jar", em, extensionMimetype.get(em), "Archive");
    }

    @Test
    public void testTarArchive()
    {
        Set<String> keySet = extensionMimetype.keySet();
        for (String em : keySet)
            sendTRequest("quick.tar", em, extensionMimetype.get(em), "Archive");
    }

    private static void sendTRequest(final String sourceFile, final String targetExtension, final String targetMimetype, final String transform)
    {
        final RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ClassPathResource(sourceFile));
        body.add("targetExtension", targetExtension);
        body.add("targetMimetype", targetMimetype);
        body.add("targetEncoding", "UTF-8");
        body.add("transform", transform);

        final HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        final ResponseEntity<Resource> response = restTemplate.postForEntity(
            ENGINE_URL + "/transform",
            entity, Resource.class);

        logger.info("Response: {}", response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
