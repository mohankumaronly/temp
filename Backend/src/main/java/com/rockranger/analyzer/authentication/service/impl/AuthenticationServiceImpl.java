package com.rockranger.analyzer.authentication.service.impl;

import com.rockranger.analyzer.authentication.dto.request.LoginRequest;
import com.rockranger.analyzer.authentication.dto.request.RegisterRequest;
import com.rockranger.analyzer.authentication.dto.request.VerifyOtpRequest;
import com.rockranger.analyzer.authentication.dto.response.LoginResponse;
import com.rockranger.analyzer.authentication.dto.response.RegisterResponse;
import com.rockranger.analyzer.authentication.dto.response.UserResponse;
import com.rockranger.analyzer.authentication.dto.response.VerifyOtpResponse;
import com.rockranger.analyzer.authentication.entity.EmailVerificationOtp;
import com.rockranger.analyzer.authentication.entity.User;
import com.rockranger.analyzer.authentication.exception.*;
import com.rockranger.analyzer.authentication.repository.EmailVerificationOtpRepository;
import com.rockranger.analyzer.authentication.repository.UserRepository;
import com.rockranger.analyzer.authentication.security.JwtService;
import com.rockranger.analyzer.authentication.service.AuthenticationService;
import com.rockranger.analyzer.authentication.service.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationOtpRepository emailVerificationOtpRepository;
    private final EmailService emailService;
    private final JwtService jwtService;

    public AuthenticationServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationOtpRepository emailVerificationOtpRepository,
            EmailService emailService,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationOtpRepository = emailVerificationOtpRepository;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest) {
        Optional<User> existingUserOpt = userRepository.findByEmail(registerRequest.getEmail());

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (user.isEmailVerified()) {
                throw new EmailAlreadyRegisteredException("Email is already registered.");
            }
            if (registerRequest.getFullName() != null && !registerRequest.getFullName().isBlank()) {
                user.setFullName(registerRequest.getFullName());
            }
            if (registerRequest.getPassword() != null && !registerRequest.getPassword().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
            }
            userRepository.save(user);
        } else {
            user = new User();
            user.setFullName(registerRequest.getFullName());
            user.setEmail(registerRequest.getEmail());
            user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
            user.setEmailVerified(false);
            user = userRepository.save(user);
        }

        generateAndSendOtp(user);

        RegisterResponse response = new RegisterResponse();
        response.setMessage("Verification code sent successfully to your email.");
        return response;
    }

    @Override
    @Transactional
    public RegisterResponse requestOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        if (user.isEmailVerified()) {
            throw new EmailAlreadyVerifiedException("Email is already verified.");
        }

        generateAndSendOtp(user);

        RegisterResponse response = new RegisterResponse();
        response.setMessage("Verification code sent successfully to your email.");
        return response;
    }

    @Override
    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest verifyOtpRequest) {
        User user = userRepository.findByEmail(verifyOtpRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + verifyOtpRequest.getEmail()));

        EmailVerificationOtp otpEntity = emailVerificationOtpRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new InvalidOtpException("No OTP request found for this email."));

        if (otpEntity.isVerified()) {
            throw new InvalidOtpException("This OTP has already been verified.");
        }

        if (LocalDateTime.now().isAfter(otpEntity.getExpiresAt())) {
            throw new OtpExpiredException("OTP code has expired. Please request a new code.");
        }

        if (otpEntity.getAttemptCount() >= 5) {
            throw new InvalidOtpException("Maximum OTP verification attempts exceeded. Please request a new code.");
        }

        if (!passwordEncoder.matches(verifyOtpRequest.getOtp(), otpEntity.getOtpHash())) {
            otpEntity.setAttemptCount(otpEntity.getAttemptCount() + 1);
            emailVerificationOtpRepository.save(otpEntity);
            throw new InvalidOtpException("Invalid OTP code.");
        }

        otpEntity.setVerified(true);
        emailVerificationOtpRepository.save(otpEntity);

        user.setEmailVerified(true);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());

        VerifyOtpResponse response = new VerifyOtpResponse();
        response.setAccessToken(token);
        response.setUser(mapToUserResponse(user));
        return response;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email is not verified. Please verify your email first.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());

        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setUser(mapToUserResponse(user));
        return response;
    }

    private void generateAndSendOtp(User user) {
        Optional<EmailVerificationOtp> latestOtpOpt = emailVerificationOtpRepository.findTopByUserOrderByCreatedAtDesc(user);
        if (latestOtpOpt.isPresent()) {
            EmailVerificationOtp latestOtp = latestOtpOpt.get();
            if (latestOtp.getCreatedAt() != null && latestOtp.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
                throw new OtpRateLimitException("Please wait 60 seconds before requesting a new verification code.");
            }
        }

        // Invalidate older active OTP records for this user
        List<EmailVerificationOtp> oldOtps = emailVerificationOtpRepository.findByUserAndVerifiedFalse(user);
        for (EmailVerificationOtp oldOtp : oldOtps) {
            oldOtp.setVerified(true); // Deactivate older OTPs so only newest can be used
        }
        if (!oldOtps.isEmpty()) {
            emailVerificationOtpRepository.saveAll(oldOtps);
        }

        String otpCode = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));

        EmailVerificationOtp otpEntity = new EmailVerificationOtp();
        otpEntity.setUser(user);
        otpEntity.setOtpHash(passwordEncoder.encode(otpCode));
        otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        emailVerificationOtpRepository.save(otpEntity);

        emailService.sendOtpEmail(user.getEmail(), otpCode);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setEmailVerified(user.isEmailVerified());
        return response;
    }
}
