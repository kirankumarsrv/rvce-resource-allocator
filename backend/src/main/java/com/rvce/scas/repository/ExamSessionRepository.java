package com.rvce.scas.repository;

import com.rvce.scas.entity.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for exam session persistence.
 */
@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {
}
