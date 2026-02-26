package com.ms.email.services;

import com.ms.email.enums.StatusEmail;
import com.ms.email.models.EmailModel;
import com.ms.email.repositories.EmailRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class EmailService {

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.queue}")
    private String queueName;

    // Brevo
    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String brevoSenderEmail;

    @Value("${brevo.sender.name}")
    private String brevoSenderName;

    @Value("${germanoconsultancy.mail.to}")
    private String mailTo;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public EmailModel enqueueEmail(EmailModel emailModel) {
        emailModel.setStatusEmail(StatusEmail.QUEUED);
        emailModel.setSendDateEmail(LocalDateTime.now());

        EmailModel saved = emailRepository.save(emailModel);

        try {
            rabbitTemplate.convertAndSend("", queueName, saved.getEmailId());
        } catch (Exception e) {
            saved.setStatusEmail(StatusEmail.ERROR);
            emailRepository.save(saved);
            System.out.println("Rabbit publish error: " + e.getMessage());
            e.printStackTrace();
        }

        return saved;
    }

    @Transactional
    public void processQueuedEmail(UUID emailId) {
        Optional<EmailModel> opt = emailRepository.findById(emailId);
        if (opt.isEmpty()) return;

        EmailModel emailModel = opt.get();
        emailModel.setSendDateEmail(LocalDateTime.now());

        try {
            String subject = "Contato pelo site - " + safe(emailModel.getName());

            String html =
                    "<h2>Novo contato pelo site</h2>" +
                    "<p><b>Nome:</b> " + esc(safe(emailModel.getName())) + "</p>" +
                    "<p><b>Email:</b> " + esc(safe(emailModel.getUserEmail())) + "</p>" +
                    "<p><b>Empresa:</b> " + esc(safe(emailModel.getCompanyName())) + "</p>" +
                    "<p><b>Telefone:</b> " + esc(safe(emailModel.getTelephone())) + "</p>" +
                    "<p><b>Mensagem:</b><br/>" + esc(safe(emailModel.getMessage())).replace("\n", "<br/>") + "</p>";

            sendViaBrevo(
                    mailTo,
                    subject,
                    html,
                    emailModel.getUserEmail(), // reply-to (pra você responder direto pra pessoa)
                    emailModel.getName()
            );

            emailModel.setStatusEmail(StatusEmail.SENT);

        } catch (Exception e) {
            emailModel.setStatusEmail(StatusEmail.ERROR);
            System.out.println("Brevo error: " + e.getMessage());
            e.printStackTrace();
        }

        emailRepository.save(emailModel);
        System.out.println("Email Status: " + emailModel.getStatusEmail());
    }

    private void sendViaBrevo(String to, String subject, String htmlContent, String replyToEmail, String replyToName) {
        String url = "https://api.brevo.com/v3/smtp/email";

        Map<String, Object> body = new HashMap<>();
        body.put("sender", Map.of("email", brevoSenderEmail, "name", brevoSenderName));
        body.put("to", List.of(Map.of("email", to)));
        body.put("subject", subject);
        body.put("htmlContent", htmlContent);

        if (replyToEmail != null && !replyToEmail.isBlank()) {
            body.put("replyTo", Map.of(
                    "email", replyToEmail,
                    "name", safe(replyToName)
            ));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Brevo HTTP " + resp.getStatusCodeValue() + " - " + resp.getBody());
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String esc(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public org.springframework.data.domain.Page<EmailModel> findAll(org.springframework.data.domain.Pageable pageable) {
        return emailRepository.findAll(pageable);
    }

    public Optional<EmailModel> findById(UUID emailId) {
        return emailRepository.findById(emailId);
    }
}
