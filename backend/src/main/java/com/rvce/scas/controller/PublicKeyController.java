package com.rvce.scas.controller;

import com.rvce.scas.dto.JwksResponse;
import com.rvce.scas.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/public-keys")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PublicKeyController {

    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping(value = "/jwt.pub", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getJwtPublicKey() {
        try {
            return ResponseEntity.ok(jwtTokenProvider.getPublicKeyPem());
        } catch (Exception e) {
            log.error("Unable to fetch JWT public key", e);
            return ResponseEntity.status(500).body("Unable to retrieve public key");
        }
    }

    @GetMapping(value = "/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JwksResponse> getJwks() {
        try {
            return ResponseEntity.ok(jwtTokenProvider.getJwks());
        } catch (Exception e) {
            log.error("Unable to fetch JWKS", e);
            return ResponseEntity.status(500).build();
        }
    }
}
