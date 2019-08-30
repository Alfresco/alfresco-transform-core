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

import static java.text.MessageFormat.format;
import static org.alfresco.transformer.EngineClient.sendTRequest;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.OK;

import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.ImmutableMap;

/**
 * @author Cezar Leahu
 */
public class TransformationIT
{
    private static final Logger logger = LoggerFactory.getLogger(TransformationIT.class);
    private static final String ENGINE_URL = "http://localhost:8090";
    private static final Map<String, String> extensionMimetype = ImmutableMap.of(
        "html", "text/html",
        "txt", "text/plain",
        "xhtml", "application/xhtml+xml",
        "xml", "text/xml");

// TODO unit tests for the following file types (for which is difficult to find file samples):
//  *.ogx (application/ogg)
//  *.cpio (application/x-cpio)
//  *.cdf (application/x-netcdf) 
//  *.hdf (application/x-hdf)

    @Test
    public void testDoc()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.doc", k, v, "Office"));
    }

    @Test
    public void testDocx()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.docx", k, v, "TikaAuto"));
    }

    @Test
    public void testHtml()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.html", k, v, "TikaAuto"));
    }

    @Test
    public void testJar()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.jar", k, v, "TikaAuto"));
    }

    @Test
    public void testJava()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.java", k, v, "TikaAuto"));
    }

    @Test
    public void testKey()
    {
        checkTRequest("quick.key", "html", "text/html", "TikaAuto");
        // Does not work, alfresco-docker-transform-misc can handle this target mimetype, removed from engine_config.json
        // checkTRequest("quick.key", "txt", "text/plain", "TikaAuto");
        checkTRequest("quick.key", "xhtml", "application/xhtml+xml", "TikaAuto");
        checkTRequest("quick.key", "xml", "text/xml", "TikaAuto");
    }

    @Test
    public void testMsg()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.msg", k, v, "OutlookMsg"));
    }

    @Test
    public void testNumbers()
    {
        checkTRequest("quick.numbers", "html", "text/html", "TikaAuto");
        // Does not work, alfresco-docker-transform-misc can handle this target mimetype, removed from engine_config.json
        // checkTRequest("quick.numbers", "txt", "text/plain", "TikaAuto");
        checkTRequest("quick.numbers", "xhtml", "application/xhtml+xml", "TikaAuto");
        checkTRequest("quick.numbers", "xml", "text/xml", "TikaAuto");
    }

    @Test
    public void testOdp()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.odp", k, v, "TikaAuto"));
    }

    @Test
    public void testOds()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.ods", k, v, "TikaAuto"));
    }

    @Test
    public void testOdt()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.odt", k, v, "TikaAuto"));
    }

    @Test
    public void testOtp()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.otp", k, v, "TikaAuto"));
    }

    @Test
    public void testOts()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.ots", k, v, "TikaAuto"));
    }

    @Test
    public void testOtt()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.ott", k, v, "TikaAuto"));
    }

    @Test
    public void testPages()
    {
        checkTRequest("quick.pages", "html", "text/html", "TikaAuto");
        // Does not work, alfresco-docker-transform-misc can handle this target mimetype, removed from engine_config.json
        // checkTRequest("quick.pages", "txt", "text/plain", "TikaAuto");
        checkTRequest("quick.pages", "xhtml", "application/xhtml+xml", "TikaAuto");
        checkTRequest("quick.pages", "xml", "text/xml", "TikaAuto");
    }

    @Test
    public void testPdf()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.pdf", k, v, "TikaAuto"));
    }

    @Test
    public void testPpt()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.ppt", k, v, "TikaAuto"));
    }

    @Test
    public void testPptx()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.pptx", k, v, "TikaAuto"));
    }

    @Test
    public void testSxw()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.sxw", k, v, "TikaAuto"));
    }

    @Test
    public void testTxt()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.txt", k, v, "TikaAuto"));
    }

    @Test
    public void testVsd()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.vsd", k, v, "TikaAuto"));
    }

    @Test
    public void testXls()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.xls", k, v, "TikaAuto"));
    }

    @Test
    public void testXslx()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.xslx", k, v, "TikaAuto"));
    }

    @Test
    public void testZip()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.zip", k, v, "TikaAuto"));
    }

    @Test
    public void testZipArchive()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.zip", k, v, "Archive"));
    }

    @Test
    public void testJarArchive()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.jar", k, v, "Archive"));
    }

    @Test
    public void testTarArchive()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.tar", k, v, "Archive"));
    }

    @Test
    public void testRTF()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("sample.rtf", k, v, "TikaAuto"));
    }

    @Test
    public void testXML()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.xml", k, v, "TikaAuto"));
    }

    @Test
    public void testXHTML()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("sample.xhtml.txt", k, v, "TikaAuto"));
    }

    @Test
    public void testRSS()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("sample.rss", k, v, "TikaAuto"));
    }

    @Test
    public void testRAR()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.rar", k, v, "TikaAuto"));
    }

    @Test
    public void testTarGz()
    {
        extensionMimetype.forEach((k, v) -> checkTRequest("quick.tar.gz", k, v, "TikaAuto"));
    }

    private static void checkTRequest(final String sourceFile, final String targetExtension,
        final String targetMimetype, final String transform)
    {
        final ResponseEntity<Resource> response = sendTRequest(ENGINE_URL, sourceFile, null,
            targetMimetype, targetExtension, ImmutableMap.of(
                "targetEncoding", "UTF-8",
                "transform", transform
            ));
        final String descriptor = format("Transform ({0} -> {1}, {2}, transform={3}) failed",
            sourceFile, targetMimetype, targetExtension, transform);
        assertEquals(descriptor, OK, response.getStatusCode());
    }
}
