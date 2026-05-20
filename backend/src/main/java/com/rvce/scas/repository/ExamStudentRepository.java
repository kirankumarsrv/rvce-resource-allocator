package com.rvce.scas.repository;

import com.rvce.scas.entity.ExamStudent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for exam student enrollment persistence and queries.
 */
@Repository
public interface ExamStudentRepository extends JpaRepository<ExamStudent, UUID> {

    @Query("""
        SELECT es FROM ExamStudent es
        WHERE es.examId = :examId
          AND (:branchCode IS NULL OR UPPER(es.branchCode) = UPPER(:branchCode))
          AND (:usn IS NULL OR UPPER(es.usn) LIKE CONCAT('%', UPPER(:usn), '%'))
        """)
    Page<ExamStudent> searchExamStudents(
            @Param("examId") UUID examId,
            @Param("branchCode") String branchCode,
            @Param("usn") String usn,
            Pageable pageable);

    @Query("SELECT es.usn FROM ExamStudent es WHERE es.examId = :examId")
    List<String> findUsnsByExamId(@Param("examId") UUID examId);

    List<ExamStudent> findByExamIdAndUsnIn(UUID examId, Collection<String> usns);

    List<ExamStudent> findByExamIdOrderByCreatedAtAsc(UUID examId);

    boolean existsByExamIdAndStudentId(UUID examId, UUID studentId);

    @Query("SELECT DISTINCT es.examId FROM ExamStudent es WHERE es.studentId = :studentId")
    List<UUID> findExamIdsByStudentId(@Param("studentId") UUID studentId);

    long countByExamId(UUID examId);

    @Query("SELECT COUNT(es) FROM ExamStudent es WHERE es.examId = :examId AND es.studentId NOT IN (SELECT DISTINCT seat.studentId FROM ExamSeat seat WHERE seat.examSession.examId = :examId)")
    long countUnassignedStudents(@Param("examId") UUID examId);
}
