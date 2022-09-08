/*
 * Copyright 2015-2022 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.transform.base.clients;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;

/**
 * JMSClient
 *
 * Contains the bare minimum logic necessary for sending and receiving T-Request/Reply messages
 * through the basic vanilla ActiveMQ client.
 *
 * Used by Aspose t-engine and t-router, but likely to be useful in other t-engines.
 *
 * @author Cezar Leahu
 */
public class JmsClient
{
    private final ConnectionFactory factory;
    private final ActiveMQQueue queue;

    public JmsClient(final String server, final String queueName)
    {
        factory = new ActiveMQConnectionFactory(server);
        queue = new ActiveMQQueue(queueName);
    }

    public ActiveMQQueue getDestination()
    {
        return queue;
    }

    public void sendBytesMessage(final TransformRequest request)
        throws Exception
    {
        sendBytesMessage(request, request.getRequestId());
    }

    public void sendBytesMessage(final TransformRequest request, final String correlationID)
        throws Exception
    {
        sendBytesMessage(JacksonSerializer.serialize(request), correlationID);
    }

    public void sendBytesMessage(final TransformRequest request, final String correlationID,
        final Destination replyTo) throws Exception
    {
        sendBytesMessage(JacksonSerializer.serialize(request), correlationID, replyTo);
    }

    public void sendBytesMessage(final byte[] data, final String correlationID) throws
        Exception
    {
        try (final Connection connection = factory.createConnection();
             final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
             final MessageProducer producer = session.createProducer(queue))
        {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            final BytesMessage message = session.createBytesMessage();
            message.writeBytes(data);
            if (correlationID != null)
            {
                message.setJMSCorrelationID(correlationID);
            }
            producer.send(message);
        }
    }

    public void sendBytesMessage(final byte[] data, final String correlationID,
        final Destination replyTo) throws Exception
    {
        try (final Connection connection = factory.createConnection();
             final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
             final MessageProducer producer = session.createProducer(queue))
        {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            final BytesMessage message = session.createBytesMessage();
            message.writeBytes(data);
            if (correlationID != null)
            {
                message.setJMSCorrelationID(correlationID);
            }
            if (replyTo != null)
            {
                message.setJMSReplyTo(replyTo);
            }
            producer.send(message);
        }
    }

    public void sendTextMessage(final TransformRequest request)
        throws Exception
    {
        sendTextMessage(request, request.getRequestId());
    }

    public void sendTextMessage(final TransformRequest request, final String correlationID)
        throws Exception
    {
        sendTextMessage(new String(JacksonSerializer.serialize(request)), correlationID);
    }

    public void sendTextMessage(final String data, final String correlationID) throws
        Exception
    {
        try (final Connection connection = factory.createConnection();
             final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
             final MessageProducer producer = session.createProducer(queue))
        {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            final TextMessage message = session.createTextMessage(data);
            if (correlationID != null)
            {
                message.setJMSCorrelationID(correlationID);
            }
            producer.send(message);
        }
    }

    public TransformReply receiveMessage() throws Exception
    {
        return receiveMessage(2 * 60 * 1000); // 2 m
    }

    public TransformReply receiveMessage(final long timeout)
        throws Exception
    {
        try (final Connection connection = factory.createConnection();
             final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
             final MessageConsumer consumer = session.createConsumer(queue))
        {
            connection.start();

            final BytesMessage message = (BytesMessage) consumer.receive(timeout);
            if (message == null)
            {
                throw new Exception("No reply was received for the multi-step transform request");
            }
            final byte[] data = new byte[2048];
            int len = message.readBytes(data);
            return JacksonSerializer.deserialize(data, len, TransformReply.class);
        }
    }

    public void cleanQueue()
    {
        try
        {
            while (receiveMessage(2 * 1000) != null)
            {
            }
        }
        catch (Exception ignore)
        {
        }
    }
}
