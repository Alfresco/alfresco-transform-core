/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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
package org.alfresco.transform.base;

import static org.alfresco.transform.messages.TransformStack.PIPELINE_FLAG;
import static org.alfresco.transform.messages.TransformStack.levelBuilder;
import static org.alfresco.transform.messages.TransformStack.setInitialTransformRequestOptions;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.jms.Queue;

import org.alfresco.transform.client.model.InternalContext;
import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.common.ExtensionService;
import org.alfresco.transform.messages.TransformStack;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

/**
 * @author Lucian Tuca
 * created on 15/01/2019
 */
@SpringBootTest(classes={org.alfresco.transform.base.Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"activemq.url=nio://localhost:61616"})
public abstract class AbstractQueueTransformServiceIT
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

        // Router.initialiseContext(TransformRequest)
        request.setInternalContext(InternalContext.initialise(request.getInternalContext()));
        request.setTargetExtension(ExtensionService.getExtensionForTargetMimetype(request.getTargetMediaType(),
            request.getSourceMediaType()));
        request.getInternalContext().getMultiStep().setInitialRequestId(request.getRequestId());
        request.getInternalContext().getMultiStep().setInitialSourceMediaType(request.getSourceMediaType());
        request.getInternalContext().setTransformRequestOptions(request.getTransformRequestOptions());
        setInitialTransformRequestOptions(request.getInternalContext(), request.getTransformRequestOptions());
        TransformStack.setInitialSourceReference(request.getInternalContext(), request.getSourceReference());

        TransformStack.addTransformLevel(request.getInternalContext(), levelBuilder(PIPELINE_FLAG) // pipeline of 1
            .withStep(
                "transformerName",
                request.getSourceMediaType(),
                request.getTargetMediaType()));
//        TransformStack.setReference(request.getInternalContext(), reference);

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
