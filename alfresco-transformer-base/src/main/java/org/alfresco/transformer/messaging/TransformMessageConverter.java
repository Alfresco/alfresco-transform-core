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
