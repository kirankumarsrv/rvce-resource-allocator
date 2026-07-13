package com.rvce.scas.repository;

import com.rvce.scas.entity.TimetableVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface TimetableVersionRepository extends JpaRepository<TimetableVersion, UUID> {

    @Modifying
    @Query("UPDATE TimetableVersion v SET v.status = 'ARCHIVED', v.archivedAt = :now " +
           "WHERE v.academicYear = :academicYear AND v.semester = :semester AND v.status = 'ACTIVE'")
    int archiveActiveVersion(@Param("academicYear") String academicYear,
                              @Param("semester") Integer semester,
                              @Param("now") LocalDateTime now);
}