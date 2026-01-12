package com.gastrocontrol.gastrocontrol.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class StripeConfig {

    private final StripeProperties props;

    public StripeConfig(StripeProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        Stripe.apiKey = props.apiKey();
    }
}
