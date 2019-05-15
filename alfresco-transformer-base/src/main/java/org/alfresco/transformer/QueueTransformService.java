/*
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
 *
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
 */
package org.alfresco.transformer;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transformer.messaging.TransformMessageConverter;
import org.alfresco.transformer.messaging.TransformReplySender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.stereotype.Component;

/**
 * Queue Transformer service.
 * This service reads all the requests for the particular engine, forwards them to the worker
 * component (at this time the injected controller - to be refactored) and sends back the reply
 * to the {@link Message#getJMSReplyTo()} value. If this value is missing we've got to a dead end.
 *
 * @author Lucian Tuca
 * created on 18/12/2018
 */
@Component
public class QueueTransformService
{
    private static final Logger logger = LoggerFactory.getLogger(QueueTransformService.class);


    // TODO: I know this is not smart but all the the transformation logic is in the Controller.
    // The controller also manages the probes. There's tons of refactoring needed there, hence this. Sorry.
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
        Destination replyToDestinationQueue;

        try
        {
            replyToDestinationQueue = msg.getJMSReplyTo();
            if (replyToDestinationQueue == null)
            {
                logger.error(
                    "Cannot find 'replyTo' destination queue for message with correlationID {}. Stopping. ",
                    correlationId);
                return;
            }
        }
        catch (JMSException e)
        {
            logger.error("Cannot find 'replyTo' destination queue for message with correlationID {}. Stopping. ", correlationId);
            return;
        }

        logger.info("New T-Request from queue with correlationId: {0}", correlationId);


        TransformRequest transformRequest = convert(msg, correlationId, replyToDestinationQueue);
        if (transformRequest == null)
        {
            logger.error("Exception during T-Request deserialization! T-Reply with error has been "
                + "sent to T-Router!");
            return;
        }

        // Tries to convert and return the object. If it fails to convert, the method sends an error message and returns null.
        TransformReply reply = transformController.transform(
            transformRequest, null).getBody();

        transformReplySender.send(replyToDestinationQueue, reply);
    }

    /**
     * Tries to convert the JMS {@link Message} to a {@link TransformRequest}
     * If any errors occur standard error {@link TransformReply} are sent back to T-Router
     *
     * @param msg Message to be deserialized
     * @param correlationId CorrelationId of the message
     * @param destination Needed in case deserialization fails. Passed here so we don't retrieve it again.
     * @return The converted {@link TransformRequest} instance or null in case of errors
     */
    private TransformRequest convert(final Message msg, final String correlationId, Destination destination)
    {
        try
        {
            TransformRequest request = (TransformRequest) transformMessageConverter
                .fromMessage(msg);
            if (request == null)
            {
                logger.error("T-Request is null deserialization!");
                replyWithInternalSvErr(destination,
                    "JMS exception during T-Request deserialization: ", correlationId);
            }
            return request;
        }
        catch (MessageConversionException e)
        {
            String message = "Message conversion exception during T-Request deserialization: ";
            logger.error(message + e.getMessage(), e);
            replyWithBadRequest(destination, message + e.getMessage(), correlationId);
        }
        catch (JMSException e)
        {
            String message = "JMS exception during T-Request deserialization: ";
            logger.error(message + e.getMessage(), e);
            replyWithInternalSvErr(destination, message + e.getMessage(), correlationId);
        }
        catch (Exception e)
        {
            String message = "Exception during T-Request deserialization: ";
            logger.error(message + e.getMessage(), e);
            replyWithInternalSvErr(destination, message + e.getMessage(), correlationId);
        }
        catch (Throwable t)
        {
            logger.error("Error during T-Request deserialization" + t.getMessage(), t);
            throw t;
        }
        return null;
    }


    private void replyWithBadRequest(final Destination destination, final String msg, final String correlationId)
    {
        replyWithError(destination, HttpStatus.BAD_REQUEST, msg, correlationId);
    }

    private void replyWithInternalSvErr(final Destination destination, final String msg, final String correlationId)
    {
        replyWithError(destination, HttpStatus.INTERNAL_SERVER_ERROR, msg, correlationId);
    }

    private void replyWithError(final Destination destination, final HttpStatus status, final String msg,
        final String correlationId)
    {
        final TransformReply reply = TransformReply.builder()
            .withStatus(status.value())
            .withErrorDetails(msg)
            .build();

        transformReplySender.send(destination, reply, correlationId);
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
