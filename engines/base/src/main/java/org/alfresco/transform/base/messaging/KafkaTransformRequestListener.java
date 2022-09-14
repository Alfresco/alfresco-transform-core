/*
 * Copyright 2015-2022 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.transform.base.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.transform.base.TransformController;
import org.alfresco.transform.client.model.TransformRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * TODO janv - hack'athon ;-)
 *
 * @author janv
 */
@Component
//@ConditionalOnProperty(prefix = "transformer.engine", name = "protocol", havingValue = "kafka")
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaTransformRequestListener {

    private static final Logger logger = LoggerFactory.getLogger(KafkaTransformRequestListener.class);
    
    @Value("${queue.engineRequestQueue}")
    private String requestQueue;

    @Autowired
    private TransformController transformController;

    @KafkaListener(topics = "${queue.engineRequestQueue}", groupId = "${queue.engineRequestQueue}")
    public void listenGroupTEngineTRequest(String message) {
        logger.info("Received Kafka Message in group " + requestQueue + ": " + message);

        // TODO janv - hack'athon ;-) - deserialize manually since @KafkaListener is not handling deserilization for some reason ...
        ObjectMapper mapper = new ObjectMapper();
        try {
            TransformRequest transformRequest = mapper.readValue(message, TransformRequest.class);
            logger.info("T-Request - deserialized Kafka Message: " + transformRequest);

            String topicName = transformRequest.getInternalContext().getReplyToDestination();

            transformController.transform(transformRequest, null, topicName);
        }
        catch (JsonMappingException e) {
            logger.error(e.getMessage(), e);
            // TODO reply with error
            /*    
            replyWithError(replyToQueue, HttpStatus.valueOf(e.getStatus().value()),
                    e.getMessage(), correlationId);
            return;
            */
        } 
        catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            // TODO reply with error
            /*    
            replyWithError(replyToQueue, HttpStatus.valueOf(e.getStatus().value()),
                    e.getMessage(), correlationId);
            return;
            */
        }
    }
}
