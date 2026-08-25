package com.rockranger.analyzer.authentication.controller;

import com.rockranger.analyzer.authentication.dto.request.LoginRequest;
import com.rockranger.analyzer.authentication.dto.request.RegisterRequest;
import com.rockranger.analyzer.authentication.dto.request.RequestOtpRequest;
import com.rockranger.analyzer.authentication.dto.request.VerifyOtpRequest;
import com.rockranger.analyzer.authentication.dto.response.LoginResponse;
import com.rockranger.analyzer.authentication.dto.response.RegisterResponse;
import com.rockranger.analyzer.authentication.dto.response.VerifyOtpResponse;
import com.rockranger.analyzer.authentication.service.AuthenticationService;
import com.rockranger.analyzer.authentication.util.CookieUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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

        ResponseCookie cookie = cookieUtils.createJwtCookie(result.getAccessToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        LoginResponse result = authenticationService.login(loginRequest);

        ResponseCookie cookie = cookieUtils.createJwtCookie(result.getAccessToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        ResponseCookie cookie = cookieUtils.createCleanJwtCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of(
                        "status", "success",
                        "message", "Logged out successfully."
                ));
    }
}