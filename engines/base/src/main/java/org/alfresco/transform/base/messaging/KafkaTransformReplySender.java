/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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

import org.alfresco.transform.client.model.TransformReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * TODO janv - hack'athon ;-)
 *
 * @author janv
 */
@Component
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaTransformReplySender {

    private static final Logger logger = LoggerFactory.getLogger(KafkaTransformReplySender.class);

    @Autowired
    private KafkaTemplate<String, TransformReply> kafkaTemplate;

    public void send(final String topicName, final TransformReply reply) {
        send(topicName, reply, reply.getRequestId());
    }

    public void send(final String topicName, final TransformReply reply, final String correlationId)
    {
        if (topicName != null) {

            logger.debug("Send T-Reply to topic: "+topicName+"\n"+reply);

            // note: initially key-less (see "send") hence round-robin to partitions (see also "producerConfigs")
            // review pros/cons of using key in future, eg. maybe T-RequestId)
            ListenableFuture<SendResult<String, TransformReply>> future = 
                    kafkaTemplate.send(topicName, reply);

            future.addCallback(new ListenableFutureCallback<>() {
                @Override
                public void onSuccess(SendResult<String, TransformReply> result) {
                    logger.trace("Sent message=[" + reply +
                            "] with offset=[" + result.getRecordMetadata().offset() + "]");
                }
                @Override
                public void onFailure(Throwable ex) {
                    logger.error("Unable to send message=["
                            + reply + "] due to : " + ex.getMessage());
                }
            });
        }
    }
}
