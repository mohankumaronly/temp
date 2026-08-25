package com.rockranger.analyzer.authentication.service.impl;

import com.rockranger.analyzer.authentication.dto.request.*;
import com.rockranger.analyzer.authentication.dto.response.*;
import com.rockranger.analyzer.authentication.entity.*;
import com.rockranger.analyzer.authentication.exception.*;
import com.rockranger.analyzer.authentication.repository.*;
import com.rockranger.analyzer.authentication.security.JwtService;
import com.rockranger.analyzer.authentication.service.AuthenticationService;
import com.rockranger.analyzer.authentication.service.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationOtpRepository emailVerificationOtpRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final JwtService jwtService;

    public AuthenticationServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationOtpRepository emailVerificationOtpRepository,
            PasswordResetOtpRepository passwordResetOtpRepository,
            RefreshTokenRepository refreshTokenRepository,
            EmailService emailService,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationOtpRepository = emailVerificationOtpRepository;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.refreshTokenRepository = refreshTokenRepository;
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

        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = createAndSaveRefreshToken(user);

        VerifyOtpResponse response = new VerifyOtpResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
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

        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = createAndSaveRefreshToken(user);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUser(mapToUserResponse(user));
        return response;
    }

    @Override
    @Transactional
    public RegisterResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));

        Optional<PasswordResetOtp> latestResetOpt = passwordResetOtpRepository.findTopByUserOrderByCreatedAtDesc(user);
        if (latestResetOpt.isPresent()) {
            PasswordResetOtp latestReset = latestResetOpt.get();
            if (latestReset.getCreatedAt() != null && latestReset.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
                throw new OtpRateLimitException("Please wait 60 seconds before requesting a new password reset code.");
            }
        }

        // Invalidate older active reset OTPs for this user
        List<PasswordResetOtp> oldOtps = passwordResetOtpRepository.findByUserAndVerifiedFalse(user);
        for (PasswordResetOtp oldOtp : oldOtps) {
            oldOtp.setVerified(true);
        }
        if (!oldOtps.isEmpty()) {
            passwordResetOtpRepository.saveAll(oldOtps);
        }

        String otpCode = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));

        PasswordResetOtp resetEntity = new PasswordResetOtp();
        resetEntity.setUser(user);
        resetEntity.setOtpHash(passwordEncoder.encode(otpCode));
        resetEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        passwordResetOtpRepository.save(resetEntity);

        emailService.sendPasswordResetEmail(user.getEmail(), otpCode);

        RegisterResponse response = new RegisterResponse();
        response.setMessage("Password reset code sent successfully to your email.");
        return response;
    }

    @Override
    @Transactional
    public RegisterResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));

        PasswordResetOtp resetEntity = passwordResetOtpRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new InvalidOtpException("No password reset request found for this email."));

        if (resetEntity.isVerified()) {
            throw new InvalidOtpException("This password reset OTP has already been used.");
        }

        if (LocalDateTime.now().isAfter(resetEntity.getExpiresAt())) {
            throw new OtpExpiredException("Password reset OTP has expired. Please request a new code.");
        }

        if (resetEntity.getAttemptCount() >= 5) {
            throw new InvalidOtpException("Maximum OTP verification attempts exceeded. Please request a new code.");
        }

        if (!passwordEncoder.matches(request.getOtp(), resetEntity.getOtpHash())) {
            resetEntity.setAttemptCount(resetEntity.getAttemptCount() + 1);
            passwordResetOtpRepository.save(resetEntity);
            throw new InvalidOtpException("Invalid OTP code.");
        }

        resetEntity.setVerified(true);
        passwordResetOtpRepository.save(resetEntity);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all existing refresh tokens for security upon password reset
        revokeUserRefreshTokens(user);

        RegisterResponse response = new RegisterResponse();
        response.setMessage("Password has been reset successfully. Please login with your new password.");
        return response;
    }

    @Override
    @Transactional
    public VerifyOtpResponse refreshToken(RefreshTokenRequest request, String refreshTokenFromCookie) {
        String rawToken = (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank())
                ? request.getRefreshToken()
                : refreshTokenFromCookie;

        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidCredentialsException("Refresh token is required.");
        }

        List<RefreshToken> activeTokens = refreshTokenRepository.findAll().stream()
                .filter(t -> !t.isRevoked() && LocalDateTime.now().isBefore(t.getExpiresAt()))
                .filter(t -> passwordEncoder.matches(rawToken, t.getTokenHash()))
                .toList();

        if (activeTokens.isEmpty()) {
            throw new InvalidCredentialsException("Invalid or expired refresh token. Please login again.");
        }

        RefreshToken activeToken = activeTokens.get(0);
        User user = activeToken.getUser();

        String newAccessToken = jwtService.generateToken(user.getEmail());
        String newRefreshToken = createAndSaveRefreshToken(user);

        activeToken.setLastUsedAt(LocalDateTime.now());
        refreshTokenRepository.save(activeToken);

        VerifyOtpResponse response = new VerifyOtpResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        response.setUser(mapToUserResponse(user));
        return response;
    }

    @Override
    @Transactional
    public void logoutUser(User user) {
        if (user != null) {
            revokeUserRefreshTokens(user);
        }
    }

    private void generateAndSendOtp(User user) {
        Optional<EmailVerificationOtp> latestOtpOpt = emailVerificationOtpRepository.findTopByUserOrderByCreatedAtDesc(user);
        if (latestOtpOpt.isPresent()) {
            EmailVerificationOtp latestOtp = latestOtpOpt.get();
            if (latestOtp.getCreatedAt() != null && latestOtp.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
                throw new OtpRateLimitException("Please wait 60 seconds before requesting a new verification code.");
            }
        }

        List<EmailVerificationOtp> oldOtps = emailVerificationOtpRepository.findByUserAndVerifiedFalse(user);
        for (EmailVerificationOtp oldOtp : oldOtps) {
            oldOtp.setVerified(true);
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

    private String createAndSaveRefreshToken(User user) {
        revokeUserRefreshTokens(user);

        String rawToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(passwordEncoder.encode(rawToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private void revokeUserRefreshTokens(User user) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
        for (RefreshToken t : activeTokens) {
            t.setRevoked(true);
        }
        if (!activeTokens.isEmpty()) {
            refreshTokenRepository.saveAll(activeTokens);
        }
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
