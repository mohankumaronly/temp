package com.rockranger.analyzer.authentication.service.impl;

import com.rockranger.analyzer.authentication.dto.request.LoginRequest;
import com.rockranger.analyzer.authentication.dto.request.RegisterRequest;
import com.rockranger.analyzer.authentication.dto.request.VerifyOtpRequest;
import com.rockranger.analyzer.authentication.dto.response.LoginResponse;
import com.rockranger.analyzer.authentication.dto.response.RegisterResponse;
import com.rockranger.analyzer.authentication.dto.response.VerifyOtpResponse;
import com.rockranger.analyzer.authentication.entity.User;
import com.rockranger.analyzer.authentication.repository.UserRepository;
import com.rockranger.analyzer.authentication.service.AuthenticationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    public AuthenticationServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        RegisterResponse response = new RegisterResponse();
        response.setMessage("Account created successfully.");

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
