package com.skillbridge.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromEmail);
            helper.setSubject("SkillBridge — Reset Your Password");
            helper.setText(
                    "Hello,\n\n" +
                            "You requested a password reset for your SkillBridge account.\n\n" +
                            "Click the link below to reset your password " +
                            "(this link expires in 30 minutes):\n\n" +
                            baseUrl + "/reset-password.html?token=" + resetToken + "\n\n" +
                            "If you did not request this, you can safely ignore this email.\n\n" +
                            "— The SkillBridge Team",
                    false
            );
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromEmail);
            helper.setSubject("Welcome to SkillBridge!");
            helper.setText(
                    "Hi " + name + ",\n\n" +
                            "Welcome to SkillBridge — the AI-powered marketplace " +
                            "connecting talent with opportunity.\n\n" +
                            "Get started:\n" +
                            "  • Complete your profile\n" +
                            "  • Browse open jobs\n" +
                            "  • Let our AI match you with the best opportunities\n\n" +
                            baseUrl + "\n\n" +
                            "— The SkillBridge Team",
                    false
            );
            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }
}