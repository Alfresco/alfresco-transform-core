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
import javax.jms.JMSException;

import org.alfresco.transform.client.model.TransformReply;
import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Copied from the t-router.
 *
 * @author Cezar Leahu
 */
@Component
public class TransformReplySender
{
    private static final Logger logger = LoggerFactory.getLogger(TransformReplySender.class);

    @Autowired
    private JmsTemplate jmsTemplate;
    
    @Autowired
    private KafkaTemplate<String, TransformReply> kafkaTemplate;

    public void send(final Destination destination, final TransformReply reply)
    {
        send(destination, reply, reply.getRequestId());
    }

    public void send(final Destination destination, final TransformReply reply, final String correlationId)
    {
        if (destination != null)
        {
            try
            {
                jmsTemplate.convertAndSend(destination, reply, m -> {
                    m.setJMSCorrelationID(correlationId);
                    return m;
                });
                logger.trace("Sent: {} - with correlation ID {}", reply, correlationId);
            }
            catch (Exception e)
            {
                logger.error("Failed to send T-Reply " + reply + " - for correlation ID " + correlationId, e);
            }
        }

        // TODO janv - hack'athon ;-)
        if (destination instanceof ActiveMQQueue) {

            String topicName;
            try {
                topicName = (((ActiveMQQueue)destination).getQueueName());
            }
            catch (JMSException e) {
                logger.error("Failed to get queue name from the destination", e);
                return;
            }

            logger.debug("Send T-Reply to topic: "+topicName+"\n"+reply);

            ListenableFuture<SendResult<String, TransformReply>> future = 
                    kafkaTemplate.send(topicName, reply);

            future.addCallback(new ListenableFutureCallback<>() {
                @Override
                public void onSuccess(SendResult<String, TransformReply> result) {
                    logger.trace("Sent message=[" + reply +
                            "] with offset=[" + result.getRecordMetadata().offset() + "]");
                }
                @Override
                public void onFailure(Throwable ex) {
                    logger.error("Unable to send message=["
                            + reply + "] due to : " + ex.getMessage());
                }
            });
        }
    }
}
