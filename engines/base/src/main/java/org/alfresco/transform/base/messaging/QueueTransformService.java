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
package org.alfresco.transform.base.messaging;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.util.Optional;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.stereotype.Component;

import org.alfresco.transform.base.TransformController;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.exceptions.TransformException;

/**
 * Queue Transformer service. This service reads all the requests for the particular engine, forwards them to the worker component (at this time the injected controller - to be refactored) and sends back the reply to the {@link Message#getJMSReplyTo()} value. If this value is missing we've got to a dead end.
 *
 * @author Lucian Tuca created on 18/12/2018
 */
@Component
@ConditionalOnProperty(name = "activemq.url")
public class QueueTransformService
{
    private static final Logger logger = LoggerFactory.getLogger(QueueTransformService.class);

    @Autowired
    private TransformController transformController;
    @Autowired
    private TransformMessageConverter transformMessageConverter;
    @Autowired
    private TransformReplySender transformReplySender;

    @JmsListener(destination = "${queue.engineRequestQueue}", concurrency = "${jms-listener.concurrency}")
    public void receive(final Message msg)
    {
        if (msg == null)
        {
            logger.error("Received null message!");
            return;
        }

        final String correlationId = tryRetrieveCorrelationId(msg);
        Destination replyToQueue;

        try
        {
            replyToQueue = msg.getJMSReplyTo();
            if (replyToQueue == null)
            {
                logger.error(
                        "Cannot find 'replyTo' destination queue for message with correlationID {}. Stopping. ",
                        correlationId);
                return;
            }
        }
        catch (JMSException e)
        {
            logger.error(
                    "Cannot find 'replyTo' destination queue for message with correlationID {}. Stopping. ",
                    correlationId);
            return;
        }

        logger.trace("New T-Request from queue with correlationId: {}", correlationId);

        Optional<TransformRequest> transformRequest;
        try
        {
            transformRequest = convert(msg, correlationId);
        }
        catch (TransformException e)
        {
            logger.error(e.getMessage(), e);
            replyWithError(replyToQueue, HttpStatus.valueOf(e.getStatus().value()),
                    e.getMessage(), correlationId);
            return;
        }

        if (!transformRequest.isPresent())
        {
            logger.error("T-Request from message with correlationID {} is null!", correlationId);
            replyWithInternalSvErr(replyToQueue,
                    "JMS exception during T-Request deserialization: ", correlationId);
            return;
        }

        transformController.transform(transformRequest.get(), null, replyToQueue);
    }

    /**
     * Tries to convert the JMS {@link Message} to a {@link TransformRequest} If any error occurs, a {@link TransformException} is thrown
     *
     * @param msg
     *            Message to be deserialized
     * @return The converted {@link TransformRequest} instance
     */
    private Optional<TransformRequest> convert(final Message msg, String correlationId)
    {
        try
        {
            TransformRequest request = (TransformRequest) transformMessageConverter.fromMessage(msg);
            return Optional.ofNullable(request);
        }
        catch (MessageConversionException e)
        {
            String message = "MessageConversionException during T-Request deserialization of message with correlationID "
                    + correlationId + ": ";
            throw new TransformException(BAD_REQUEST, message + e.getMessage());
        }
        catch (JMSException e)
        {
            String message = "JMSException during T-Request deserialization of message with correlationID "
                    + correlationId + ": ";
            throw new TransformException(INTERNAL_SERVER_ERROR, message + e.getMessage());
        }
        catch (Exception e)
        {
            String message = "Exception during T-Request deserialization of message with correlationID "
                    + correlationId + ": ";
            throw new TransformException(INTERNAL_SERVER_ERROR, message + e.getMessage());
        }
    }

    private void replyWithInternalSvErr(final Destination destination, final String msg,
            final String correlationId)
    {
        replyWithError(destination, INTERNAL_SERVER_ERROR, msg, correlationId);
    }

    private void replyWithError(final Destination replyToQueue, final HttpStatus status,
            final String msg,
            final String correlationId)
    {
        final TransformReply reply = TransformReply
                .builder()
                .withStatus(status.value())
                .withErrorDetails(msg)
                .build();

        transformReplySender.send(replyToQueue, reply, correlationId);
    }

    private static String tryRetrieveCorrelationId(final Message msg)
    {
        try
        {
            return msg.getJMSCorrelationID();
        }
        catch (Exception ignore)
        {
            return null;
        }
    }
}
