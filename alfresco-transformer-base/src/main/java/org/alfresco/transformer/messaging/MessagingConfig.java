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

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

import org.alfresco.transform.client.model.TransformRequestValidator;
import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.lang.NonNull;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * JMS and messaging configuration for the T-Engines. Contains the basic config in order to have the
 * T-Engine able to read from queues and send a reply back.
 *
 * @author Lucian Tuca
 * created on 18/12/2018
 */
@Configuration
public class MessagingConfig implements JmsListenerConfigurer
{
    private static final Logger logger = LoggerFactory.getLogger(MessagingConfig.class);

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
        final TransformMessageConverter transformMessageConverter)
    {
        final DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(transformMessageConverter);
        factory.setErrorHandler(t -> logger.error("JMS error: " + t.getMessage(), t));
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
    public Queue engineRequestQueue(@Value("${queue.engineRequestQueue}") String engineRequestQueueValue)
    {
        return new ActiveMQQueue(engineRequestQueueValue);
    }
}


