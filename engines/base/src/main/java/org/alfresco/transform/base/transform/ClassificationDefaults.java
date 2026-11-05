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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * The lists {@link TransformExceptionClassifier} ships with, read from {@value #RESOURCE} on the classpath.
 * <p/>
 * They are data rather than code - a few hundred exception names and message fragments, each needing an explanation of why it is listed or deliberately absent - so they live in a file that can be read and reviewed without Java. Keeping them out of the class also stops a correction looking like a behaviour change in a diff.
 * <p/>
 * The file is <i>not</i> a Spring property source, and is read once however many engines are in the image. It supplies the starting point; what a deployment actually runs is settled by the properties under {@code transform.failure.classification}, which replace or extend these. Giving the file the same key path as those properties is what makes the two readable together.
 */
final class ClassificationDefaults
{
    static final String RESOURCE = "/transform-failure-classification-defaults.yaml";

    private static final List<String> PATH = List.of("transform", "failure", "classification");

    private static final Map<String, Object> SECTION = section(load());

    static final List<String> CLASS_NAMES = list(SECTION, "class-names");
    static final List<String> MESSAGE_FRAGMENTS = list(SECTION, "message-fragments");
    static final List<String> MESSAGE_FRAGMENT_CLASS_NAMES = list(SECTION, "message-fragment-class-names");
    static final List<String> INFRASTRUCTURE_CLASS_NAMES = list(SECTION, "infrastructure-class-names");
    static final List<String> INFRASTRUCTURE_MESSAGE_FRAGMENTS = list(SECTION, "infrastructure-message-fragments");

    private ClassificationDefaults()
    {}

    /**
     * Every failure here stops the engine starting.
     * <p/>
     * The alternative - carrying on with empty lists - is worse than it looks: nothing would ever match, every failure would be reported as {@code 500}, and the T-Router would go back to retrying documents that can never succeed. That is the behaviour this classifier exists to correct, and it would return silently.
     */
    private static Object load()
    {
        try (InputStream stream = ClassificationDefaults.class.getResourceAsStream(RESOURCE))
        {
            if (stream == null)
            {
                throw new IllegalStateException(RESOURCE + " is not on the classpath. It is packaged in the "
                        + "engines/base jar, so an engine that repackages or filters resources needs to keep it.");
            }
            // Safe rather than the default constructor: this is read before anything else runs, and the file is only
            // ever five lists of strings, so there is no reason to let it name a type.
            return new Yaml(new SafeConstructor(new LoaderOptions())).load(stream);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not read " + RESOURCE + ".", e);
        }
    }

    /**
     * @return the mapping under the key path the configuration properties use. Sharing the path is what lets the file and an override be read together.
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> section(Object root)
    {
        Object node = root;
        for (String key : PATH)
        {
            if (!(node instanceof Map<?, ?> map) || map.get(key) == null)
            {
                throw new IllegalStateException(RESOURCE + " does not contain " + String.join(".", PATH) + ".");
            }
            node = map.get(key);
        }
        if (!(node instanceof Map<?, ?> section))
        {
            throw new IllegalStateException(RESOURCE + " does not contain " + String.join(".", PATH) + ".");
        }
        return (Map<String, Object>) section;
    }

    static List<String> list(Map<String, Object> section, String key)
    {
        Object value = section.get(key);
        if (!(value instanceof List<?> entries) || entries.isEmpty())
        {
            throw new IllegalStateException(
                    RESOURCE + " does not hold a non-empty list at " + String.join(".", PATH) + "." + key + ".");
        }
        return entries.stream()
                .map(entry -> entryText(key, entry))
                .toList();
    }

    /**
     * A blank entry would be far from harmless: message matching asks whether the message <i>contains</i> the fragment, so an empty one matches every message and would make every failure a terminal {@code 422}.
     */
    private static String entryText(String key, Object entry)
    {
        if (!(entry instanceof String text) || text.isBlank())
        {
            throw new IllegalStateException(RESOURCE + " has a blank or non-text entry in " + key + ".");
        }
        return text.trim();
    }
}
