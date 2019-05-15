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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transformer.messaging.TransformMessageConverter;
import org.alfresco.transformer.messaging.TransformReplySender;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.support.converter.MessageConversionException;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

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

    @Before
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
        ActiveMQQueue destination = new ActiveMQQueue();
        msg.setJMSReplyTo(destination);

        TransformReply reply = TransformReply.builder()
            .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .withErrorDetails("JMS exception during T-Request deserialization: ").build();

        doReturn(null).when(transformMessageConverter).fromMessage(msg);

        queueTransformService.receive(msg);

        verify(transformMessageConverter).fromMessage(msg);
        verify(transformReplySender).send(destination, reply, null);

        verifyNoMoreInteractions(transformController);
    }

    @Test
    public void testConvertMessageThrowsMessageConversionExceptionThenReplyWithBadRequest()
        throws JMSException
    {
        ActiveMQObjectMessage msg = new ActiveMQObjectMessage();
        ActiveMQQueue destination = new ActiveMQQueue();
        msg.setJMSReplyTo(destination);

        TransformReply reply = TransformReply.builder().withStatus(HttpStatus.BAD_REQUEST.value())
            .withErrorDetails("Message conversion exception during T-Request deserialization: ")
            .build();

        doThrow(MessageConversionException.class).when(transformMessageConverter).fromMessage(msg);

        queueTransformService.receive(msg);

        verify(transformMessageConverter).fromMessage(msg);
        verify(transformReplySender).send(destination, reply, reply.getRequestId());

        verifyNoMoreInteractions(transformController);
    }

    @Test
    public void testConvertMessageThrowsJMSExceptionThenReplyWithInternalServerError()
        throws JMSException
    {
        ActiveMQObjectMessage msg = new ActiveMQObjectMessage();
        ActiveMQQueue destination = new ActiveMQQueue();
        msg.setJMSReplyTo(destination);

        TransformReply reply = TransformReply.builder().withStatus(HttpStatus.BAD_REQUEST.value())
            .withErrorDetails("JMS exception during T-Request deserialization: ").build();

        doThrow(JMSException.class).when(transformMessageConverter).fromMessage(msg);

        queueTransformService.receive(msg);

        verify(transformMessageConverter).fromMessage(msg);
        verify(transformReplySender).send(destination, reply, reply.getRequestId());

        verifyNoMoreInteractions(transformController);
    }

    @Test
    public void testWhenReceiveValidTransformRequestThenReplyWithSuccess() throws JMSException
    {
        ActiveMQObjectMessage msg = new ActiveMQObjectMessage();
        ActiveMQQueue destination = new ActiveMQQueue();
        msg.setJMSReplyTo(destination);

        TransformRequest request = new TransformRequest();
        TransformReply reply = TransformReply.builder().withStatus(HttpStatus.CREATED.value())
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
        TransformReply reply = TransformReply.builder().withStatus(HttpStatus.CREATED.value())
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
