package com.rvce.scas.service;

import com.rvce.scas.dto.request.CreateExamSessionRequest;
import com.rvce.scas.dto.response.ExamSessionDto;
import com.rvce.scas.entity.Department;
import com.rvce.scas.repository.DepartmentRepository;
import com.rvce.scas.repository.ExamSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@Transactional
public class DepartmentIntegrationTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ExamUploadService examUploadService;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Test
    void createExamSessionCreatesDepartmentWhenNameProvided() {
        CreateExamSessionRequest req = new CreateExamSessionRequest();
        req.setName("Integration Test Exam");
        req.setSubjectCode("21CS01");
        req.setSubjectName("Integration Testing");
        req.setSemester(1);
        req.setDepartmentName("Integration Dept X");
        req.setExamDate(LocalDate.now().plusDays(7));
        req.setStartTime(LocalTime.of(9, 0));
        req.setEndTime(LocalTime.of(12, 0));

        UUID actor = UUID.randomUUID();
        ExamSessionDto dto = examUploadService.createExamSession(req, actor);

        assertThat(dto).isNotNull();
        assertThat(dto.getDepartmentId()).isNotNull();

        Department dept = departmentRepository.findById(dto.getDepartmentId()).orElseThrow();
        assertThat(dept.getName()).isEqualToIgnoringCase("Integration Dept X");

        assertThat(examSessionRepository.findById(dto.getExamId())).isPresent();
    }
}
