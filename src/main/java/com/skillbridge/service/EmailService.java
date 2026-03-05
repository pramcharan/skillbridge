package com.skillbridge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("SkillBridge — Reset Your Password");
            message.setText(
                    "Hello,\n\n" +
                            "You requested a password reset for your SkillBridge account.\n\n" +
                            "Click the link below to reset your password " +
                            "(this link expires in 1 hour):\n\n" +
                            "http://localhost:8080/reset-password.html?token=" + resetToken + "\n\n" +
                            "If you did not request this, ignore this email.\n\n" +
                            "— The SkillBridge Team"
            );
            message.setFrom("noreply@skillbridge.com");
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Welcome to SkillBridge!");
            message.setText(
                    "Hi " + name + ",\n\n" +
                            "Welcome to SkillBridge — the AI-powered marketplace " +
                            "connecting talent with opportunity.\n\n" +
                            "Get started:\n" +
                            "• Complete your profile\n" +
                            "• Browse open jobs\n" +
                            "• Let our AI match you with the best opportunities\n\n" +
                            "http://localhost:8080\n\n" +
                            "— The SkillBridge Team"
            );
            message.setFrom("noreply@skillbridge.com");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }
}