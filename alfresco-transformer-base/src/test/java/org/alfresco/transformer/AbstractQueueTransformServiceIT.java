/*
 * #%L
 * Alfresco Enterprise Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
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
