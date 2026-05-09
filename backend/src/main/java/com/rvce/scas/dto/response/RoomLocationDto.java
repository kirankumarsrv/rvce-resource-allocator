package com.rvce.scas.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h3>Purpose</h3>
 * Data transfer object for the public room location endpoint.
 * Returned by GET /api/rooms/{id}/location (T-301).
 *
 * <h3>Security Notes</h3>
 * This DTO exposes only navigation-relevant fields. It does NOT include:
 * <ul>
 *   <li>bench_rows, bench_cols, dept_owner_id (internal fields)</li>
 *   <li>directions_text (unstructured; replaced by room_directions table)</li>
 * </ul>
 * The endpoint is public (permitAll) — students need it on exam day even
 * with expired sessions. No authentication required.
 *
 * <h3>Floor Plan URL</h3>
 * If floor_plan_s3_key is present in the DB, the backend generates a
 * pre-signed S3 URL with TTL=1 hour. This ensures:
 * <ul>
 *   <li>S3 is not publicly readable (signed URL expires)</li>
 *   <li>CloudFront CDN can cache and serve the image efficiently</li>
 *   <li>Security: URL cannot be reused after 1 hour</li>
 * </ul>
 *
 * @author SCAS Engineering Team
 * @since 2.0 (Epic 2)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomLocationDto {

    private UUID id;
    private String name;
    @JsonProperty("floor")
    private Integer floorNumber;
    private String block;
    private String building;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String landmarkDescription;
    private String floorPlanUrl; // Pre-signed S3 URL (NULL if no floor plan available)

}
