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

import org.alfresco.transform.messages.TransformRequestValidator;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.lang.NonNull;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Queue;

/**
 * JMS and messaging configuration for the T-Engines. Contains the basic config in order to have the
 * T-Engine able to read from queues and send a reply back.
 *
 * @author Lucian Tuca
 * created on 18/12/2018
 */
@Configuration
@ConditionalOnProperty(name = "activemq.url")
public class MessagingConfig implements JmsListenerConfigurer
{
    @Override
    public void configureJmsListeners(@NonNull JmsListenerEndpointRegistrar registrar)
    {
        registrar.setMessageHandlerMethodFactory(methodFactory());
    }

    @Bean
    public DefaultMessageHandlerMethodFactory methodFactory()
    {
        DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
        factory.setValidator(new TransformRequestValidator());
        return factory;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
        final ConnectionFactory connectionFactory,
        final TransformMessageConverter transformMessageConverter,
        final MessagingErrorHandler messagingErrorHandler)
    {
        final DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(transformMessageConverter);
        factory.setErrorHandler(messagingErrorHandler);
        factory.setTransactionManager(transactionManager(connectionFactory));
        return factory;
    }

    @Bean
    public PlatformTransactionManager transactionManager(final ConnectionFactory connectionFactory)
    {
        final JmsTransactionManager transactionManager = new JmsTransactionManager();
        transactionManager.setConnectionFactory(connectionFactory);
        return transactionManager;
    }

    @Bean
    public Queue engineRequestQueue(
        @Value("${queue.engineRequestQueue}") String engineRequestQueueValue)
    {
        return new ActiveMQQueue(engineRequestQueueValue);
    }
}


