package com.rockranger.analyzer.authentication.controller;

import com.rockranger.analyzer.authentication.dto.request.RegisterRequest;
import com.rockranger.analyzer.authentication.dto.response.RegisterResponse;
import com.rockranger.analyzer.authentication.service.AuthenticationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping("/health")
    public String healthMonitoring() {
        return "Server is up and running";
    }

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest registerRequest) {
        return authenticationService.register(registerRequest);
    }
}