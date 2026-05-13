package com.rvce.scas.repository;

import com.rvce.scas.entity.ExamHall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for exam hall configuration snapshots.
 */
@Repository
public interface ExamHallRepository extends JpaRepository<ExamHall, UUID> {

    List<ExamHall> findByExamSession_ExamIdOrderBySortOrderAsc(UUID examId);

    Optional<ExamHall> findByHallIdAndExamSession_ExamId(UUID hallId, UUID examId);

    boolean existsByExamSession_ExamIdAndRoom_Id(UUID examId, UUID roomId);

    boolean existsByExamSession_ExamIdAndInvigilator_UserId(UUID examId, UUID invigilatorId);

    List<ExamHall> findByInvigilator_UserId(UUID invigilatorId);

    long countByExamSession_ExamId(UUID examId);
}