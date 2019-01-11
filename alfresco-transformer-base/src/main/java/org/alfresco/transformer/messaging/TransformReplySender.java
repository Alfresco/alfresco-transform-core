/*
 * Copyright 2015-2018 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.transformer.messaging;

import javax.jms.Destination;

import org.alfresco.transform.client.model.TransformReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * TODO: Duplicated from the Router
 * TransformReplySender Bean
 * <p/>
 * JMS message sender/publisher
 *
 * @author Cezar Leahu
 */
@Component
public class TransformReplySender
{
    private static final Logger logger = LoggerFactory.getLogger(TransformReplySender.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    public void send(final Destination destination, final TransformReply reply)
    {
        send(destination, reply, reply.getRequestId());
    }

    public void send(final Destination destination, final TransformReply reply, final String correlationId)
    {
        try
        {
            //jmsTemplate.setSessionTransacted(true); // do we need this?
            jmsTemplate.convertAndSend(destination, reply, m -> {
                m.setJMSCorrelationID(correlationId);
                return m;
            });
            logger.info("Sent: {} - with correlation ID {}", reply, correlationId);
        }
        catch (Exception e)
        {
            logger.error(
                "Failed to send T-Reply " + reply + " - for correlation ID " + correlationId, e);
        }
    }
}
