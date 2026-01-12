package com.gastrocontrol.gastrocontrol.infrastructure.mail;

import com.gastrocontrol.gastrocontrol.application.port.EmailSender;
import com.gastrocontrol.gastrocontrol.common.exception.EmailSendException;
import com.gastrocontrol.gastrocontrol.config.MailProperties;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class MailgunEmailSender implements EmailSender {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final String apiKey;
    private final String domain;
    private final String baseUrl;
    private final String fromEmail;
    private final String fromName;

    public MailgunEmailSender(MailProperties props) {
        Objects.requireNonNull(props);
        Objects.requireNonNull(props.from());
        Objects.requireNonNull(props.mailgun());

        this.apiKey = requireNonBlank(props.mailgun().apiKey(), "mail.mailgun.api-key");
        this.domain = requireNonBlank(props.mailgun().domain(), "mail.mailgun.domain");
        this.baseUrl = requireNonBlank(props.mailgun().baseUrl(), "mail.mailgun.base-url");

        this.fromEmail = requireNonBlank(props.from().email(), "mail.from.email");
        this.fromName = props.from().name() == null ? "" : props.from().name();
    }

    @Override
    public void send(String toEmail, String toName, String subject, String htmlBody) {
        try {
            String to = (toName == null || toName.isBlank())
                    ? toEmail
                    : String.format("%s <%s>", toName, toEmail);

            String from = (fromName == null || fromName.isBlank())
                    ? fromEmail
                    : String.format("%s <%s>", fromName, fromEmail);

            String form = ""
                    + "from=" + enc(from)
                    + "&to=" + enc(to)
                    + "&subject=" + enc(subject)
                    + "&html=" + enc(htmlBody);

            String url = baseUrl.replaceAll("/+$", "") + "/v3/" + domain + "/messages";

            String basicAuth = Base64.getEncoder()
                    .encodeToString(("api:" + apiKey).getBytes(StandardCharsets.UTF_8));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + basicAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                String msg = "Mailgun send failed: HTTP " + resp.statusCode()
                        + " (" + resp.body() + ")"
                        + " | url=" + url
                        + " | domain=" + domain;
                throw new EmailSendException(msg);
            }



        } catch (Exception e) {
            throw new RuntimeException("Failed to send email via Mailgun", e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String requireNonBlank(String v, String key) {
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing config: " + key);
        return v;
    }
}
