package org.alfresco.transform.base;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.URISyntaxException;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import org.springframework.web.reactive.function.client.WebClient;

public abstract class LivenessReadinessProbeTest
{
    protected final Integer MAX_TRANSFORMS = 10;

    @Test
    public void readinessShouldReturnAn429ErrorAfterReachingMaxTransforms() throws URISyntaxException
    {
        final ImagesForTests testData = getImageForTest();

        try (final var env = createEnv(testData.image))
        {
            env.start();
            var url = "http://localhost:" + env.getFirstMappedPort();

            /*
                Asserts that /ready probe hasn't died before sending a transformation request.
                Each /ready request creates a valid transformation and increases the counter of
                used transformations, hence the need to divide MAX_TRANSFORMS
            */
            for (int i = 0; i<MAX_TRANSFORMS/2; i++) {
                assertProbeIsOk(url);
                sendTransformRequest(url, testData.sourceMimetype, testData.targetMimetype, testData.filename);
            }

            assertProbeDied(url);

            final String logs = env.getLogs();
            System.out.println(logs);
        }
    }

    protected abstract ImagesForTests getImageForTest();

    private GenericContainer<?> createEnv(String image) throws URISyntaxException
    {
        System.out.println(image);
        final GenericContainer<?> transformCore = new GenericContainer<>("alfresco/"+image+":latest");

        return transformCore.withEnv("livenessTransformEnabled", "true")
            .withEnv("maxTransforms", MAX_TRANSFORMS.toString())
            .withNetworkAliases(image)
            .withExposedPorts(8090)
            .waitingFor(Wait.forListeningPort());
    }

    protected static class ImagesForTests
    {
        private final String image;
        private final String sourceMimetype;
        private final String targetMimetype;
        private final String filename;

        public ImagesForTests(String image, String sourceMimetype, String targetMimetype, String filename)
        {
            this.image = Objects.requireNonNull(image);
            this.sourceMimetype = Objects.requireNonNull(sourceMimetype);
            this.targetMimetype = Objects.requireNonNull(targetMimetype);
            this.filename = Objects.requireNonNull(filename);
        }
    }

    private void sendTransformRequest(String url, String sourceMimetype, String targetMimetype, String filename)
    {
        var builder = createRequestBuilder(sourceMimetype, targetMimetype, filename);
        WebClient client = WebClient.create();
        WebClient.ResponseSpec responseSpec = client.post()
                .uri(url + "/test")
                .bodyValue(builder.build())
                .retrieve();

        System.out.println(responseSpec.toBodilessEntity().block());
        assertEquals(OK, responseSpec.toBodilessEntity().block().getStatusCode());
    }

    private MultipartBodyBuilder createRequestBuilder(String sourceMimetype, String targetMimetype, String filename)
    {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("_sourceMimetype", sourceMimetype);
        builder.part("_targetMimetype", targetMimetype);
        builder.part("file", new ClassPathResource(filename));

        return builder;
    }

    private static void assertProbeDied(String url)
    {
        WebTestClient client = WebTestClient.bindToServer().baseUrl(url+"/ready").build();
        client.get()
                .exchange()
                .expectStatus().isEqualTo(TOO_MANY_REQUESTS);
    }

    private static void assertProbeIsOk(String url)
    {
        WebTestClient client = WebTestClient.bindToServer().baseUrl(url+"/ready").build();
        client.get()
                .exchange()
                .expectStatus().isEqualTo(OK);
    }
}