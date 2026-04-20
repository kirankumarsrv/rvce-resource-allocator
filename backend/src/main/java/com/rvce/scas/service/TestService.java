package com.rvce.scas.service;

import com.rvce.scas.dto.TestResponseDto;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    public TestResponseDto getMessage() {
        return new TestResponseDto(
                "Backend is working 🚀",
                "SUCCESS"
        );
    }
}
