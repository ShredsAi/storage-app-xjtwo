package ai.shreds.application.config;

import ai.shreds.domain.services.DomainServiceImageFactory;
import ai.shreds.domain.services.DomainServiceVariantPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationBeansConfig {

    @Bean
    public DomainServiceImageFactory domainServiceImageFactory() {
        return new DomainServiceImageFactory();
    }

    @Bean
    public DomainServiceVariantPolicy domainServiceVariantPolicy() {
        return new DomainServiceVariantPolicy();
    }
}
