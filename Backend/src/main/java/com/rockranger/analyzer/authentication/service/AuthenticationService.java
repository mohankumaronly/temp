package com.rockranger.analyzer.authentication.service;

import com.rockranger.analyzer.authentication.dto.request.LoginRequest;
import com.rockranger.analyzer.authentication.dto.request.RegisterRequest;
import com.rockranger.analyzer.authentication.dto.request.VerifyOtpRequest;
import com.rockranger.analyzer.authentication.dto.response.LoginResponse;
import com.rockranger.analyzer.authentication.dto.response.RegisterResponse;
import com.rockranger.analyzer.authentication.dto.response.VerifyOtpResponse;

public interface AuthenticationService {
    RegisterResponse register(RegisterRequest registerRequest);
    RegisterResponse requestOtp(String email);
    VerifyOtpResponse verifyOtp(VerifyOtpRequest verifyOtpRequest);
    LoginResponse login(LoginRequest loginRequest);
}
