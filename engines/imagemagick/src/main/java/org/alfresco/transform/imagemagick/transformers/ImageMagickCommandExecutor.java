/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2025 Alfresco Software Limited
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
package org.alfresco.transform.imagemagick.transformers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;
import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.alfresco.transform.base.executors.AbstractCommandExecutor;
import org.alfresco.transform.base.executors.RuntimeExec;

@Component
public class ImageMagickCommandExecutor extends AbstractCommandExecutor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageMagickCommandExecutor.class);

    private static final String OMP_NUM_THREADS = "OMP_NUM_THREADS";
    private static final String THREADS_PROPERTY = "transform.core.imagemagick.threads";
    private static final String AUTO = "auto";
    private static final int DEFAULT_MAX_CONCURRENT_TRANSFORMS = 10;

    @Value("${transform.core.imagemagick.exe}")
    private String exe;
    @Value("${transform.core.imagemagick.dyn}")
    private String dyn;
    @Value("${transform.core.imagemagick.root}")
    private String root;

    // Not currently used, but may be again in the future if we need an ImageMagick extension
    @Value("${transform.core.imagemagick.coders}")
    private String coders;
    @Value("${transform.core.imagemagick.config}")
    private String config;

    @Value("${transform.core.imagemagick.threads:1}")
    private String threads;

    @Value("${jms-listener.concurrency:1-10}")
    private String jmsListenerConcurrency;

    IntSupplier cpuBudget = Runtime.getRuntime()::availableProcessors;

    private String ompNumThreads;

    @PostConstruct
    private void createCommands()
    {
        if (StringUtils.isEmpty(exe))
        {
            throw new IllegalArgumentException("ImageMagickTransformer IMAGEMAGICK_EXE variable cannot be null or empty");
        }
        if (StringUtils.isEmpty(dyn))
        {
            throw new IllegalArgumentException("ImageMagickTransformer IMAGEMAGICK_DYN variable cannot be null or empty");
        }
        if (StringUtils.isEmpty(root))
        {
            throw new IllegalArgumentException("ImageMagickTransformer IMAGEMAGICK_ROOT variable cannot be null or empty");
        }

        ompNumThreads = resolveOmpNumThreads();
        LOGGER.info("{}={} resolved from {}={}, availableProcessors={}, jms-listener.concurrency={}",
                OMP_NUM_THREADS, ompNumThreads, THREADS_PROPERTY, threads, cpuBudget.getAsInt(),
                jmsListenerConcurrency);

        super.transformCommand = createTransformCommand();
        super.checkCommand = createCheckCommand();
    }

    private String resolveOmpNumThreads()
    {
        return resolveOmpNumThreads(threads, jmsListenerConcurrency, cpuBudget.getAsInt(),
                System.getenv(OMP_NUM_THREADS));
    }

    static String resolveOmpNumThreads(String threads, String jmsListenerConcurrency, int cpuBudget,
            String valueSetOnContainer)
    {
        if (StringUtils.isNotBlank(valueSetOnContainer))
        {
            return valueSetOnContainer.trim();
        }

        String requested = StringUtils.trimToEmpty(threads);
        if (!AUTO.equalsIgnoreCase(requested))
        {
            return Integer.toString(parsePositiveThreadCount(requested));
        }

        int maxConcurrentTransforms = maxConcurrentTransforms(jmsListenerConcurrency);
        return Integer.toString(Math.max(1, cpuBudget / maxConcurrentTransforms));
    }

    private static int parsePositiveThreadCount(String requested)
    {
        int threadCount;
        try
        {
            threadCount = Integer.parseInt(requested);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(THREADS_PROPERTY
                    + " must be a positive integer or '" + AUTO + "', but was '" + requested + "'", e);
        }
        if (threadCount < 1)
        {
            throw new IllegalArgumentException(THREADS_PROPERTY
                    + " must be a positive integer or '" + AUTO + "', but was " + threadCount);
        }
        return threadCount;
    }

    static int maxConcurrentTransforms(String jmsListenerConcurrency)
    {
        String range = StringUtils.trimToEmpty(jmsListenerConcurrency);
        int separator = range.indexOf('-');
        String upperBound = separator < 0 ? range : range.substring(separator + 1);
        try
        {
            return Math.max(1, Integer.parseInt(upperBound.trim()));
        }
        catch (NumberFormatException e)
        {
            LOGGER.warn("Could not read an upper bound from jms-listener.concurrency '{}', assuming {}",
                    jmsListenerConcurrency, DEFAULT_MAX_CONCURRENT_TRANSFORMS);
            return DEFAULT_MAX_CONCURRENT_TRANSFORMS;
        }
    }

    @Override
    protected RuntimeExec createTransformCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        // GraphicsMagick is a single dispatch binary invoked as "gm convert ...".
        // "-quiet" is dropped: it is not a GraphicsMagick option (GM is quiet by default).
        commandsAndArguments.put(".*",
                new String[]{exe, "convert", "${source}", "SPLIT:${options}", "-strip", "${target}"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);

        Map<String, String> processProperties = new HashMap<>();
        if (ompNumThreads != null)
        {
            processProperties.put(OMP_NUM_THREADS, ompNumThreads);
        }
        processProperties.put("MAGICK_HOME", root);
        processProperties.put("DYLD_FALLBACK_LIBRARY_PATH", dyn);
        processProperties.put("LD_LIBRARY_PATH", dyn);

        // Optional properties (see also https://imagemagick.org/script/resources.php#environment)
        if (coders != null && !coders.isBlank())
        {
            processProperties.put("MAGICK_CODER_MODULE_PATH", coders);
        }
        if (config != null && !config.isBlank())
        {
            processProperties.put("MAGICK_CONFIGURE_PATH", config);
        }
        runtimeExec.setProcessProperties(processProperties);

        Map<String, String> defaultProperties = new HashMap<>();
        defaultProperties.put("options", null);
        runtimeExec.setDefaultProperties(defaultProperties);

        runtimeExec.setErrorCodes(
                "1,2,255,400,405,410,415,420,425,430,435,440,450,455,460,465,470,475,480,485,490,495,499,700,705,710,715,720,725,730,735,740,750,755,760,765,770,775,780,785,790,795,799");

        return runtimeExec;
    }

    @Override
    protected RuntimeExec createCheckCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        // GraphicsMagick reports its version via "gm version" (not "gm -version").
        commandsAndArguments.put(".*", new String[]{exe, "version"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);
        return runtimeExec;
    }
}
