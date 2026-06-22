/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2026 Alfresco Software Limited
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
package org.alfresco.transform.base.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.alfresco.transform.exceptions.TransformException;

/**
 * Tests for {@link FileManager#getDirectAccessUrlInputStream(String)}.
 * <p>
 * In particular guards against ACS-12053 (double percent-encoding of the
 * direct-access URL's query string), which broke S3/Azure pre-signed URLs
 * because the rebuilt URL no longer matched the original signature.
 */
class FileManagerTest
{
    private HttpServer server;
    private final AtomicReference<String> receivedRawQuery = new AtomicReference<>();
    private final AtomicReference<String> receivedRawPath = new AtomicReference<>();
    private int port;

    @BeforeEach
    void startServer() throws IOException
    {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            receivedRawPath.set(exchange.getRequestURI().getRawPath());
            receivedRawQuery.set(exchange.getRequestURI().getRawQuery());
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody())
            {
                os.write(body);
            }
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer()
    {
        if (server != null)
        {
            server.stop(0);
        }
    }

    /**
     * Reproduces ACS-12053. A pre-signed S3-style URL with a query string that already contains
     * percent-encoded characters must be forwarded verbatim; re-encoding the {@code %} sign
     * invalidates the AWS signature and yields HTTP 400 from S3.
     */
    @Test
    void preservesPercentEncodingInPreSignedUrlQuery() throws IOException
    {
        // Query taken from the ACS-12053 reproducer (truncated for the test).
        // Contains pre-encoded characters: %3B (;), %20 (space), %3D (=), %22 ("), %2F (/).
        String rawQuery = "response-content-disposition=attachment%3B%20filename%3D%22doc1.docx%22"
                + "&X-Amz-Credential=AKIA%2F20260622%2Fus-east-2%2Fs3%2Faws4_request"
                + "&X-Amz-Signature=deadbeef";
        String url = "http://127.0.0.1:" + port + "/object.bin?" + rawQuery;

        try (InputStream in = FileManager.getDirectAccessUrlInputStream(url))
        {
            in.readAllBytes();
        }

        assertEquals("/object.bin", receivedRawPath.get(),
                "Path must be forwarded verbatim");
        assertEquals(rawQuery, receivedRawQuery.get(),
                "Query string must be forwarded verbatim without re-encoding the '%' sign (ACS-12053)");
    }

    @Test
    void rejectsUnsupportedProtocol()
    {
        assertThrows(TransformException.class,
                () -> FileManager.getDirectAccessUrlInputStream("ftp://example.com/file"));
    }

    @Test
    void rejectsInvalidHost()
    {
        // host contains characters disallowed by the validator
        assertThrows(TransformException.class,
                () -> FileManager.getDirectAccessUrlInputStream("http://bad host/file"));
    }
}
