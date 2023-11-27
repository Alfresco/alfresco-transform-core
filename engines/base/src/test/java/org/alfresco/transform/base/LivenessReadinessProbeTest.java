package org.alfresco.transform.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.transform.base.model.FileRefEntity;
import org.alfresco.transform.base.model.FileRefResponse;
import org.alfresco.transform.client.model.TransformRequest;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.alfresco.transform.base.AbstractBaseTest.getTestFile;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_PDF;
import static org.alfresco.transform.common.Mimetype.MIMETYPE_TEXT_PLAIN;
import static org.assertj.core.api.FactoryBasedNavigableListAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import org.springframework.web.reactive.function.client.WebClient;
import org.alfresco.transform.base.sfs.SharedFileStoreClient;

//@SpringBootTest(classes={org.alfresco.transform.base.Application.class})

public class LivenessReadinessProbeTest
{
    @Autowired
    private ObjectMapper mapper = new ObjectMapper();
    @TempDir
    public File tempDir;

    protected SharedFileStoreClient sfsClient;

    protected String sourceExtension;
    protected String targetExtension;
    protected String sourceMimetype;
    protected String targetMimetype;

    protected String sourceMediaType;
    protected String targetMediaType;


    protected HashMap<String, String> options = new HashMap<>();

    protected String url;
    protected String readyUrl = "/ready";


    @ParameterizedTest
    @MethodSource ("containers")
    public void readinessShouldReturnAn429ErrorAfterReachingMaxTransforms(final ImagesForTests testData) throws URISyntaxException, InterruptedException, IOException {
        try (final var env = createEnv(testData.image))
        {
            env.start();
            url = "http://localhost:" + env.getFirstMappedPort();

            int max_transforms = 11;
            for (int i = 0; i<max_transforms; i++) {
                sendTransformRequest(url);
            }

            checkReadiness(url+"/ready", TOO_MANY_REQUESTS);

            final String logs = env.getLogs();
            System.out.println(logs);
        }
    }

    private GenericContainer<?> createEnv(String image) throws URISyntaxException
    {
        System.out.println(image);
        final GenericContainer<?> transformCore = new GenericContainer<>("alfresco/"+image+":latest");

        return transformCore.withEnv("livenessTransformEnabled", "true")
            .withEnv("maxTransforms", "10")
            .withNetworkAliases(image)
            .withExposedPorts(8090)
            .waitingFor(Wait.forListeningPort());
    }

    private static List<ImagesForTests> containers()
    {
//        final var allContainers = List.of(
//                new ImagesForTests("imagemagick", "alfresco-imagemagick"),
//                new ImagesForTests("ats-aio", "alfresco-transform-core-aio"),
//                new ImagesForTests("libreoffice", "alfresco-libreoffice"),
//                new ImagesForTests("misc", "alfresco-transform-misc"),
//                new ImagesForTests("pdf-renderer", "alfresco-pdf-renderer"),
//                new ImagesForTests("tika", "alfresco-tika"));

        final var allContainers = List.of(
                new ImagesForTests("libreoffice", "alfresco-libreoffice"));

        return allContainers;
    }
    private static class ImagesForTests
    {
        private final String name;
        private final String image;

        private ImagesForTests(String name, String image)
        {
            this.name = Objects.requireNonNull(name);
            this.image = Objects.requireNonNull(image);
        }
    }
    private void sendTransformRequest(String url) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("_sourceMimetype", "text/plain");
        builder.part("_targetMimetype", "application/pdf");
        builder.part("file", new ByteArrayResource("a new file for tests".getBytes(StandardCharsets.UTF_8))).filename("testfile.txt");

        WebClient client = WebClient.create();
        WebClient.ResponseSpec responseSpec = client.post()
                .uri(url + "/test")
                .bodyValue(builder.build())
                .retrieve();

        System.out.println(responseSpec.toBodilessEntity().block());
        assertEquals(OK, responseSpec.toBodilessEntity().block().getStatusCode());
    }

//    private static HttpStatusCode getResponse(String url) {
//        WebClient client = WebClient.create();
//        WebClient.ResponseSpec response = client.get()
//                .uri(url)
//                .retrieve();
//
////        System.out.println(response.toBodilessEntity().block().getStatusCode());
//        return response.toBodilessEntity().block().getStatusCode();
//    }

    private static void checkReadiness(String url, HttpStatusCode status) {
        WebTestClient client = WebTestClient.bindToServer().baseUrl(url).build();
        client.get()
                .exchange()
                .expectStatus().isEqualTo(status);
    }
}