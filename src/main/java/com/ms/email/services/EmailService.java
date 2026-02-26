package com.ms.email.services;

import com.ms.email.enums.StatusEmail;
import com.ms.email.models.EmailModel;
import com.ms.email.repositories.EmailRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailService {

    @Autowired
    EmailRepository emailRepository;

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.queue}")
    private String queueName;

    @Transactional
    public EmailModel enqueueEmail(EmailModel emailModel) {
        emailModel.setStatusEmail(StatusEmail.QUEUED);
        EmailModel saved = emailRepository.save(emailModel);
        rabbitTemplate.convertAndSend("", queueName, saved.getEmailId());

        return saved;
    }

    @Transactional
    public void processQueuedEmail(UUID emailId) {
        Optional<EmailModel> opt = emailRepository.findById(emailId);
        if (!opt.isPresent()) return;

        EmailModel emailModel = opt.get();
        emailModel.setSendDateEmail(LocalDateTime.now());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailModel.getEmailConsultancy());
            message.setReplyTo(emailModel.getUserEmail());

            message.setSubject("Contato pelo site - " + emailModel.getName());

            String body =
                    "Nome: " + emailModel.getName() + "\n" +
                            "Email: " + emailModel.getUserEmail() + "\n" +
                            "Empresa: " + (emailModel.getCompanyName() == null ? "" : emailModel.getCompanyName()) + "\n" +
                            "Telefone: " + (emailModel.getTelephone() == null ? "" : emailModel.getTelephone()) + "\n\n" +
                            "Mensagem:\n" + emailModel.getMessage();

            message.setText(body);

            emailSender.send(message);
            emailModel.setStatusEmail(StatusEmail.SENT);

        } catch (MailException e) {
            emailModel.setStatusEmail(StatusEmail.ERROR);
            System.out.println("MailException: " + e.getMessage());
            e.printStackTrace();
        }

        emailRepository.save(emailModel);
        System.out.println("Email Status: " + emailModel.getStatusEmail());
    }

    public org.springframework.data.domain.Page<EmailModel> findAll(org.springframework.data.domain.Pageable pageable) {
        return emailRepository.findAll(pageable);
    }

    public Optional<EmailModel> findById(UUID emailId) {
        return emailRepository.findById(emailId);
    }
}