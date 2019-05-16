/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
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
