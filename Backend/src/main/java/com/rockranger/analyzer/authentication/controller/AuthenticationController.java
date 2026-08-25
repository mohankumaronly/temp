package com.rockranger.analyzer.authentication.controller;

import com.rockranger.analyzer.authentication.dto.request.*;
import com.rockranger.analyzer.authentication.dto.response.*;
import com.rockranger.analyzer.authentication.entity.User;
import com.rockranger.analyzer.authentication.service.AuthenticationService;
import com.rockranger.analyzer.authentication.util.CookieUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final CookieUtils cookieUtils;

    public AuthenticationController(AuthenticationService authenticationService, CookieUtils cookieUtils) {
        this.authenticationService = authenticationService;
        this.cookieUtils = cookieUtils;
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthMonitoring() {
        return ResponseEntity.ok("Server is up and running");
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        RegisterResponse response = authenticationService.register(registerRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/otp/request", "/request-otp"})
    public ResponseEntity<RegisterResponse> requestOtp(
            @Valid @RequestBody RequestOtpRequest request
    ) {
        RegisterResponse response = authenticationService.requestOtp(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/otp/verify", "/verify-otp"})
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest verifyOtpRequest
    ) {
        VerifyOtpResponse result = authenticationService.verifyOtp(verifyOtpRequest);

        ResponseCookie jwtCookie = cookieUtils.createJwtCookie(result.getAccessToken());
        ResponseCookie refreshCookie = cookieUtils.createRefreshTokenCookie(result.getRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        LoginResponse result = authenticationService.login(loginRequest);

        ResponseCookie jwtCookie = cookieUtils.createJwtCookie(result.getAccessToken());
        ResponseCookie refreshCookie = cookieUtils.createRefreshTokenCookie(result.getRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(result);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<RegisterResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        RegisterResponse response = authenticationService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<RegisterResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        RegisterResponse response = authenticationService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<VerifyOtpResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = "refreshToken", required = false) String refreshTokenFromCookie
    ) {
        VerifyOtpResponse result = authenticationService.refreshToken(request, refreshTokenFromCookie);

        ResponseCookie jwtCookie = cookieUtils.createJwtCookie(result.getAccessToken());
        ResponseCookie refreshCookie = cookieUtils.createRefreshTokenCookie(result.getRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal User user) {
        authenticationService.logoutUser(user);

        ResponseCookie jwtCookie = cookieUtils.createCleanJwtCookie();
        ResponseCookie refreshCookie = cookieUtils.createCleanRefreshTokenCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of(
                        "status", "success",
                        "message", "Logged out successfully."
                ));
    }
}