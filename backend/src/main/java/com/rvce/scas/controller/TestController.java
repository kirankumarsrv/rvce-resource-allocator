package com.rvce.scas.controller;

import com.rvce.scas.dto.TestResponseDto;
import com.rvce.scas.service.TestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping
    public TestResponseDto hello() {
        return testService.getMessage();
    }
}
