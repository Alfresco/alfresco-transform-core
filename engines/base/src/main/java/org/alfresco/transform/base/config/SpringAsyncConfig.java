/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2022 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.base.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class SpringAsyncConfig implements AsyncConfigurer
{
    private static final Logger logger = LoggerFactory.getLogger(SpringAsyncConfig.class);

    @Value("${async-task-executor.core-pool-size:1}")
    int corePoolSize;

    @Value("${async-task-executor.max-pool-size:"+Integer.MAX_VALUE+"}")
    int maxPoolSize;

    @Value("${async-task-executor.keep-alive-seconds:60}")
    int keepAliveSeconds;

    @Value("${async-task-executor.queue-capacity:"+Integer.MAX_VALUE+"}")
    int queueCapacity;

    @Value("${async-task-executor.allow-core-thread-time-out:false}")
    boolean allowCoreThreadTimeOut;

    @Value("${async-task-executor.prestart-all-core-threads:false}")
    boolean prestartAllCoreThreads;

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor()
    {
        logger.debug("async-task-executor:");
        logger.debug("  corePoolSize="+corePoolSize);
        logger.debug("  max-pool-size: "+maxPoolSize);
        logger.debug("  keep-alive-seconds: "+keepAliveSeconds);
        logger.debug("  queue-capacity: "+queueCapacity);
        logger.debug("  allow-core-thread-time-out: "+allowCoreThreadTimeOut);
        logger.debug("  prestart-all-core-threads: "+prestartAllCoreThreads);
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setQueueCapacity(queueCapacity);
        executor.setAllowCoreThreadTimeOut(allowCoreThreadTimeOut);
        executor.setPrestartAllCoreThreads(prestartAllCoreThreads);
        return executor;
    }
}
