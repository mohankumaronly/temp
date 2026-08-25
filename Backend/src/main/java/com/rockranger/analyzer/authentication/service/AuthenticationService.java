package com.rockranger.analyzer.authentication.service;

import com.rockranger.analyzer.authentication.dto.request.*;
import com.rockranger.analyzer.authentication.dto.response.*;
import com.rockranger.analyzer.authentication.entity.User;

public interface AuthenticationService {
    RegisterResponse register(RegisterRequest registerRequest);
    RegisterResponse requestOtp(String email);
    VerifyOtpResponse verifyOtp(VerifyOtpRequest verifyOtpRequest);
    LoginResponse login(LoginRequest loginRequest);
    RegisterResponse forgotPassword(ForgotPasswordRequest request);
    RegisterResponse resetPassword(ResetPasswordRequest request);
    VerifyOtpResponse refreshToken(RefreshTokenRequest request, String refreshTokenFromCookie);
    void logoutUser(User user);
}
