package com.rvce.scas.repository;

import com.rvce.scas.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Optional<Department> findByNameIgnoreCase(String name);
    Optional<Department> findByCodeIgnoreCase(String code);
    boolean existsByCode(String code);

    Page<Department> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
