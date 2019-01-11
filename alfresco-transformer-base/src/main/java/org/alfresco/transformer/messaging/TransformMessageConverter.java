/*
 * Copyright 2015-2018 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.transformer.messaging;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableMap;

/**
 * TODO: Duplicated from the Router
 * Custom wrapper over MappingJackson2MessageConverter for T-Request/T-Reply objects.
 *
 * @author Cezar Leahu
 */
@Service
public class TransformMessageConverter implements MessageConverter
{
    private static final Logger logger = LoggerFactory.getLogger(TransformMessageConverter.class);

    private static final MappingJackson2MessageConverter converter;
    private static final JavaType TRANSFORM_REQUEST_TYPE =
        TypeFactory.defaultInstance().constructType(TransformRequest.class);

    static
    {
        converter = new MappingJackson2MessageConverter()
        {
            @Override
            @NonNull
            protected JavaType getJavaTypeForMessage(final Message message) throws JMSException
            {
                if (message.getStringProperty("_type") == null)
                {
                    return TRANSFORM_REQUEST_TYPE;
                }
                return super.getJavaTypeForMessage(message);
            }
        };
        converter.setTargetType(MessageType.BYTES);
        converter.setTypeIdPropertyName("_type");
        converter.setTypeIdMappings(ImmutableMap.of(
            TransformRequest.class.getName(), TransformRequest.class,
            TransformReply.class.getName(), TransformReply.class)
        );
    }

    @Override
    @NonNull
    public Message toMessage(
        @NonNull final Object object,
        @NonNull final Session session) throws JMSException, MessageConversionException
    {
            return converter.toMessage(object, session);
    }

    @Override
    @NonNull
    public Object fromMessage(@NonNull final Message message) throws JMSException
    {
            return converter.fromMessage(message);
    }
}
