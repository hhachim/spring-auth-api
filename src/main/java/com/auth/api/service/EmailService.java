package com.auth.api.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.email-from}")
    private String emailFrom;

    @Value("${app.email-from-name}")
    private String emailFromName;

    /**
     * Envoie un email de réinitialisation de mot de passe
     * 
     * @param to Destinataire de l'email
     * @param token Token de réinitialisation
     */
    @Async
    public void sendPasswordResetEmail(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, 
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, 
                    StandardCharsets.UTF_8.name());

            // Préparation du contexte pour le template
            Map<String, Object> variables = new HashMap<>();
            variables.put("resetUrl", frontendUrl + "/reset-password?token=" + token);
            variables.put("appName", emailFromName);

            Context context = new Context();
            context.setVariables(variables);
            
            // Construction du contenu HTML à partir du template
            String html = templateEngine.process("reset-password-email", context);

            // Configuration du message
            helper.setTo(to);
            helper.setSubject("Réinitialisation de votre mot de passe");
            helper.setText(html, true);
            helper.setFrom(emailFrom, emailFromName);

            mailSender.send(message);
            logger.info("Email de réinitialisation de mot de passe envoyé à: {}", to);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de réinitialisation: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email de réinitialisation", e);
        }
    }

    /**
     * Envoie un email de confirmation après la réinitialisation du mot de passe
     * 
     * @param to Destinataire de l'email
     */
    @Async
    public void sendPasswordResetConfirmationEmail(String to) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, 
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, 
                    StandardCharsets.UTF_8.name());

            // Préparation du contexte pour le template
            Map<String, Object> variables = new HashMap<>();
            variables.put("loginUrl", frontendUrl + "/login");
            variables.put("appName", emailFromName);

            Context context = new Context();
            context.setVariables(variables);
            
            // Construction du contenu HTML à partir du template
            String html = templateEngine.process("reset-password-confirmation", context);

            // Configuration du message
            helper.setTo(to);
            helper.setSubject("Votre mot de passe a été réinitialisé");
            helper.setText(html, true);
            helper.setFrom(emailFrom, emailFromName);

            mailSender.send(message);
            logger.info("Email de confirmation de réinitialisation envoyé à: {}", to);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de confirmation: {}", e.getMessage(), e);
            // Ne pas lever d'exception pour ne pas bloquer le processus si l'email de confirmation échoue
        }
    }
}