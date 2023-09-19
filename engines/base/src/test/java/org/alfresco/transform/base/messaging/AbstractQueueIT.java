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

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.jms.Queue;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

/**
 * Checks that a t-engine can respond to its message queue. This is really just checking that
 * ${queue.engineRequestQueue} has been configured. The transform request can (and does fail
 * because the shared file store does not exist).
 *
 * @author Lucian Tuca
 * created on 15/01/2019
 */
@SpringBootTest(classes={org.alfresco.transform.base.Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"activemq.url=nio://localhost:61616"})
public abstract class AbstractQueueIT
{
    @Autowired
    private Queue engineRequestQueue;
    @Autowired
    private JmsTemplate jmsTemplate;
    private final ActiveMQQueue testingQueue = new ActiveMQQueue("org.alfresco.transform.engine.IT");

    @Test
    public void queueTransformServiceIT()
    {
        TransformRequest request = buildRequest();

        jmsTemplate.convertAndSend(engineRequestQueue, request, m -> {
            m.setJMSCorrelationID(request.getRequestId());
            m.setJMSReplyTo(testingQueue);
            return m;
        });

        this.jmsTemplate.setReceiveTimeout(1_000);
        TransformReply reply = (TransformReply) this.jmsTemplate.receiveAndConvert(testingQueue);

        // The transform may fail (for example the SFS is unavailable), but we check we get a response with the
        // correct id, so we know that the message was processed.
        assertEquals(request.getRequestId(), reply.getRequestId());
    }

    protected abstract TransformRequest buildRequest();
}
