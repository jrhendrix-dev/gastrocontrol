package com.gastrocontrol.gastrocontrol.application.service.mailer;

import com.gastrocontrol.gastrocontrol.application.port.EmailSender;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.UserJpaEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails (welcome emails, admin-created user emails, etc.).
 *
 * <p>Design rule: email failures must never block core business flows.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionalEmailService {

    private final EmailSender emailSender;

    /**
     * Sends a welcome email to a newly registered customer.
     *
     * @param user persisted user
     */
    public void sendWelcomeCustomer(UserJpaEntity user) {
        if (user == null || isBlank(user.getEmail())) return;

        String subject = "Welcome to GastroControl 👋";
        String htmlBody = """
                <h2>Welcome!</h2>
                <p>Your customer account is ready.</p>
                <p>You can now browse the menu, place orders, and track them in real time.</p>
                <p>If this wasn’t you, please contact support.</p>
                """;

        safeSend(user.getEmail(), displayName(user), subject, htmlBody);
    }

    public void sendInviteSetPassword(UserJpaEntity user, String setPasswordUrl) {
        if (user == null || isBlank(user.getEmail())) return;

        String subject = "Set your GastroControl password";
        String htmlBody = """
            <h2>You're invited to GastroControl</h2>
            <p>An administrator created an account for you.</p>
            <p>Set your password here:</p>
            <p><a href="%s">Set password</a></p>
            <p>If you didn’t expect this email, you can ignore it.</p>
            """.formatted(escapeHtml(setPasswordUrl));

        safeSend(user.getEmail(), displayName(user), subject, htmlBody);
    }

    public void sendPasswordReset(UserJpaEntity user, String resetUrl) {
        if (user == null || isBlank(user.getEmail())) return;

        String subject = "Reset your GastroControl password";
        String htmlBody = """
            <h2>Password reset requested</h2>
            <p>Reset your password here:</p>
            <p><a href="%s">Reset password</a></p>
            <p>If you didn't request this, you can ignore this email.</p>
            """.formatted(escapeHtml(resetUrl));

        safeSend(user.getEmail(), displayName(user), subject, htmlBody);
    }

    public void sendEmailChangeConfirmation(String toNewEmail, String displayName, String confirmUrl) {
        if (isBlank(toNewEmail)) return;

        String subject = "Confirm your GastroControl email change";
        String htmlBody = """
        <h2>Confirm your email change</h2>
        <p>You requested to change the email on your GastroControl account.</p>
        <p>Confirm here:</p>
        <p><a href="%s">Confirm email change</a></p>
        <p>If you didn't request this, you can ignore this email.</p>
        """.formatted(escapeHtml(confirmUrl));

        safeSend(toNewEmail, displayName == null ? "" : displayName, subject, htmlBody);
    }

    public void sendEmailChangedNotification(String toOldEmail, String newEmail) {
        if (isBlank(toOldEmail)) return;

        String subject = "Your GastroControl email was changed";
        String htmlBody = """
        <h2>Email changed</h2>
        <p>The email for your account was changed to: <b>%s</b></p>
        <p>If you didn't do this, please contact support immediately.</p>
        """.formatted(escapeHtml(newEmail));

        safeSend(toOldEmail, "", subject, htmlBody);
    }





    private void safeSend(String toEmail, String toName, String subject, String htmlBody) {
        try {
            emailSender.send(toEmail, toName, subject, htmlBody);
        } catch (Exception ex) {
            // Do not break core flow due to email provider issues.
            log.warn("Transactional email failed: to={} subject={} cause={}", toEmail, subject, ex.toString());
        }
    }

    private static String displayName(UserJpaEntity user) {
        String first = safe(user.getFirstName());
        String last = safe(user.getLastName());
        String full = (first + " " + last).trim();
        return full.isBlank() ? "" : full;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Minimal HTML escaping to avoid injection issues in email templates.
     */
    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
