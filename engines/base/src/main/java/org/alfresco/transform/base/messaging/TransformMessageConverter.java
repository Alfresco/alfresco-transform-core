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

import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableMap;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;

import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.stereotype.Service;

import org.alfresco.transform.client.model.TransformReply;
import org.alfresco.transform.client.model.TransformRequest;

/**
 * Copied from the t-router. We would need to create a common dependency between t-engine base and t-router that knows about jms to remove this duplication.
 *
 * @author Cezar Leahu
 */
@Service
public class TransformMessageConverter implements MessageConverter
{
    private static final JacksonJsonMessageConverter converter;
    private static final JavaType TRANSFORM_REQUEST_TYPE = TypeFactory.createDefaultInstance().constructType(TransformRequest.class);

    static
    {
        converter = new JacksonJsonMessageConverter() {
            @Override
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
                TransformReply.class.getName(), TransformReply.class));
    }

    @Override
    public Message toMessage(
            final Object object,
            final Session session) throws JMSException, MessageConversionException
    {
        return converter.toMessage(object, session);
    }

    @Override
    public Object fromMessage(final Message message) throws JMSException
    {
        return converter.fromMessage(message);
    }
}
