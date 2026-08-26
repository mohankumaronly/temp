package com.rockranger.analyzer.resume.repository;

import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.entity.ResumeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByUserId(Long userId);

    List<Resume> findByStatus(ResumeStatus status);

    @Query("SELECT r FROM Resume r WHERE r.id = :id AND r.user.email = :email")
    Optional<Resume> findByIdAndUserEmail(@Param("id") Long id, @Param("email") String email);

    /**
     * Updates the status of a resume directly in the database.
     * This avoids loading the entity and triggering cascade operations
     * that would delete associated ResumeParsedData.
     *
     * @param id the resume ID
     * @param status the new status to set
     */
    @Modifying
    @Transactional
    @Query("UPDATE Resume r SET r.status = :status WHERE r.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") ResumeStatus status);
}