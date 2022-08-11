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

import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.registry.AbstractTransformRegistry;
import org.alfresco.transform.registry.CombinedTransformConfig;
import org.alfresco.transform.registry.TransformCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import static org.alfresco.transform.config.CoreVersionDecorator.setCoreVersionOnSingleStepTransformers;

@Service
public class TransformRegistry extends AbstractTransformRegistry
{
    private static final Logger log = LoggerFactory.getLogger(TransformRegistry.class);

    @Autowired
    private String coreVersion;
    @Autowired
    private List<TransformConfigSource> transformConfigSources;

    private static class Data extends TransformCache
    {
        private TransformConfig transformConfigBeforeIncompleteTransformsAreRemoved;

        public TransformConfig getTransformConfigBeforeIncompleteTransformsAreRemoved()
        {
            return transformConfigBeforeIncompleteTransformsAreRemoved;
        }

        public void setTransformConfigBeforeIncompleteTransformsAreRemoved(
            TransformConfig transformConfigBeforeIncompleteTransformsAreRemoved)
        {
            this.transformConfigBeforeIncompleteTransformsAreRemoved = transformConfigBeforeIncompleteTransformsAreRemoved;
        }
    }

    private Data data = new Data();

    // Ensures that read operations are blocked while config is being updated
    private ReadWriteLock configRefreshLock = new ReentrantReadWriteLock();

    @EventListener
    void handleContextRefreshedEvent(final ContextRefreshedEvent event)
    {
        final ApplicationContext context = event.getApplicationContext();
        // the local "initEngineConfigs" method has to be called through the Spring proxy
        context.getBean(TransformRegistry.class).initRegistryOnAppStartup(null);
    }

    /**
     * Load the registry on application startup. This allows Components in projects that extend the t-engine base
     * to use @PostConstruct to add to {@code transformConfigSources}, before the registry is loaded.
     */
//    @Async
//    @Retryable(include = {IllegalStateException.class},
//        maxAttemptsExpression = "#{${transform.engine.config.retry.attempts}}",
//        backoff = @Backoff(delayExpression = "#{${transform.engine.config.retry.timeout} * 1000}"))
    public void initRegistryOnAppStartup(final ContextRefreshedEvent event)
    {
        initRegistry();
    }

    /**
     * Takes the schedule from a spring-boot property
     */
//    @Scheduled(cron = "${transformer.engine.config.cron}")
    public void retrieveEngineConfigs()
    {
        log.trace("Refresh TransformRegistry");
        initRegistry();
    }

    void initRegistry()
    {
        CombinedTransformConfig combinedTransformConfig = new CombinedTransformConfig();

        transformConfigSources.stream()
            .sorted(Comparator.comparing(TransformConfigSource::getSortOnName))
            .forEach(source -> {
                TransformConfig transformConfig = source.getTransformConfig();
                setCoreVersionOnSingleStepTransformers(transformConfig, coreVersion);
                combinedTransformConfig.addTransformConfig(transformConfig, source.getReadFrom(), source.getBaseUrl(),
                    this);
            });

        TransformConfig transformConfigBeforeIncompleteTransformsAreRemoved = combinedTransformConfig.buildTransformConfig();
        combinedTransformConfig.combineTransformerConfig(this);
        concurrentUpdate(combinedTransformConfig, transformConfigBeforeIncompleteTransformsAreRemoved);
    }

    /**
     * Recovery method in case all the retries fail. If not specified, the @Retryable method will cause the application
     * to stop.
     */
    //    @Recover
    private void recover(IllegalStateException e)
    {
        log.warn(e.getMessage());
    }

    public TransformConfig getTransformConfig()
    {
        return getData().getTransformConfigBeforeIncompleteTransformsAreRemoved();
    }

    @Override
    public Data getData()
    {
        return concurrentRead(() -> data );
    }

    /**
     * Lock for reads while updating, use {@link #concurrentRead} to access locked fields
     */
    private void concurrentUpdate(CombinedTransformConfig combinedTransformConfig,
        TransformConfig transformConfigBeforeIncompleteTransformsAreRemoved)
    {
        configRefreshLock.writeLock().lock();
        try
        {
            data = new Data(); // clear data
            data.setTransformConfigBeforeIncompleteTransformsAreRemoved(transformConfigBeforeIncompleteTransformsAreRemoved);
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
        log.error(msg);
    }

    @Override
    protected void logWarn(String msg)
    {
        log.warn(msg);
    }
}
