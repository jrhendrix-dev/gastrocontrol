package com.gastrocontrol.gastrocontrol.config;

import com.gastrocontrol.gastrocontrol.application.port.EmailSender;
import com.gastrocontrol.gastrocontrol.infrastructure.mail.MailgunEmailSender;
import com.gastrocontrol.gastrocontrol.infrastructure.mail.NoopEmailSender;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    @Bean
    public EmailSender emailSender(MailProperties props) {
        if (props == null || !props.enabled()) {
            return new NoopEmailSender();
        }
        return new MailgunEmailSender(props);
    }
}
