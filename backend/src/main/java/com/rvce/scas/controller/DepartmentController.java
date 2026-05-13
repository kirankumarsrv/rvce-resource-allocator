package com.rvce.scas.controller;

import com.rvce.scas.dto.response.SimpleDto;
import com.rvce.scas.entity.Department;
import com.rvce.scas.repository.DepartmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    @GetMapping
    @Operation(summary = "Search departments by name prefix", description = "Autocomplete search for departments")
    public ResponseEntity<List<SimpleDto>> search(@RequestParam(name = "q", required = false) String q) {
        String term = q == null ? "" : q.trim();
        var page = departmentRepository.findByNameContainingIgnoreCase(term, PageRequest.of(0, 20));
        List<Department> results = page.getContent();

        List<SimpleDto> dto = results.stream()
                .map(d -> new SimpleDto(d.getDepartmentId().toString(), d.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }
}
