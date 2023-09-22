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
package org.alfresco.transform.base;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class MtlsTestUtils {

    private static final boolean MTLS_ENABLED = Boolean.parseBoolean(System.getProperty("test-mtls-enabled"));
    private static final boolean HOSTNAME_VERIFICATION_DISABLED = Boolean.parseBoolean(System.getProperty("test-client-disable-hostname-verification"));

    public static boolean isMtlsEnabled()
    {
        return MTLS_ENABLED;
    }

    public static boolean isHostnameVerificationDisabled()
    {
        return HOSTNAME_VERIFICATION_DISABLED;
    }

    public static CloseableHttpClient httpClientWithMtls() throws NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException, IOException, CertificateException
    {
        String keyStoreFile = System.getProperty("test-client-keystore-file");
        String keyStoreType = System.getProperty("test-client-keystore-type");
        char[] keyStorePassword = System.getProperty("test-client-keystore-password").toCharArray();
        String trustStoreFile = System.getProperty("test-client-truststore-file");
        String trustStoreType = System.getProperty("test-client-truststore-type");
        char[] trustStorePassword = System.getProperty("test-client-truststore-password").toCharArray();

        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        try (InputStream keyStoreInputStream = new FileInputStream(keyStoreFile))
        {
            keyStore.load(keyStoreInputStream, keyStorePassword);
            sslContextBuilder.loadKeyMaterial(keyStore, keyStorePassword);
        }

        File trustStore = new File(trustStoreFile);
        sslContextBuilder
                .setKeyStoreType(trustStoreType)
                .loadTrustMaterial(trustStore, trustStorePassword);

        SSLContext sslContext = sslContextBuilder.build();

        return HttpClients.custom().setConnectionManager(buildSslConnectionManager(sslContext)).build();
    }

    private static HttpClientConnectionManager buildSslConnectionManager(SSLContext sslContext) {
        final SSLConnectionSocketFactoryBuilder sslConnectionSocketFactoryBuilder =
                SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(sslContext)
                        .setTlsVersions(TLS.V_1_2, TLS.V_1_3);
        if (HOSTNAME_VERIFICATION_DISABLED) {
            sslConnectionSocketFactoryBuilder.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }
        final SSLConnectionSocketFactory sslConnectionSocketFactory = sslConnectionSocketFactoryBuilder.build();

        final Registry<ConnectionSocketFactory> sslSocketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory> create()
                        .register("https", sslConnectionSocketFactory)
                        .build();

        return new PoolingHttpClientConnectionManager(sslSocketFactoryRegistry);
    }

    public static RestTemplate restTemplateWithMtls()
    {
        ClientHttpRequestFactory requestFactory = null;
        try {
            requestFactory = new HttpComponentsClientHttpRequestFactory(httpClientWithMtls());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new RestTemplate(requestFactory);
    }

    public static RestTemplate getRestTemplate()
    {
        return MtlsTestUtils.isMtlsEnabled() ? MtlsTestUtils.restTemplateWithMtls() : new RestTemplate();
    }

    public static CloseableHttpClient getHttpClient() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        return MtlsTestUtils.isMtlsEnabled() ? MtlsTestUtils.httpClientWithMtls() : HttpClients.createDefault();
    }
}
