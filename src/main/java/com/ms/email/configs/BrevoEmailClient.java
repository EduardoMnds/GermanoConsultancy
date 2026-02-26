import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class BrevoEmailClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    public void sendContactEmail(String toEmail, String subject, String htmlContent, String replyToEmail, String replyToName) {

        String url = "https://api.brevo.com/v3/smtp/email";

        Map<String, Object> body = new HashMap<>();
        body.put("sender", Map.of("email", senderEmail, "name", senderName));
        body.put("to", List.of(Map.of("email", toEmail)));
        body.put("subject", subject);
        body.put("htmlContent", htmlContent);

        // pra você responder o usuário direto no “Responder”
        body.put("replyTo", Map.of("email", replyToEmail, "name", replyToName));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Brevo error: " + resp.getStatusCode() + " - " + resp.getBody());
        }
    }
}
