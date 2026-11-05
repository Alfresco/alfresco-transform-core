/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2026 Alfresco Software Limited
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
package org.alfresco.transform.base.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * Checks each configuration route (files, env vars, system properties, args, JSON) and how they combine, using a real {@code SpringApplication}.
 */
// One test per cell of the source/combination matrix.
@SuppressWarnings("PMD.TooManyMethods")
class ClassificationConfigurationSourcesTest
{
    private static final String PREFIX = "transform.failure.classification.";
    private static final String ENGINE = "contributions.engine.";
    private static final String SITE = "contributions.site.";

    @TempDir
    Path tempDir;

    /** Every route a deployment can supply a property by, other than the engine's packaged file. */
    enum Source
    {
        /** A mounted directory named by {@code SPRING_CONFIG_ADDITIONAL_LOCATION}. */
        EXTERNAL_DIRECTORY,
        /** A single file named by {@code --spring.config.additional-location}. */
        EXTERNAL_FILE,
        /** A file pulled in with {@code spring.config.import}. */
        CONFIG_IMPORT,
        /** Environment variables, lists comma separated: {@code TRANSFORM_FAILURE_CLASSIFICATION_CLASSNAMES=A,B}. */
        ENVIRONMENT_COMMA_SEPARATED,
        /** Environment variables, one per entry: {@code TRANSFORM_FAILURE_CLASSIFICATION_CLASSNAMES_0_=A}. */
        ENVIRONMENT_INDEXED,
        /** JVM system properties, as set with {@code -D} or {@code JAVA_OPTS}. */
        SYSTEM_PROPERTIES,
        /** {@code --name=value} arguments. */
        COMMAND_LINE,
        /** {@code SPRING_APPLICATION_JSON}. */
        APPLICATION_JSON
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Static only

    @Test
    void testWithNothingConfiguredTheShippedDefaultsApply()
    {
        TransformExceptionClassifier classifier = launch().run();

        assertEquals(ClassificationDefaults.CLASS_NAMES, classifier.getClassNames());
        assertEquals(ClassificationDefaults.MESSAGE_FRAGMENTS, classifier.getMessageFragments());
        assertEquals(ClassificationDefaults.MESSAGE_FRAGMENT_CLASS_NAMES, classifier.getMessageFragmentClassNames());
        assertEquals(ClassificationDefaults.INFRASTRUCTURE_CLASS_NAMES, classifier.getInfrastructureClassNames());
        assertEquals(ClassificationDefaults.INFRASTRUCTURE_MESSAGE_FRAGMENTS,
                classifier.getInfrastructureMessageFragments());
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("invalid stored block lengths")));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new IOException("No space left on device")));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Static + one runtime source

    @ParameterizedTest
    @EnumSource(Source.class)
    void testAContributionExtendsTheDefaults(Source source)
    {
        TransformExceptionClassifier classifier = launch()
                .with(source, Map.of(
                        SITE + "class-names", List.of("RuntimeAddedException"),
                        SITE + "message-fragments", List.of("Glyph Table Is Unreadable")))
                .run();

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new RuntimeAddedException()));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IOException("the glyph table is unreadable")));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("still a default")), source.name());
        assertEquals(ClassificationDefaults.CLASS_NAMES, classifier.getClassNames());
        assertEquals(Set.of("site"), classifier.getContributions().keySet());
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void testAReplacementRemovesTheDefaults(Source source)
    {
        TransformExceptionClassifier classifier = launch()
                .with(source, Map.of("class-names", List.of("ReplacementOnlyException")))
                .run();

        assertEquals(List.of("ReplacementOnlyException"), classifier.getClassNames());
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ReplacementOnlyException()));
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new ZipException("no longer listed")));
    }

    @ParameterizedTest
    @EnumSource(Source.class)
    void testTheKillSwitchWorksFromEverySource(Source source)
    {
        TransformExceptionClassifier classifier = launch()
                .with(source, Map.of("enabled", "false"))
                .run();

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new ZipException("would be 422 if enabled")));
    }

    /** The variable {@code application.yaml} documents, not the relaxed-binding name. */
    @Test
    void testTheDocumentedKillSwitchVariableWorks()
    {
        TransformExceptionClassifier classifier = launch()
                .env("TRANSFORM_FAILURE_CLASSIFICATION_ENABLED", "false")
                .run();

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new ZipException("would be 422 if enabled")));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Static + engine-packaged

    @Test
    void testAnEnginesPackagedContributionExtendsTheDefaults()
    {
        TransformExceptionClassifier classifier = launch()
                .engine(Map.of(ENGINE + "class-names", List.of("EngineAddedException")))
                .run();

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new EngineAddedException()));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("still a default")));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Static + engine-packaged + runtime

    /** The engine ships its own entries and an operator adds one; both must survive. */
    @ParameterizedTest
    @EnumSource(Source.class)
    void testAnOperatorsContributionDoesNotDiscardAnEnginesContribution(Source source)
    {
        TransformExceptionClassifier classifier = launch()
                .engine(Map.of(ENGINE + "class-names", List.of("EngineAddedException")))
                .with(source, Map.of(SITE + "class-names", List.of("RuntimeAddedException")))
                .run();

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new RuntimeAddedException()), source.name());
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new EngineAddedException()), source.name());
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("still a default")), source.name());
        assertEquals(Set.of("engine", "site"), classifier.getContributions().keySet(), source.name());
    }

    @Test
    void testContributionsFromThreeSourcesAllApply()
    {
        TransformExceptionClassifier classifier = launch()
                .engine(Map.of(ENGINE + "class-names", List.of("EngineAddedException")))
                .with(Source.EXTERNAL_DIRECTORY, Map.of(SITE + "class-names", List.of("RuntimeAddedException")))
                .with(Source.ENVIRONMENT_COMMA_SEPARATED, Map.of("contributions.team.message-fragments",
                        List.of("glyph table is unreadable")))
                .run();

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new EngineAddedException()));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new RuntimeAddedException()));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IOException("Glyph table is unreadable")));
        assertEquals(Set.of("engine", "site", "team"), classifier.getContributions().keySet());
    }

    /** Overruling is per list: lists the operator sets replace the engine's, the rest are kept. */
    @ParameterizedTest
    @EnumSource(Source.class)
    void testAnOperatorCanOverruleOneListOfAnEnginesContribution(Source source)
    {
        TransformExceptionClassifier classifier = launch()
                .engine(Map.of(
                        ENGINE + "class-names", List.of("EngineAddedException"),
                        ENGINE + "message-fragments", List.of("engine specific damage")))
                .with(source, Map.of(ENGINE + "class-names", List.of("RuntimeAddedException")))
                .run();

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new EngineAddedException()), source.name());
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new RuntimeAddedException()), source.name());
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IOException("Engine specific damage")),
                source.name());
    }

    /** Excludes the indexed environment form, which cannot express an empty list. */
    @ParameterizedTest
    @EnumSource(value = Source.class, mode = EnumSource.Mode.EXCLUDE, names = "ENVIRONMENT_INDEXED")
    void testAnOperatorCanRemoveAnEnginesContribution(Source source)
    {
        TransformExceptionClassifier classifier = launch()
                .engine(Map.of(ENGINE + "class-names", List.of("EngineAddedException")))
                .with(source, Map.of(ENGINE + "class-names", List.of()))
                .run();

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new EngineAddedException()), source.name());
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ZipException("still a default")), source.name());
        assertEquals(List.of(), classifier.getContributions().get("engine").getClassNames(), source.name());
    }

    /** The result must not depend on which source Spring binds first. */
    @ParameterizedTest
    @EnumSource(Source.class)
    void testAnOperatorsReplacementKeepsAnEnginesContribution(Source source)
    {
        TransformExceptionClassifier classifier = launch()
                .engine(Map.of(ENGINE + "class-names", List.of("EngineAddedException")))
                .with(source, Map.of("class-names", List.of("ReplacementOnlyException")))
                .run();

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new ReplacementOnlyException()), source.name());
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new EngineAddedException()), source.name());
        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new ZipException("replaced")), source.name());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The shipped file itself, edited and mounted at runtime

    /** The defaults file uses the property keys, so a copy can be edited and mounted as Spring config. */
    @Test
    void testTheShippedFileCanBeEditedAndMountedToRemoveOneDefault() throws IOException
    {
        String shipped = readShippedDefaults();
        String edited = shipped.replace("        - ZipException\n", "");
        assertEquals(shipped.length() - "        - ZipException\n".length(), edited.length(),
                "The edit did not apply, so this test would prove nothing.");
        Path dir = Files.createDirectories(tempDir.resolve("mounted"));
        Files.writeString(dir.resolve("application.yaml"), edited, StandardCharsets.UTF_8);

        TransformExceptionClassifier classifier = launch()
                .engine(Map.of(ENGINE + "class-names", List.of("EngineAddedException")))
                .env("SPRING_CONFIG_ADDITIONAL_LOCATION", dir.toUri().toString())
                .run();

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new ZipException("removed by the operator")));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new java.io.EOFException("still shipped")));
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new EngineAddedException()));
        assertEquals(ClassificationDefaults.CLASS_NAMES.size() - 1, classifier.getClassNames().size());
        assertEquals(ClassificationDefaults.INFRASTRUCTURE_CLASS_NAMES, classifier.getInfrastructureClassNames());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precedence between runtime sources

    @Test
    void testPrecedenceBetweenRuntimeSourcesIsSpringBootsDocumentedOrder()
    {
        Map<String, Object> fromFile = Map.of("class-names", List.of("FromFileException"));
        Map<String, Object> fromEnv = Map.of("class-names", List.of("FromEnvironmentException"));
        Map<String, Object> fromArgs = Map.of("class-names", List.of("FromCommandLineException"));

        assertEquals(List.of("FromCommandLineException"), launch()
                .engine(Map.of("class-names", List.of("FromEngineException")))
                .with(Source.EXTERNAL_DIRECTORY, fromFile)
                .with(Source.ENVIRONMENT_COMMA_SEPARATED, fromEnv)
                .with(Source.COMMAND_LINE, fromArgs)
                .run().getClassNames());
        assertEquals(List.of("FromEnvironmentException"), launch()
                .engine(Map.of("class-names", List.of("FromEngineException")))
                .with(Source.EXTERNAL_DIRECTORY, fromFile)
                .with(Source.ENVIRONMENT_COMMA_SEPARATED, fromEnv)
                .run().getClassNames());
        assertEquals(List.of("FromFileException"), launch()
                .engine(Map.of("class-names", List.of("FromEngineException")))
                .with(Source.EXTERNAL_DIRECTORY, fromFile)
                .run().getClassNames());
        assertEquals(List.of("FromEngineException"), launch()
                .engine(Map.of("class-names", List.of("FromEngineException")))
                .run().getClassNames());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Malformed values

    /** A blank fragment would match every message. */
    @ParameterizedTest
    @EnumSource(Source.class)
    void testABlankFragmentIsIgnoredRatherThanMatchingEverything(Source source)
    {
        TransformExceptionClassifier classifier = launch()
                .with(source, Map.of(SITE + "message-fragments", List.of("glyph table is unreadable", " ")))
                .run();

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new IOException("the stream was reset")),
                source.name());
        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new IOException("Glyph table is unreadable")),
                source.name());
    }

    @Test
    void testATrailingCommaInAnEnvironmentVariableDoesNotMatchEverything()
    {
        TransformExceptionClassifier contributed = launch()
                .env("TRANSFORM_FAILURE_CLASSIFICATION_CONTRIBUTIONS_SITE_MESSAGEFRAGMENTS", "glyph table is unreadable,")
                .run();
        TransformExceptionClassifier replaced = launch()
                .env("TRANSFORM_FAILURE_CLASSIFICATION_MESSAGEFRAGMENTS", "unexpected end of file,")
                .run();

        assertEquals(INTERNAL_SERVER_ERROR, contributed.classify(new IOException("the stream was reset")));
        assertEquals(INTERNAL_SERVER_ERROR, replaced.classify(new IOException("the stream was reset")));
        assertEquals(List.of("unexpected end of file"), replaced.getMessageFragments());
    }

    /** An anonymous class has an empty simple name, which a blank entry would match. */
    @Test
    void testABlankClassNameIsIgnored()
    {
        TransformExceptionClassifier classifier = launch()
                .with(Source.EXTERNAL_DIRECTORY, Map.of(SITE + "class-names", List.of("")))
                .run();

        assertEquals(INTERNAL_SERVER_ERROR, classifier.classify(new IllegalStateException("anonymous") {}));
    }

    @Test
    void testSurroundingWhitespaceInAQuotedEntryIsIgnored()
    {
        TransformExceptionClassifier classifier = launch()
                .with(Source.EXTERNAL_DIRECTORY, Map.of(SITE + "class-names", List.of("  RuntimeAddedException  ")))
                .run();

        assertEquals(UNPROCESSABLE_ENTITY, classifier.classify(new RuntimeAddedException()));
    }

    /** e.g. a fragment quoting a library message; comma-separated values cannot carry it. */
    @Test
    void testAFragmentContainingACommaNeedsTheIndexedOrJsonForm()
    {
        Map<String, Object> withComma = Map.of(SITE + "infrastructure-message-fragments",
                List.of("quota exhausted, try later"));

        assertEquals(List.of("quota exhausted", "try later"), launch()
                .with(Source.ENVIRONMENT_COMMA_SEPARATED, withComma)
                .run().getContributions().get("site").getInfrastructureMessageFragments());
        assertEquals(List.of("quota exhausted, try later"), launch()
                .with(Source.ENVIRONMENT_INDEXED, withComma)
                .run().getContributions().get("site").getInfrastructureMessageFragments());
        assertEquals(List.of("quota exhausted, try later"), launch()
                .with(Source.APPLICATION_JSON, withComma)
                .run().getContributions().get("site").getInfrastructureMessageFragments());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Harness

    private Launch launch()
    {
        return new Launch();
    }

    private static String readShippedDefaults() throws IOException
    {
        try (var stream = ClassificationDefaults.class.getResourceAsStream(ClassificationDefaults.RESOURCE))
        {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TransformExceptionClassifier.class)
    static class ClassifierOnly
    {}

    private final class Launch
    {
        private final Map<String, Object> environment = new LinkedHashMap<>();
        private final Map<String, Object> systemProperties = new LinkedHashMap<>();
        private final List<String> args = new ArrayList<>();
        private Path engineClasspathRoot;
        private int files;

        Launch env(String name, String value)
        {
            environment.put(name, value);
            return this;
        }

        Launch engine(Map<String, Object> settings)
        {
            engineClasspathRoot = newDirectory();
            write(engineClasspathRoot.resolve("application-default.yaml"), settings);
            return this;
        }

        Launch with(Source source, Map<String, Object> settings)
        {
            switch (source)
            {
            case EXTERNAL_DIRECTORY -> {
                Path dir = newDirectory();
                write(dir.resolve("application.yaml"), settings);
                environment.put("SPRING_CONFIG_ADDITIONAL_LOCATION", dir.toUri().toString());
            }
            case EXTERNAL_FILE -> {
                Path file = newDirectory().resolve("classification.yaml");
                write(file, settings);
                args.add("--spring.config.additional-location=" + file.toUri());
            }
            case CONFIG_IMPORT -> {
                Path file = newDirectory().resolve("classification.yaml");
                write(file, settings);
                args.add("--spring.config.import=" + file.toUri());
            }
            case ENVIRONMENT_COMMA_SEPARATED -> settings.forEach(
                    (key, value) -> environment.put(environmentName(key), commaSeparated(value)));
            case ENVIRONMENT_INDEXED -> settings.forEach((key, value) -> {
                if (value instanceof List<?> list)
                {
                    for (int i = 0; i < list.size(); i++)
                    {
                        environment.put(environmentName(key) + "_" + i + "_", list.get(i));
                    }
                }
                else
                {
                    environment.put(environmentName(key), value);
                }
            });
            case SYSTEM_PROPERTIES -> settings.forEach(
                    (key, value) -> systemProperties.put(PREFIX + key, commaSeparated(value)));
            case COMMAND_LINE -> settings.forEach(
                    (key, value) -> args.add("--" + PREFIX + key + "=" + commaSeparated(value)));
            case APPLICATION_JSON -> environment.put("SPRING_APPLICATION_JSON", json(settings));
            }
            return this;
        }

        TransformExceptionClassifier run()
        {
            StandardEnvironment env = new StandardEnvironment();
            env.getPropertySources().replace(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
                    new MapPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, systemProperties));
            env.getPropertySources().replace(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, environment));

            ClassLoader parent = ClassificationConfigurationSourcesTest.class.getClassLoader();
            try (URLClassLoader loader = new URLClassLoader(engineClasspath(), parent);
                    ConfigurableApplicationContext context = new SpringApplicationBuilder(ClassifierOnly.class)
                            .web(WebApplicationType.NONE)
                            .bannerMode(Banner.Mode.OFF)
                            .logStartupInfo(false)
                            .registerShutdownHook(false)
                            .resourceLoader(new DefaultResourceLoader(loader))
                            .environment(env)
                            .run(args.toArray(String[]::new)))
            {
                return context.getBean(TransformExceptionClassifier.class);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        private URL[] engineClasspath()
        {
            if (engineClasspathRoot == null)
            {
                return new URL[0];
            }
            try
            {
                return new URL[]{engineClasspathRoot.toUri().toURL()};
            }
            catch (MalformedURLException e)
            {
                throw new IllegalStateException(e);
            }
        }

        private Path newDirectory()
        {
            try
            {
                return Files.createDirectories(tempDir.resolve("source-" + files++));
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static String environmentName(String key)
    {
        return (PREFIX + key).replace(".", "_").replace("-", "").toUpperCase(java.util.Locale.ROOT);
    }

    private static String commaSeparated(Object value)
    {
        return value instanceof List<?> list
                ? list.stream().map(Object::toString).collect(Collectors.joining(","))
                : value.toString();
    }

    private static void write(Path file, Map<String, Object> settings)
    {
        StringBuilder yaml = new StringBuilder("transform:\n  failure:\n    classification:\n");
        settings.forEach((key, value) -> {
            if (value instanceof List<?> list && list.isEmpty())
            {
                yaml.append("      ").append(key).append(": []\n");
            }
            else if (value instanceof List<?> list)
            {
                yaml.append("      ").append(key).append(":\n");
                list.forEach(entry -> yaml.append("        - '").append(entry.toString().replace("'", "''"))
                        .append("'\n"));
            }
            else
            {
                yaml.append("      ").append(key).append(": ").append(value).append('\n');
            }
        });
        try
        {
            Files.writeString(file, yaml, StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static String json(Map<String, Object> settings)
    {
        String body = settings.entrySet().stream()
                .map(entry -> quote(entry.getKey()) + ":" + (entry.getValue() instanceof List<?> list
                        ? list.stream().map(Object::toString).map(ClassificationConfigurationSourcesTest::quote)
                                .collect(Collectors.joining(",", "[", "]"))
                        : quote(entry.getValue().toString())))
                .collect(Collectors.joining(","));
        return "{\"transform\":{\"failure\":{\"classification\":{" + body + "}}}}";
    }

    private static String quote(String value)
    {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static final class RuntimeAddedException extends Exception
    {}

    private static final class EngineAddedException extends Exception
    {}

    private static final class ReplacementOnlyException extends Exception
    {}
}
