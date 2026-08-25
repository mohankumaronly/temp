package com.rockranger.analyzer.authentication.repository;

import com.rockranger.analyzer.authentication.entity.RefreshToken;
import com.rockranger.analyzer.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);
    List<RefreshToken> findByUserAndRevokedFalse(User user);
}
