package com.rockranger.analyzer.resume.repository;

import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.entity.ResumeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByUserId(Long userId);

    List<Resume> findByStatus(ResumeStatus status);

    @Query("SELECT r FROM Resume r WHERE r.id = :id AND r.user.email = :email")
    Optional<Resume> findByIdAndUserEmail(@Param("id") Long id, @Param("email") String email);
}
