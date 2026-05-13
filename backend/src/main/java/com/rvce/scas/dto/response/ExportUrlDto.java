package com.rvce.scas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for export result with presigned URL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportUrlDto {
    private String downloadUrl;
    private Instant expiresAt;
    private String generatedAt;
    private String scope;
    private Integer pageCount;
    private Long fileSizeBytes;
}
