package org.alfresco.transform.base;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public abstract class LivenessReadinessProbeTest
{
    protected final Integer MAX_TRANSFORMS = 10;

    @Test
    public void readinessShouldReturnAn429ErrorAfterReachingMaxTransforms() throws URISyntaxException
    {
        final ImagesForTests testData = getImageForTest();

        try (var env = createEnv(testData.image))
        {
            env.start();
            var url = "http://localhost:" + env.getFirstMappedPort();

            /* Asserts that /ready probe hasn't died before sending a transformation request. Each /ready request creates a valid transformation and increases the counter of used transformations, hence the need to divide MAX_TRANSFORMS */
            for (int i = 0; i < MAX_TRANSFORMS / 2; i++)
            {
                assertProbeIsOk(url);
                sendTransformRequest(url, testData.sourceMimetype, testData.targetMimetype, testData.filename);
            }

            assertProbeDied(url);

            final String logs = env.getLogs();
            System.out.println(logs);
        }
    }

    protected abstract ImagesForTests getImageForTest();

    @Test
    public void transformsEndpointReportsRegistryStateWithoutAffectingHealth() throws URISyntaxException
    {
        final ImagesForTests testData = getImageForTest();

        try (final var env = createEnv(testData.image))
        {
            env.start();
            final String url = "http://localhost:" + env.getFirstMappedPort();

            final String transforms = await("the transform registry to be loaded")
                    .atMost(2, TimeUnit.MINUTES)
                    .pollInterval(Duration.ofSeconds(1))
                    .ignoreExceptions()
                    .until(() -> getBody(url + "/actuator/transforms"),
                            body -> body.contains("\"ready\":true"));

            assertTrue(transforms.contains("\"transformCount\""),
                    "/actuator/transforms should report the transform count, but was: " + transforms);

            // The k8s style probes must still answer exactly as they did before. They are
            // available without any extra configuration: Spring Boot 4 declares
            // AvailabilityProbesAutoConfiguration as
            // @ConditionalOnBooleanProperty(name = "management.endpoint.health.probes.enabled",
            // matchIfMissing = true), so the liveness and readiness groups exist by default
            // rather than only on Kubernetes as in Spring Boot 3. Asserting the shipped
            // default here is deliberate - enabling the probes just for the test would stop
            // this covering the configuration the engines actually run with.
            for (String probe : new String[]{"/actuator/health/readiness", "/actuator/health/liveness"})
            {
                WebTestClient.bindToServer().baseUrl(url + probe).build()
                        .get()
                        .exchange()
                        .expectStatus().isEqualTo(OK);
            }

            final String health = getBody(url + "/actuator/health");
            assertFalse(health.toLowerCase(Locale.ROOT).contains("transform"),
                    "The transform registry must not contribute to /actuator/health, but it answered: " + health);
        }
    }

    private static String getBody(String uri)
    {
        final String body = WebClient.create()
                .get()
                .uri(uri)
                .exchangeToMono(response -> response.bodyToMono(String.class))
                .block();
        return body == null ? "" : body;
    }

    private GenericContainer<?> createEnv(String image)
    {
        System.out.println(image);
        final GenericContainer<?> transformCore = new GenericContainer<>("alfresco/" + image + ":latest");

        return transformCore.withEnv("livenessTransformEnabled", "true")
                .withEnv("maxTransforms", MAX_TRANSFORMS.toString())
                .withEnv("TRANSFORM_ENDPOINT_TEST_ENABLE", "true")
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
        WebTestClient client = WebTestClient.bindToServer().baseUrl(url + "/ready").build();
        client.get()
                .exchange()
                .expectStatus().isEqualTo(TOO_MANY_REQUESTS);
    }

    private static void assertProbeIsOk(String url)
    {
        WebTestClient client = WebTestClient.bindToServer().baseUrl(url + "/ready").build();
        client.get()
                .exchange()
                .expectStatus().isEqualTo(OK);
    }
}
