/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2023 Alfresco Software Limited
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
package org.alfresco.transformer.messaging;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @deprecated will be removed in a future release. Replaced by alfresco-base-t-engine.
 *
 * Prints JMS status information at application startup.
 *
 * @author Cezar Leahu
 */
@Deprecated
@Configuration
public class MessagingInfo
{
    private static final Logger logger = LoggerFactory.getLogger(MessagingInfo.class);

    @Value("${activemq.url:}")
    private String activemqUrl;

    @PostConstruct
    public void init()
    {
        // For backwards-compatibility, we continue to rely on setting ACTIVEMQ_URL environment variable (see application.yaml)
        // The MessagingConfig class uses on ConditionalOnProperty (ie. activemq.url is set and not false)

        // Note: as per application.yaml the broker url is appended with ACTIVEMQ_CONN_OPTIONS with default value "?jms.watchTopicAdvisories=false".
        // If this needs to be fully overridden then it would require explicitly setting both "spring.activemq.broker-url"
        // *and* "activemq.url" (latter to non-false value). ACTIVEMQ_CONN_OPTIONS value will be ignored in that case.

        if ((activemqUrl != null) && (! activemqUrl.equals("false")))
        {
            logger.info("JMS client is ENABLED - ACTIVEMQ_URL ='{}'", activemqUrl);
        }
        else
        {
            logger.info("JMS client is DISABLED - ACTIVEMQ_URL is not set");
        }
    }
}
