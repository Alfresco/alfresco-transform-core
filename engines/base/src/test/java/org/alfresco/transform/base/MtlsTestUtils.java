package org.alfresco.transform.base;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
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
        SSLConnectionSocketFactory sslContextFactory = HOSTNAME_VERIFICATION_DISABLED ? new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE)
                : new SSLConnectionSocketFactory(sslContext);

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
