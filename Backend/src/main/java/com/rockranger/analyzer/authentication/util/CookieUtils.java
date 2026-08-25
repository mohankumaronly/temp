package com.rockranger.analyzer.authentication.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtils {

    @Value("${app.cookie.secure:false}")
    private boolean secure;

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${app.cookie.domain:}")
    private String domain;

    @Value("${app.cookie.max-age-seconds:86400}")
    private long maxAgeSeconds;

    public ResponseCookie createJwtCookie(String token) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(sameSite);

        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        return builder.build();
    }

    public ResponseCookie createCleanJwtCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite);

        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        return builder.build();
    }
}
