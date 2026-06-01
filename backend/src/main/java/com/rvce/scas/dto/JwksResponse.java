package com.rvce.scas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwksResponse {
    private List<RsaPublicKey> keys;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RsaPublicKey {
        private String kid;
        private String kty;
        private String alg;
        @JsonProperty("use")
        private String keyUse;
        private String n;
        private String e;
    }
}
