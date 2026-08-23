package com.rockranger.analyzer.authentication.service.impl;

import com.rockranger.analyzer.authentication.dto.request.LoginRequest;
import com.rockranger.analyzer.authentication.dto.request.RegisterRequest;
import com.rockranger.analyzer.authentication.dto.request.VerifyOtpRequest;
import com.rockranger.analyzer.authentication.dto.response.LoginResponse;
import com.rockranger.analyzer.authentication.dto.response.RegisterResponse;
import com.rockranger.analyzer.authentication.dto.response.VerifyOtpResponse;
import com.rockranger.analyzer.authentication.service.AuthenticationService;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        return null;
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
