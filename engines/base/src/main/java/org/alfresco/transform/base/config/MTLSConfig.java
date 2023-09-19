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
import org.alfresco.transform.base.WebClientBuilderAdjuster;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

@Configuration
public class MTLSConfig {

    @Value("${client.ssl.key-store:#{null}}")
    private Resource keyStoreResource;

    @Value("${client.ssl.key-store-password:}")
    private char[] keyStorePassword;

    @Value("${client.ssl.key-store-type:}")
    private String keyStoreType;

    @Value("${client.ssl.trust-store:#{null}}")
    private Resource trustStoreResource;

    @Value("${client.ssl.trust-store-password:}")
    private char[] trustStorePassword;

    @Value("${client.ssl.trust-store-type:}")
    private String trustStoreType;

    @Value("${client.ssl.hostname-verification-disabled:false}")
    private boolean hostNameVerificationDisabled;

    @Bean
    public WebClientBuilderAdjuster webClientBuilderAdjuster(SslContextBuilder nettySslContextBuilder)
    {
        return builder -> {
            if(isTlsOrMtlsConfigured())
            {
                HttpClient httpClientWithSslContext = null;
                try {
                    httpClientWithSslContext = createHttpClientWithSslContext(nettySslContextBuilder);
                } catch (SSLException e) {
                    throw new RuntimeException(e);
                }
                builder.clientConnector(new ReactorClientHttpConnector(httpClientWithSslContext));
            }
        };
    }

    @Bean
    public RestTemplate restTemplate(SSLContextBuilder apacheSSLContextBuilder) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException
    {
        if(isTlsOrMtlsConfigured())
        {
            return createRestTemplateWithSslContext(apacheSSLContextBuilder);
        } else {
            return new RestTemplate();
        }
    }

    @Bean
    public SSLContextBuilder apacheSSLContextBuilder() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        if(isKeystoreConfigured())
        {
            KeyStore keyStore = getKeyStore(keyStoreType, keyStoreResource, keyStorePassword);
            sslContextBuilder.loadKeyMaterial(keyStore, keyStorePassword);
        }
        if(isTruststoreConfigured())
        {
            sslContextBuilder
                    .setKeyStoreType(trustStoreType)
                    .loadTrustMaterial(trustStoreResource.getURL(), trustStorePassword);
        }

        return sslContextBuilder;
    }

    @Bean
    public SslContextBuilder nettySslContextBuilder() throws UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
        if(isKeystoreConfigured())
        {
            KeyManagerFactory keyManagerFactory = initKeyManagerFactory();
            sslContextBuilder.keyManager(keyManagerFactory);
        }

        if(isTruststoreConfigured())
        {
            TrustManagerFactory trustManagerFactory = initTrustManagerFactory();
            sslContextBuilder.trustManager(trustManagerFactory);
        }

        return sslContextBuilder;
    }

    private boolean isTlsOrMtlsConfigured()
    {
        return isTruststoreConfigured() || isKeystoreConfigured();
    }

    private boolean isTruststoreConfigured()
    {
        return trustStoreResource != null;
    }

    private boolean isKeystoreConfigured()
    {
        return keyStoreResource != null;
    }

    private HttpClient createHttpClientWithSslContext(SslContextBuilder sslContextBuilder) throws SSLException {
        SslContext sslContext = sslContextBuilder.build();
        return HttpClient.create().secure(p -> p.sslContext(sslContext).handlerConfigurator(handler -> {
            SSLEngine sslEngine = handler.engine();
            SSLParameters sslParameters = sslEngine.getSSLParameters();
            if(hostNameVerificationDisabled)
            {
                sslParameters.setEndpointIdentificationAlgorithm("");
            } else {
                sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
            }
            sslEngine.setSSLParameters(sslParameters);
        }));
    }

    private RestTemplate createRestTemplateWithSslContext(SSLContextBuilder sslContextBuilder) throws NoSuchAlgorithmException, KeyManagementException {
        final SSLConnectionSocketFactoryBuilder sslConnectionSocketFactoryBuilder =
                SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContextBuilder.build())
                    .setTlsVersions(TLS.V_1_2, TLS.V_1_3);
        if (hostNameVerificationDisabled) {
            sslConnectionSocketFactoryBuilder.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }
        final SSLConnectionSocketFactory sslConnectionSocketFactory = sslConnectionSocketFactoryBuilder.build();

        final Registry<ConnectionSocketFactory> sslSocketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory> create()
                        .register("https", sslConnectionSocketFactory)
                        .build();

        final BasicHttpClientConnectionManager sslConnectionManager = new BasicHttpClientConnectionManager(sslSocketFactoryRegistry);

        HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(sslConnectionManager);
        CloseableHttpClient httpClient = httpClientBuilder.build();
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(requestFactory);
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

    private TrustManagerFactory initTrustManagerFactory() throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException
    {
        KeyStore trustStore = getKeyStore(trustStoreType, trustStoreResource, trustStorePassword);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        return trustManagerFactory;
    }

    private KeyManagerFactory initKeyManagerFactory() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException
    {
        KeyStore clientKeyStore = getKeyStore(keyStoreType, keyStoreResource, keyStorePassword);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, keyStorePassword);
        return keyManagerFactory;
    }
}
