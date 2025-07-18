/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transform.base.registry;

import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;

import static org.springframework.util.CollectionUtils.isEmpty;

import static org.alfresco.transform.config.CoreVersionDecorator.setCoreVersionOnSingleStepTransformers;
import static org.alfresco.transform.registry.TransformerType.FAILOVER_TRANSFORMER;
import static org.alfresco.transform.registry.TransformerType.PIPELINE_TRANSFORMER;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.alfresco.transform.base.CustomTransformer;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.TransformOption;
import org.alfresco.transform.config.TransformOptionGroup;
import org.alfresco.transform.config.TransformOptionValue;
import org.alfresco.transform.config.Transformer;
import org.alfresco.transform.registry.AbstractTransformRegistry;
import org.alfresco.transform.registry.CombinedTransformConfig;
import org.alfresco.transform.registry.Origin;
import org.alfresco.transform.registry.TransformCache;
import org.alfresco.transform.registry.TransformerType;

@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class TransformRegistry extends AbstractTransformRegistry
{
    private static final Logger logger = LoggerFactory.getLogger(TransformRegistry.class);

    @Autowired
    private String coreVersion;
    @Autowired
    private List<TransformConfigSource> transformConfigSources;
    @Value("${container.isTRouter}")
    private boolean isTRouter;

    private final AtomicBoolean isRecoveryModeOn = new AtomicBoolean(true);

    // Not autowired - avoids a circular reference in the router - initialised on startup event
    private List<CustomTransformer> customTransformerList;

    private int previousLogMessageHashCode;

    private static class Data extends TransformCache
    {
        private TransformConfig transformConfig;
        private TransformConfig uncombinedTransformConfig;
        private Map<String, Origin<Transformer>> transformerByNameMap;

        public TransformConfig getTransformConfig()
        {
            return transformConfig;
        }

        public void setTransformConfig(TransformConfig transformConfig)
        {
            this.transformConfig = transformConfig;
        }

        public TransformConfig getUncombinedTransformConfig()
        {
            return uncombinedTransformConfig;
        }

        public void setUncombinedTransformConfig(TransformConfig uncombinedTransformConfig)
        {
            this.uncombinedTransformConfig = uncombinedTransformConfig;
        }

        public Map<String, Origin<Transformer>> getTransformerByNameMap()
        {
            return transformerByNameMap;
        }

        public void setTransformerByNameMap(Map<String, Origin<Transformer>> transformerByNameMap)
        {
            this.transformerByNameMap = transformerByNameMap;
        }

        public int getTransformCount()
        {
            return transformCount;
        }
    }

    private Data data = new Data();

    // Ensures that read operations are blocked while config is being updated
    private final ReadWriteLock configRefreshLock = new ReentrantReadWriteLock();

    @EventListener(ContextRefreshedEvent.class)
    public void handleContextRefreshedEvent(final ContextRefreshedEvent event)
    {
        final ApplicationContext context = event.getApplicationContext();
        // the local "initEngineConfigs" method has to be called through the Spring proxy
        context.getBean(TransformRegistry.class).initRegistryOnAppStartup(event);
    }

    /**
     * Load the registry on application startup. This allows Components in projects that extend the t-engine base to use @PostConstruct to add to {@code transformConfigSources}, before the registry is loaded.
     */
    @Async
    void initRegistryOnAppStartup(final ContextRefreshedEvent event)
    {
        customTransformerList = event.getApplicationContext().getBean(CustomTransformers.class).toList();
        retrieveConfig();
    }

    /**
     * Takes the schedule from a spring-boot property
     */
    @Scheduled(cron = "${transform.engine.config.cron}")
    public void retrieveEngineConfigs()
    {
        logger.trace("Refresh TransformRegistry.");
        retrieveConfig();
    }

    /**
     * Recovery mode method, executes only when there was failure during initial config retrieval process.
     */
    @Scheduled(initialDelayString = "#{${transform.engine.config.retry.timeout} * 1000}",
            fixedDelayString = "#{${transform.engine.config.retry.timeout} * 1000}")
    public void retrieveEngineConfigsAfterFailure()
    {
        if (isRecoveryModeOn.get())
        {
            logger.trace("Recovery mode, attempting to retrieve configs for all registered T-Engines.");
            retrieveConfig();
        }
    }

    void retrieveConfig()
    {
        CombinedTransformConfig combinedTransformConfig = new CombinedTransformConfig();
        TreeMap<String, LocalTransformConfigSource> availableTransformers = new TreeMap<>();

        logger.debug("Retrieving available TransformConfig.");
        for (TransformConfigSource source : transformConfigSources)
        {
            try
            {
                String sortOnName = source.getSortOnName();
                TransformConfig transformConfig = source.getTransformConfig();
                availableTransformers.put(
                        sortOnName,
                        new LocalTransformConfigSource(transformConfig, sortOnName, source.getReadFrom(), source.getBaseUrl()));
            }
            catch (IllegalStateException e)
            {
                if (isRecoveryModeOn.getAcquire())
                {
                    logger.trace("Failed to retrieved TransformConfig during recovery mode. {}", e.getMessage());
                }
                else
                {
                    logger.warn(
                            "Failed to retrieved TransformConfig during refreshment. Stops refreshing TransformRegistry. {}",
                            e.getMessage());
                    return;
                }
            }
        }

        if (transformConfigSources.size() == availableTransformers.size()
                && isRecoveryModeOn.compareAndExchange(true, false))
        {
            logger.trace("All TransformConfigSources have been retrieved, turning off recovery mode.");
        }

        logger.debug("Creating CombinedTransformConfig.");
        availableTransformers.values().forEach(source -> {
            TransformConfig transformConfig = source.getTransformConfig();
            setCoreVersionOnSingleStepTransformers(transformConfig, coreVersion);
            combinedTransformConfig.addTransformConfig(transformConfig, source.getReadFrom(), source.getBaseUrl(),
                    this);
        });

        TransformConfig uncombinedTransformConfig = combinedTransformConfig.buildTransformConfig();
        combinedTransformConfig.combineTransformerConfig(this);
        TransformConfig transformConfig = combinedTransformConfig.buildTransformConfig();
        Map<String, Origin<Transformer>> transformerByNameMap = combinedTransformConfig.getTransformerByNameMap();
        concurrentUpdate(combinedTransformConfig, uncombinedTransformConfig, transformConfig, transformerByNameMap);

        logTransformers(uncombinedTransformConfig, transformerByNameMap);
    }

    private void logTransformers(TransformConfig uncombinedTransformConfig, Map<String, Origin<Transformer>> transformerByNameMap)
    {
        if (logger.isInfoEnabled())
        {
            Set<String> customTransformerNames = new HashSet<>(customTransformerList == null
                    ? Collections.emptySet()
                    : customTransformerList.stream().map(CustomTransformer::getTransformerName).collect(Collectors.toSet()));
            List<String> nonNullTransformerNames = uncombinedTransformConfig.getTransformers().stream()
                    .map(Transformer::getTransformerName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            ArrayList<String> logMessages = new ArrayList<>();
            if (!nonNullTransformerNames.isEmpty())
            {
                logMessages.add("Transformers (" + nonNullTransformerNames.size() + ") Transforms (" + getData().getTransformCount() + "):");
                nonNullTransformerNames
                        .stream()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .map(name -> {
                            Origin<Transformer> transformerOrigin = transformerByNameMap.get(name);
                            String message = "  " + name + (transformerOrigin == null
                                    ? " -- unavailable: see previous messages"
                                    : isTRouter
                                            ? ""
                                            : TransformerType.valueOf(transformerOrigin.get()) == PIPELINE_TRANSFORMER
                                                    ? " -- unavailable: pipeline only available via t-router"
                                                    : TransformerType.valueOf(transformerOrigin.get()) == FAILOVER_TRANSFORMER
                                                            ? " -- unavailable: failover only available via t-router"
                                                            : !customTransformerNames.contains(name)
                                                                    ? " -- missing: CustomTransformer"
                                                                    : "");
                            customTransformerNames.remove(name);
                            return message;
                        })
                        .forEach(logMessages::add);

                List<String> unusedCustomTransformNames = customTransformerNames.stream()
                        .filter(Objects::nonNull)
                        .sorted()
                        .collect(Collectors.toList());
                if (!unusedCustomTransformNames.isEmpty())
                {
                    logMessages.add("Unused CustomTransformers (" + unusedCustomTransformNames.size() + ") - name is not in the transform config:");
                    unusedCustomTransformNames
                            .stream()
                            .map(name -> "  " + name)
                            .forEach(logMessages::add);
                }

                int logMessageHashCode = logMessages.hashCode();
                if (previousLogMessageHashCode != logMessageHashCode)
                {
                    previousLogMessageHashCode = logMessageHashCode;
                    logMessages.stream().forEach(logger::info);
                }
                else
                {
                    logger.debug("Config unchanged");
                }
            }
        }
    }

    public TransformConfig getTransformConfig()
    {
        Data data = getData();
        return isTRouter
                ? data.getTransformConfig()
                : data.getUncombinedTransformConfig();
    }

    /**
     * @return Returns true if transform information has been loaded.
     */
    public boolean isReadyForTransformRequests()
    {
        return getData().getTransforms().size() > 0;
    }

    public boolean isRecoveryModeOn()
    {
        return isRecoveryModeOn.getAcquire();
    }

    @Override
    public Data getData()
    {
        return concurrentRead(() -> data);
    }

    /**
     * Lock for reads while updating, use {@link #concurrentRead} to access locked fields
     */
    private void concurrentUpdate(CombinedTransformConfig combinedTransformConfig,
            TransformConfig uncombinedTransformConfig, TransformConfig transformConfig,
            Map<String, Origin<Transformer>> transformerByNameMap)
    {
        configRefreshLock.writeLock().lock();
        try
        {
            data = new Data(); // clear data
            data.setTransformConfig(transformConfig);
            data.setUncombinedTransformConfig(uncombinedTransformConfig);
            data.setTransformerByNameMap(transformerByNameMap);
            combinedTransformConfig.registerCombinedTransformers(this);
        }
        finally
        {
            configRefreshLock.writeLock().unlock();
        }
    }

    private <T> T concurrentRead(Supplier<T> s)
    {
        configRefreshLock.readLock().lock();
        try
        {
            return s.get();
        }
        finally
        {
            configRefreshLock.readLock().unlock();
        }
    }

    @Override
    protected void logError(String msg)
    {
        logger.error(msg);
    }

    @Override
    protected void logWarn(String msg)
    {
        logger.warn(msg);
    }

    public Transformer getTransformer(final String sourceMediaType, final Long fileSizeBytes,
            final String targetMediaType, final Map<String, String> transformOptions)
    {
        return concurrentRead(() -> {
            long fileSize = fileSizeBytes == null ? 0 : fileSizeBytes;
            String transformerName = findTransformerName(sourceMediaType, fileSize, targetMediaType, transformOptions, null);
            return getTransformer(transformerName);
        });
    }

    public Transformer getTransformer(String transformerName)
    {
        return getTransformer(getData(), transformerName);
    }

    private Transformer getTransformer(Data data, String transformerName)
    {
        Origin<Transformer> transformerOrigin = data.getTransformerByNameMap().get(transformerName);
        return transformerOrigin == null ? null : transformerOrigin.get();
    }

    public boolean checkSourceSize(String transformerName, String sourceMediaType, Long sourceSize, String targetMediaType)
    {
        return Optional.ofNullable(getTransformer(transformerName)).map(transformer -> transformer.getSupportedSourceAndTargetList().stream().filter(supported -> supported.getSourceMediaType().equals(sourceMediaType) &&
                supported.getTargetMediaType().equals(targetMediaType)).findFirst().map(supported -> supported.getMaxSourceSizeBytes() == -1 ||
                        supported.getMaxSourceSizeBytes() >= sourceSize)
                .orElse(false)).orElse(false);
    }

    public String getEngineName(String transformerName)
    {
        return getData().getTransformerByNameMap().get(transformerName).getReadFrom();
    }

    /**
     * Filters the transform options for a given transformer. In a pipeline there may be options for different steps.
     */
    public Map<String, String> filterOptions(final String transformerName, final Map<String, String> options)
    {
        Data data = getData();
        final Map<String, Set<TransformOption>> configOptions = data.getTransformConfig().getTransformOptions();
        final Transformer transformer = getTransformer(data, transformerName);
        if (isNull(transformer) || isEmpty(options) || isEmpty(configOptions))
        {
            return emptyMap();
        }

        final Set<String> knownOptions = transformer.getTransformOptions()
                .stream()
                .flatMap(name -> configOptions.get(name).stream())
                .filter(Objects::nonNull)
                .flatMap(TransformRegistry::retrieveOptionsStrings)
                .collect(toUnmodifiableSet());
        if (isEmpty(knownOptions))
        {
            return emptyMap();
        }

        return options
                .entrySet()
                .stream()
                .filter(e -> knownOptions.contains(e.getKey()))
                .collect(toUnmodifiableMap(Entry::getKey, Entry::getValue));
    }

    private static Stream<String> retrieveOptionsStrings(final TransformOption option)
    {
        if (option instanceof TransformOptionGroup)
        {
            return ((TransformOptionGroup) option)
                    .getTransformOptions()
                    .stream()
                    .flatMap(TransformRegistry::retrieveOptionsStrings);
        }
        return Stream.of(((TransformOptionValue) option).getName());
    }
}
