package org.alfresco.transform.base;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
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

    public static boolean isMtlsEnabled()
    {
        return Boolean.parseBoolean(System.getProperty("mtls-enabled"));
    }

    public static CloseableHttpClient httpClientWithMtls() throws NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException, IOException, CertificateException
    {
        String keyStoreFile = System.getProperty("mtls-keystore-file");
        String keyStoreType = System.getProperty("mtls-keystore-type");
        char[] keyStorePassword = System.getProperty("mtls-keystore-password").toCharArray();
        String trustStoreFile = System.getProperty("mtls-truststore-file");
        String trustStoreType = System.getProperty("mtls-truststore-type");
        char[] trustStorePassword = System.getProperty("mtls-truststore-password").toCharArray();

        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        try (InputStream keyStoreInputStream = new FileInputStream(keyStoreFile))
        {
            keyStore.load(keyStoreInputStream, keyStorePassword);
            sslContextBuilder.loadKeyMaterial(keyStore, keyStorePassword);
        }

        File trustStore = new File(trustStoreFile);
        sslContextBuilder.loadTrustMaterial(trustStore, trustStorePassword);

        SSLContext sslContext = sslContextBuilder.build();
        SSLConnectionSocketFactory sslContextFactory = new SSLConnectionSocketFactory(sslContext);
        return HttpClients.custom().setSSLSocketFactory(sslContextFactory).build();
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
