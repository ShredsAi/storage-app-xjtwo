package ai.shreds.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfrastructurePersistenceConfig {

    @Bean
    @ConditionalOnMissingBean(InfrastructureJpaEnumConverters.class)
    public InfrastructureJpaEnumConverters enumConverters() {
        return new InfrastructureJpaEnumConverters();
    }
}
