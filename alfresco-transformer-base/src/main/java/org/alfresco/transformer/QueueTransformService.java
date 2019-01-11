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
        final String correlationId = tryRetrieveCorrelationId(msg);
        Destination replyToDestinationQueue;

        try
        {
            replyToDestinationQueue = msg.getJMSReplyTo();
        }
        catch (JMSException e)
        {
            logger.error("Cannot find 'replyTo' destination queue for message with correlationID {}. Stopping. ", correlationId);
            return;
        }

        logger.info("New T-Request from queue with correlationId: {0}");

        TransformReply reply = transformController.transform(convert(msg, correlationId, replyToDestinationQueue), null).getBody();

        transformReplySender.send(replyToDestinationQueue, reply);
    }

    /**
     * Tries to convert the JMS {@link Message} to a {@link TransformRequest}
     * If any errors occur standard error {@link TransformReply} are sent back
     *
     * @param msg Message to be deserialized
     * @param correlationId CorrelationId of the message
     * @param destination Needed in case deserialization fails. Passed here so we don't retrieve it again.
     * @return The converted {@link TransformRequest} instance
     */
    private TransformRequest convert(final Message msg, final String correlationId, Destination destination)
    {
        try
        {
            return (TransformRequest) transformMessageConverter.fromMessage(msg);
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
