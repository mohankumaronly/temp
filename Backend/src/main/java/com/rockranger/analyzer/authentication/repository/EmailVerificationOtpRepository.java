package com.rockranger.analyzer.authentication.repository;

import com.rockranger.analyzer.authentication.entity.EmailVerificationOtp;
import com.rockranger.analyzer.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, Long> {
    Optional<EmailVerificationOtp> findTopByUserOrderByCreatedAtDesc(User user);
}
