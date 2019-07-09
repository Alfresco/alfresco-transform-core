package org.alfresco.transformer.messaging;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Prints JMS status information at application startup.
 *
 * @author Cezar Leahu
 */
@Configuration
public class MessagingInfo
{
    private static final Logger logger = LoggerFactory.getLogger(MessagingInfo.class);

    @Value("${activemq.url:}")
    private String activemqUrl;

    @PostConstruct
    public void init()
    {
        final boolean jms = activemqUrl != null && !activemqUrl.isBlank();
        logger.info("JMS client is {}, activemq.url: '{}'", jms ? "ENABLED" : "DISABLED",
            activemqUrl);
    }
}
