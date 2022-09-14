/*
 * Copyright 2015-2022 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.transform.base.clients;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

/**
 * TODO janv - hack'athon ;-)
 *  
 * KafkaClient
 *
 * Contains the bare minimum logic necessary for tests to send and receive T-Request/Reply messages
 * using the basic vanilla Kafka client. For these tests, one client per topic.
 *
 * @author janv
 */
public class KafkaClient
{
    private final String bootstrapServers;
    private final String topicName;

    private Producer<String, String> producer;
    private Consumer<String, String> consumer;

    public KafkaClient(final String bootstrapServers, final String topicName)
    {
        this.bootstrapServers = bootstrapServers;
        this.topicName = topicName;

        initProducer();
        initConsumer();
    }

    public void close() {
        producer.close();
        consumer.close();
    }

    private void initProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("linger.ms", 1);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        producer = new KafkaProducer<>(props);
    }
    
    private void initConsumer() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", bootstrapServers);
        props.setProperty("group.id", "test");
        props.setProperty("enable.auto.commit", "true");
        props.setProperty("auto.commit.interval.ms", "1000");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topicName));
    }

    public void sendMessage(final TransformRequest request) throws Exception {
        sendMessage(request, request.getRequestId());
    }

    public void sendMessage(final TransformRequest request, final String correlationID) throws Exception {
        sendMessage(new String(JacksonSerializer.serialize(request)), correlationID);
    }

    public void sendMessage(final String data, final String correlationID) throws Exception {
        // note: initially key-less (see "send") hence round-robin to partitions (see also "producerConfigs")
        // review pros/cons of using key in future, eg. maybe T-RequestId)        
        producer.send(new ProducerRecord<>(topicName, data));
    }

    public TransformReply receiveMessage() throws Exception
    {
        return receiveMessage(2 * 60 * 1000); // 2 m
    }
    
    // hack - for now, just return the most recently received message (during the poll)
    public TransformReply receiveMessage(final long timeout) throws Exception
    {
        TransformReply result = null;

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(timeout));

        for (ConsumerRecord<String, String> record : records) {
            System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());

            int len = record.value().length();
            result = JacksonSerializer.deserialize(record.value().getBytes(StandardCharsets.UTF_8), len, TransformReply.class);
        }
    
        return result;
    }

    public void cleanQueue() {
        try {
            while (receiveMessage(2 * 1000) != null) {
            }
        }
        catch (Exception ignore) {
        }
    }
}
