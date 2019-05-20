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
package org.alfresco.transformer;

import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_OPENXML_WORDPROCESSING;
import static org.alfresco.transform.client.model.Mimetype.MIMETYPE_PDF;
import static org.junit.Assert.assertEquals;

import java.util.UUID;

import javax.jms.Queue;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Lucian Tuca
 * created on 15/01/2019
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public abstract class AbstractQueueTransformServiceIT
{
    @Autowired
    private Queue engineRequestQueue;

    @Autowired
    private JmsTemplate jmsTemplate;

    private final ActiveMQQueue testingQueue = new ActiveMQQueue("org.alfresco.transform.engine.IT");

    @Test
    public void queueTransformServiceIT() {

        TransformRequest request = buildRequest();

        jmsTemplate.convertAndSend(engineRequestQueue, request, m -> {
            m.setJMSCorrelationID(request.getRequestId());
            m.setJMSReplyTo(testingQueue);
            return m;
        });

        this.jmsTemplate.setReceiveTimeout(1_000);
        TransformReply reply = (TransformReply) this.jmsTemplate.receiveAndConvert(testingQueue);
        assertEquals(request.getRequestId(), reply.getRequestId());
    }

    protected abstract TransformRequest buildRequest();
}
