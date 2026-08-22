package com.rockranger.analyzer.authentication.repository;

import com.rockranger.analyzer.authentication.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {}
