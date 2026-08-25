package com.rockranger.analyzer.authentication.service.impl;

import com.rockranger.analyzer.authentication.dto.request.LoginRequest;
import com.rockranger.analyzer.authentication.dto.request.RegisterRequest;
import com.rockranger.analyzer.authentication.dto.request.VerifyOtpRequest;
import com.rockranger.analyzer.authentication.dto.response.LoginResponse;
import com.rockranger.analyzer.authentication.dto.response.RegisterResponse;
import com.rockranger.analyzer.authentication.dto.response.VerifyOtpResponse;
import com.rockranger.analyzer.authentication.entity.EmailVerificationOtp;
import com.rockranger.analyzer.authentication.entity.User;
import com.rockranger.analyzer.authentication.repository.EmailVerificationOtpRepository;
import com.rockranger.analyzer.authentication.repository.UserRepository;
import com.rockranger.analyzer.authentication.service.AuthenticationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private EmailVerificationOtpRepository emailVerificationOtpRepository;

    public AuthenticationServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailVerificationOtpRepository emailVerificationOtpRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationOtpRepository = emailVerificationOtpRepository;
    }

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            RegisterResponse response = new RegisterResponse();
            response.setMessage("Email is already registered.");
            return response;
        }

        User user = new User();

        user.setFullName(registerRequest.getFullName());
        user.setEmail(registerRequest.getEmail());

        user.setPasswordHash(
                passwordEncoder.encode(registerRequest.getPassword())
        );

        userRepository.save(user);

        String otp = String.valueOf(
                ThreadLocalRandom.current().nextInt(100000, 1000000)
        );

        System.out.println("OTP for " + user.getEmail() + ": " + otp);

        EmailVerificationOtp otpEntity = new EmailVerificationOtp();

        otpEntity.setUser(user);
        otpEntity.setOtpHash(passwordEncoder.encode(otp));
        otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        emailVerificationOtpRepository.save(otpEntity);

        RegisterResponse response = new RegisterResponse();
        response.setMessage("OTP has been sent to your email.");

        return response;
    }

    @Override
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest verifyOtpRequest) {
        return null;
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        return null;
    }
}
