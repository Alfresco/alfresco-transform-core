/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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
package org.alfresco.transform.base.messaging;

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

    public void send(final Destination destination, final TransformReply reply,
        final String correlationId)
    {
        try
        {
            //jmsTemplate.setSessionTransacted(true); // do we need this?
            jmsTemplate.convertAndSend(destination, reply, m -> {
                m.setJMSCorrelationID(correlationId);
                return m;
            });
            logger.trace("Sent: {} - with correlation ID {}", reply, correlationId);
        }
        catch (Exception e)
        {
            logger.error(
                "Failed to send T-Reply " + reply + " - for correlation ID " + correlationId, e);
        }
    }
}
