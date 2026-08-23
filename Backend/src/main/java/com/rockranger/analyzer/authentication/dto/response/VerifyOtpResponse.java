package com.rockranger.analyzer.authentication.dto.response;

public class VerifyOtpResponse {

    private String accessToken;
    private UserResponse user;

    public VerifyOtpResponse() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}
