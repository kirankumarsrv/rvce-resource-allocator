package com.rvce.scas.repository;

import com.rvce.scas.entity.ExamSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for manual exam seat assignments.
 */
@Repository
public interface ExamSeatRepository extends JpaRepository<ExamSeat, UUID> {

    List<ExamSeat> findByExamSession_ExamIdOrderByHall_SortOrderAscBenchRowAscBenchColAscBenchSeatIndexAsc(UUID examId);

    List<ExamSeat> findByExamSession_ExamIdAndHall_HallId(UUID examId, UUID hallId);

    List<ExamSeat> findByExamSession_ExamIdAndHall_HallIdOrderByBenchRowAscBenchColAscBenchSeatIndexAsc(UUID examId, UUID hallId);

    List<ExamSeat> findByHall_HallId(UUID hallId);

    Optional<ExamSeat> findByExamSession_ExamIdAndStudentId(UUID examId, UUID studentId);

        @Query("""
                        SELECT es FROM ExamSeat es
                        JOIN FETCH es.examSession exam
                        JOIN FETCH es.hall hall
                        JOIN FETCH hall.room room
                        WHERE es.studentId = :studentId
                            AND exam.status = com.rvce.scas.entity.ExamSession$ExamStatus.PUBLISHED
                        ORDER BY exam.examDate ASC, exam.startTime ASC
                        """)
        List<ExamSeat> findPublishedSeatsByStudentId(@Param("studentId") UUID studentId);

        @Query("""
                        SELECT es FROM ExamSeat es
                        JOIN FETCH es.examSession exam
                        JOIN FETCH es.hall hall
                        JOIN FETCH hall.room room
                        WHERE es.studentId = :studentId
                            AND exam.examId = :examId
                            AND exam.status = com.rvce.scas.entity.ExamSession$ExamStatus.PUBLISHED
                        """)
        Optional<ExamSeat> findPublishedSeatByExamAndStudent(@Param("examId") UUID examId, @Param("studentId") UUID studentId);

    boolean existsByHall_HallIdAndBenchRowAndBenchColAndBenchSeatIndex(UUID hallId, Integer benchRow, Integer benchCol, Integer benchSeatIndex);

    long countByExamSession_ExamId(UUID examId);

    @Modifying
    @Query("DELETE FROM ExamSeat es WHERE es.examSession.examId = :examId")
    void deleteByExamId(@Param("examId") UUID examId);

    @Modifying
    @Query("DELETE FROM ExamSeat es WHERE es.examSession.examId = :examId AND es.hall.hallId = :hallId")
    void deleteByExamIdAndHallId(@Param("examId") UUID examId, @Param("hallId") UUID hallId);
}