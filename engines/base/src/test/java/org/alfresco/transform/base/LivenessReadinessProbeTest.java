package org.alfresco.transform.base;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.core.io.ClassPathResource;

import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.URISyntaxException;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import org.springframework.web.reactive.function.client.WebClient;

public class LivenessReadinessProbeTest
{
    protected String url;

    @ParameterizedTest
    @MethodSource ("containers")
    public void readinessShouldReturnAn429ErrorAfterReachingMaxTransforms(final ImagesForTests testData) throws URISyntaxException {
        try (final var env = createEnv(testData.image))
        {
            env.start();
            url = "http://localhost:" + env.getFirstMappedPort();

            int max_transforms = 11;
            for (int i = 0; i<max_transforms; i++) {
                sendTransformRequest(url, testData.sourceMimetype, testData.targetMimetype, testData.filename);
            }

            assertProbeDied(url);

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
        final var allContainers = List.of(
                new ImagesForTests("imagemagick", "alfresco-imagemagick", "image/jpeg", "image/png", "test.jpeg"),
                new ImagesForTests("ats-aio", "alfresco-transform-core-aio", "text/plain", "text/plain", "original.txt"),
                new ImagesForTests("libreoffice", "alfresco-libreoffice", "text/plain", "application/pdf", "original.txt"),
                new ImagesForTests("misc", "alfresco-transform-misc", "text/plain", "text/plain", "original.txt"),
                new ImagesForTests("pdf-renderer", "alfresco-pdf-renderer", "application/pdf", "image/png", "test.pdf"),
                new ImagesForTests("tika", "alfresco-tika", "text/plain", "text/plain", "original.txt"));

        return allContainers;
    }
    private static class ImagesForTests
    {
        private final String name;
        private final String image;

        private final String sourceMimetype;
        private final String targetMimetype;
        private final String filename;

        private ImagesForTests(String name, String image, String sourceMimetype, String targetMimetype, String filename)
        {
            this.name = Objects.requireNonNull(name);
            this.image = Objects.requireNonNull(image);
            this.sourceMimetype = Objects.requireNonNull(sourceMimetype);
            this.targetMimetype = Objects.requireNonNull(targetMimetype);
            this.filename = Objects.requireNonNull(filename);
        }
    }

    private void sendTransformRequest(String url, String sourceMimetype, String targetMimetype, String filename) {
        var builder = createRequestBuilder(sourceMimetype, targetMimetype, filename);
        WebClient client = WebClient.create();
        WebClient.ResponseSpec responseSpec = client.post()
                .uri(url + "/test")
                .bodyValue(builder.build())
                .retrieve();

        System.out.println(responseSpec.toBodilessEntity().block());
        assertEquals(OK, responseSpec.toBodilessEntity().block().getStatusCode());
    }

    private MultipartBodyBuilder createRequestBuilder(String sourceMimetype, String targetMimetype, String filename) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("_sourceMimetype", sourceMimetype);
        builder.part("_targetMimetype", targetMimetype);
        builder.part("file", new ClassPathResource(filename));

        return builder;
    }

    private static void assertProbeDied(String url) {
        WebTestClient client = WebTestClient.bindToServer().baseUrl(url+"/ready").build();
        client.get()
                .exchange()
                .expectStatus().isEqualTo(TOO_MANY_REQUESTS);
    }
}