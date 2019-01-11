package org.alfresco.transformer.messaging;

import javax.jms.ConnectionFactory;

import org.alfresco.transform.client.model.TransformRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
}


