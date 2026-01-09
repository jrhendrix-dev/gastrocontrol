package com.gastrocontrol.gastrocontrol.service.mailer;

import com.gastrocontrol.gastrocontrol.application.port.EmailSender;
import org.springframework.stereotype.Service;

@Service
public class UserMailer {

    private final EmailSender emailSender;

    public UserMailer(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendWelcome(String toEmail, String toName) {
        String safeName = (toName == null || toName.isBlank()) ? "there" : toName;

        String subject = "Welcome to GastroControl";
        String html = """
                <h2>Welcome, %s 👋</h2>
                <p>Your account is ready.</p>
                <p>If this wasn’t you, you can ignore this email.</p>
                """.formatted(escapeHtml(safeName));

        emailSender.send(toEmail, safeName, subject, html);
    }

    // minimal escape to avoid breaking HTML; can be replaced later with a real templating solution
    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
