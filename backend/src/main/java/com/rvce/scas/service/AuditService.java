package com.rvce.scas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class AuditService {

    public void logLogin(UUID userId, String email, boolean success, String reason) {
        log.info("AUDIT login userId={} email={} success={} reason={}", userId, email, success, reason);
    }

    public void logLogout(UUID userId) {
        log.info("AUDIT logout userId={}", userId);
    }
}
