/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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

package org.alfresco.transformer;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transformer.messaging.TransformMessageConverter;
import org.alfresco.transformer.messaging.TransformReplySender;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.support.converter.MessageConversionException;

public class QueueTransformServiceTest
{
    @Mock
    private TransformController transformController;
    @Mock
    private TransformMessageConverter transformMessageConverter;
    @Mock
    private TransformReplySender transformReplySender;

    @InjectMocks
    private QueueTransformService queueTransformService;

    @BeforeEach
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testWhenReceiveNullMessageThenStopFlow()
    {
        queueTransformService.receive(null);

        verifyNoMoreInteractions(transformController);
        verifyNoMoreInteractions(transformMessageConverter);
        verifyNoMoreInteractions(transformReplySender);
    }

    @Test
    public void testWhenReceiveMessageWithNoReplyToQueueThenStopFlow()
    {
        queueTransformService.receive(new ActiveMQObjectMessage());

        verifyNoMoreInteractions(transformController);
        verifyNoMoreInteractions(transformMessageConverter);
        verifyNoMoreInteractions(transformReplySender);
    }

    @Test
    public void testConvertMessageReturnsNullThenReplyWithInternalServerError() throws JMSException
    {
        ActiveMQObjectMessage msg = new ActiveMQObjectMessage();
        msg.setCorrelationId("1234");
        ActiveMQQueue destination = new ActiveMQQueue();
        msg.setJMSReplyTo(destination);

        TransformReply reply = TransformReply
            .builder()
            .withStatus(INTERNAL_SERVER_ERROR.value())
            .withErrorDetails(
                "JMS exception during T-Request deserialization of message with correlationID "
                + msg.getCorrelationId() + ": null")
            .build();

        doReturn(null).when(transformMessageConverter).fromMessage(msg);

        queueTransformService.receive(msg);

        verify(transformMessageConverter).fromMessage(msg);
        verify(transformReplySender).send(destination, reply, msg.getCorrelationId());

        verifyNoMoreInteractions(transformController);
    }

    @Test
    public void testConvertMessageThrowsMessageConversionExceptionThenReplyWithBadRequest()
        throws JMSException
    {
        ActiveMQObjectMessage msg = new ActiveMQObjectMessage();
        msg.setCorrelationId("1234");
        ActiveMQQueue destination = new ActiveMQQueue();
        msg.setJMSReplyTo(destination);

        TransformReply reply = TransformReply
            .builder()
            .withStatus(BAD_REQUEST.value())
            .withErrorDetails(
                "Message conversion exception during T-Request deserialization of message with correlationID"
                + msg.getCorrelationId() + ": null")
            .build();

        doThrow(MessageConversionException.class).when(transformMessageConverter).fromMessage(msg);

        queueTransformService.receive(msg);

        verify(transformMessageConverter).fromMessage(msg);
        verify(transformReplySender).send(destination, reply, msg.getCorrelationId());

        verifyNoMoreInteractions(transformController);
    }

    @Test
    public void testConvertMessageThrowsJMSExceptionThenReplyWithInternalServerError()
        throws JMSException
    {
        ActiveMQObjectMessage msg = new ActiveMQObjectMessage();
        msg.setCorrelationId("1234");
        ActiveMQQueue destination = new ActiveMQQueue();
        msg.setJMSReplyTo(destination);

        TransformReply reply = TransformReply
            .builder()
            .withStatus(INTERNAL_SERVER_ERROR.value())
            .withErrorDetails(
                "JMSException during T-Request deserialization of message with correlationID " +
                msg.getCorrelationId() + ": null")
            .build();

        doThrow(JMSException.class).when(transformMessageConverter).fromMessage(msg);

        queueTransformService.receive(msg);

        verify(transformMessageConverter).fromMessage(msg);
        verify(transformReplySender).send(destination, reply, msg.getCorrelationId());

        verifyNoMoreInteractions(transformController);
    }

    @Test
    public void testWhenReceiveValidTransformRequestThenReplyWithSuccess() throws JMSException
    {
        ActiveMQObjectMessage msg = new ActiveMQObjectMessage();
        ActiveMQQueue destination = new ActiveMQQueue();
        msg.setJMSReplyTo(destination);

        TransformRequest request = new TransformRequest();
        TransformReply reply = TransformReply
            .builder()
            .withStatus(CREATED.value())
            .build();

        doReturn(request).when(transformMessageConverter).fromMessage(msg);
        doReturn(new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus())))
            .when(transformController).transform(request, null);

        queueTransformService.receive(msg);

        verify(transformMessageConverter).fromMessage(msg);
        verify(transformController).transform(request, null);
        verify(transformReplySender).send(destination, reply);
    }

    @Test
    public void testWhenJMSExceptionOnMessageIsThrownThenStopFlow() throws JMSException
    {
        Message msg = mock(Message.class);

        doThrow(JMSException.class).when(msg).getJMSReplyTo();

        queueTransformService.receive(msg);

        verifyNoMoreInteractions(transformController);
        verifyNoMoreInteractions(transformMessageConverter);
        verifyNoMoreInteractions(transformReplySender);
    }

    @Test
    public void testWhenExceptionOnCorrelationIdIsThrownThenContinueFlowWithNullCorrelationId()
        throws JMSException
    {
        Message msg = mock(Message.class);
        Destination destination = mock(Destination.class);

        doThrow(JMSException.class).when(msg).getJMSCorrelationID();
        doReturn(destination).when(msg).getJMSReplyTo();

        TransformRequest request = new TransformRequest();
        TransformReply reply = TransformReply
            .builder()
            .withStatus(CREATED.value())
            .build();

        doReturn(request).when(transformMessageConverter).fromMessage(msg);
        doReturn(new ResponseEntity<>(reply, HttpStatus.valueOf(reply.getStatus())))
            .when(transformController).transform(request, null);

        queueTransformService.receive(msg);

        verify(transformMessageConverter).fromMessage(msg);
        verify(transformController).transform(request, null);
        verify(transformReplySender).send(destination, reply);
    }
}
