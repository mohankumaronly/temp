package com.rockranger.analyzer.authentication.repository;

import com.rockranger.analyzer.authentication.entity.PasswordResetOtp;
import com.rockranger.analyzer.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    Optional<PasswordResetOtp> findTopByUserOrderByCreatedAtDesc(User user);
    List<PasswordResetOtp> findByUserAndVerifiedFalse(User user);
}
