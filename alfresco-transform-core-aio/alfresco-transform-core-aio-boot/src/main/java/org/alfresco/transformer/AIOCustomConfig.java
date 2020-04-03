package org.alfresco.transformer;

import jdk.jfr.Name;
import org.alfresco.transform.client.model.config.TransformConfig;
import org.alfresco.transform.client.registry.TransformServiceRegistry;
import org.alfresco.transformer.transformers.AllInOneTransformer;
import org.alfresco.transformer.transformers.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AIOCustomConfig
{

    @Bean("AllInOneTransformer")
    public Transformer aioTransformer()
    {
        return new AllInOneTransformer();
    }

    /**
     *
     * @return Override the TransformRegistryImpl used in {@link AbstractTransformerController}
     */
    @Bean
    @Primary
    public TransformServiceRegistry transformRegistryOverride()
    {
        return new TransformRegistryImpl()
        {

            @Autowired
            @Qualifier("AllInOneTransformer")
            Transformer transformer;

            @Override
            TransformConfig getTransformConfig()
            {
                return transformer.getTransformConfig();
            }
        };
    }
}
