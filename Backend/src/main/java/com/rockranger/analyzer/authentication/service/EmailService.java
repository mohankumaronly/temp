package com.rockranger.analyzer.authentication.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otpCode);
    void sendPasswordResetEmail(String toEmail, String otpCode);
}
