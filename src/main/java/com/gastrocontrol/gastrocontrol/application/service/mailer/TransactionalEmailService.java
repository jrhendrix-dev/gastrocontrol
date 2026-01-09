package com.gastrocontrol.gastrocontrol.application.service.mailer;

import com.gastrocontrol.gastrocontrol.application.port.EmailSender;
import com.gastrocontrol.gastrocontrol.domain.enums.UserRole;
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

    /**
     * Sends an email when an admin creates an account for a user.
     *
     * <p>Since we don't have an invite/set-password token yet, we instruct them to use "Forgot password".</p>
     *
     * @param user persisted user created by admin
     * @param role role assigned to the user
     */
    public void sendAdminCreatedAccount(UserJpaEntity user, UserRole role) {
        if (user == null || isBlank(user.getEmail())) return;

        String subject = "Your GastroControl account was created";
        String htmlBody = """
                <h2>Your account is ready</h2>
                <p>An administrator created an account for you in GastroControl.</p>
                <ul>
                  <li><b>Email:</b> %s</li>
                  <li><b>Role:</b> %s</li>
                </ul>
                <p>To access your account, set your password using the <b>Forgot password</b> option on the login screen.</p>
                <p>If you believe this is a mistake, please contact support.</p>
                """.formatted(escapeHtml(user.getEmail()), escapeHtml(String.valueOf(role)));

        safeSend(user.getEmail(), displayName(user), subject, htmlBody);
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
