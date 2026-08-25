package com.rockranger.analyzer.authentication.service.impl;

import com.rockranger.analyzer.authentication.exception.EmailSendingException;
import com.rockranger.analyzer.authentication.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${app.email.sender:rockrangerz801@gmail.com}")
    private String senderEmail;

    @Value("${app.email.sender-name:Resume Analyzer}")
    private String senderName;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        sendMail(toEmail, "Your OTP Verification Code - Resume Analyzer", "Verification Code",
                "Your One-Time Password (OTP) for authentication is:", otpCode);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String otpCode) {
        sendMail(toEmail, "Password Reset Code - Resume Analyzer", "Password Reset Code",
                "Your One-Time Password (OTP) to reset your password is:", otpCode);
    }

    private void sendMail(String toEmail, String subject, String headerTitle, String bodyMessage, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, senderName);
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String htmlContent = "<div style=\"font-family: Arial, sans-serif; padding: 20px; max-width: 500px; border: 1px solid #e0e0e0; border-radius: 8px;\">"
                    + "<h2 style=\"color: #333; text-align: center;\">" + headerTitle + "</h2>"
                    + "<p>Hello,</p>"
                    + "<p>" + bodyMessage + "</p>"
                    + "<div style=\"text-align: center; margin: 25px 0;\">"
                    + "<span style=\"font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #4F46E5; background: #EEF2FF; padding: 10px 20px; border-radius: 6px;\">"
                    + otpCode + "</span>"
                    + "</div>"
                    + "<p>This code will expire in <strong>5 minutes</strong>. If you did not request this, please ignore this email.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #eee; margin-top: 20px;\">"
                    + "<p style=\"font-size: 12px; color: #888; text-align: center;\">Resume Analyzer Security Team</p>"
                    + "</div>";

            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Successfully sent email [{}] from <{}> to {}", subject, senderEmail, toEmail);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
            throw new EmailSendingException("Unable to send verification email. Please try again later.");
        }
    }
}
