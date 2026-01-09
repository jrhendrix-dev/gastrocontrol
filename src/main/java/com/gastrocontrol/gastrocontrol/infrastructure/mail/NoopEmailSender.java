package com.gastrocontrol.gastrocontrol.infrastructure.mail;

import com.gastrocontrol.gastrocontrol.application.port.EmailSender;

/**
 * Dev-friendly no-op sender to avoid blocking startup when no email credentials are present.
 */
public class NoopEmailSender implements EmailSender {
    @Override
    public void send(String toEmail, String toName, String subject, String htmlBody) {
        // intentionally no-op
    }
}
