/*
 * Copyright 2015-2018 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.transform.base.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ErrorHandler;

/**
 * Extensible Error Handler for JMS exceptions
 *
 * @author Cezar Leahu
 */
@Service
public class MessagingErrorHandler implements ErrorHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MessagingErrorHandler.class);

    @Override
    public void handleError(Throwable t)
    {
        logger.error("JMS error: " + t.getMessage(), t);
    }
}
