package com.gastrocontrol.gastrocontrol.application.port;

/**
 * Port for sending transactional emails.
 * Infrastructure will provide an implementation (e.g., MailerSend).
 */
public interface EmailSender {

    /**
     * Sends an email.
     *
     * @param toEmail recipient email
     * @param toName recipient display name (optional)
     * @param subject subject
     * @param htmlBody html content
     */
    void send(String toEmail, String toName, String subject, String htmlBody);
}
