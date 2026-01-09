package com.gastrocontrol.gastrocontrol.controller.dev;

import com.gastrocontrol.gastrocontrol.application.port.EmailSender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Profile("dev")
@RestController
@RequestMapping("/api/dev/mail")
public class DevMailController {

    private final EmailSender emailSender;

    public DevMailController(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public record TestMailRequest(
            @Email @NotBlank String toEmail,
            String toName
    ) {}

    @PostMapping("/test")
    public ResponseEntity<?> sendTest(@RequestBody TestMailRequest req) {
        System.out.println("DevMailController hit -> " + req.toEmail());
        emailSender.send(
                req.toEmail(),
                req.toName() == null ? "" : req.toName(),
                "GastroControl SMTP test ✅",
                "<h3>It works!</h3><p>This is a test email from dev profile.</p>"
        );
        return ResponseEntity.ok().body("sent");
    }
}
