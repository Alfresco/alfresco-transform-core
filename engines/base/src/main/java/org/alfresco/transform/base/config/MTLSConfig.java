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
package org.alfresco.transform.base.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
public class MTLSConfig {

    @Value("${filestore-url}")
    private String url;

    @Value("${server.ssl.enabled:false}")
    boolean sslEnabled;

    @Value("${server.ssl.key.store:}")
    private Resource keyStoreResource;

    @Value("${server.ssl.key.password:}")
    private char[] keyPassword;

    @Value("${server.ssl.key.store.password:}")
    private char[] keyStorePassword;

    @Value("${server.ssl.key.store.type:}")
    private String keyStoreType;

    @Value("${server.ssl.trust.store:}")
    private Resource trustStoreResource;

    @Value("${server.ssl.trust.store.password:}")
    private char[] trustStorePassword;

    @Value("${server.ssl.trust.store.type:}")
    private String trustStoreType;

    @Bean
    public WebClient client(WebClient.Builder clientBuilder)
    {
            return clientBuilder.baseUrl(url.endsWith("/") ? url : url + "/")
                    .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .defaultHeader(ACCEPT, APPLICATION_JSON_VALUE)
                    .build();
    }

    @Bean
    public WebClient.Builder clientBuilder() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        if(sslEnabled)
        {
            HttpClient httpClient = getHttpClientWithMTLS();
            return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
        } else {
            return WebClient.builder();
        }
    }

    private HttpClient getHttpClientWithMTLS() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        KeyManagerFactory keyManagerFactory = initKeyManagerFactory();
        TrustManagerFactory trustManagerFactory = initTrustManagerFactory();

        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(trustManagerFactory)
                .keyManager(keyManagerFactory)
                .build();

        return HttpClient.create().secure(p -> p.sslContext(sslContext));
    }

    private TrustManagerFactory initTrustManagerFactory() throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException
    {
        KeyStore trustStore = getKeyStore(trustStoreType, trustStoreResource, trustStorePassword);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        return trustManagerFactory;
    }

    private KeyManagerFactory initKeyManagerFactory() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        KeyStore clientKeyStore = getKeyStore(keyStoreType, keyStoreResource, keyStorePassword);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, keyPassword);
        return keyManagerFactory;
    }

    private KeyStore getKeyStore(String keyStoreType, Resource keyStoreResource, char[] keyStorePassword) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
    {
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        try (InputStream keyStoreInputStream = keyStoreResource.getInputStream())
        {
            keyStore.load(keyStoreInputStream, keyStorePassword);
        }
        return keyStore;
    }

    @Bean
    public RestTemplate restTemplate() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException
    {
        if(sslEnabled)
        {
            return getRestTemplateWithMTLS();
        } else {
            return new RestTemplate();
        }
    }

    private RestTemplate getRestTemplateWithMTLS() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException
    {
        KeyStore keyStore = getKeyStore(keyStoreType, keyStoreResource, keyStorePassword);
        SSLContext sslContext = new SSLContextBuilder()
                .loadKeyMaterial(keyStore, keyPassword)
                .loadTrustMaterial(trustStoreResource.getURL(), trustStorePassword)
                .build();

        SSLConnectionSocketFactory sslContextFactory = new SSLConnectionSocketFactory(sslContext);
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslContextFactory).build();
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(requestFactory);
    }
}
