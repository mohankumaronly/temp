package com.rockranger.analyzer.resume.repository;

import com.rockranger.analyzer.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

}
