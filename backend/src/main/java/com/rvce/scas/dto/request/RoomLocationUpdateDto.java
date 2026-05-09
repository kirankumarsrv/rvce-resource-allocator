package com.rvce.scas.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * <h3>Purpose</h3>
 * Request DTO for updating room location data via the admin endpoint.
 * Used by PUT /api/admin/rooms/{id}/location (T-301).
 *
 * <h3>Validation Notes</h3>
 * <ul>
 *   <li>Latitude must be between -90 and 90 degrees (valid geographic range)</li>
 *   <li>Longitude must be between -180 and 180 degrees</li>
 *   <li>NUMERIC(10,7) precision ensures ~1cm GPS accuracy</li>
 *   <li>landmark_description is optional (VARCHAR(255) recommended, but stored as TEXT for flexibility)</li>
 *   <li>floor_plan_s3_key is the S3 object key, not a full URL (e.g., "floor-plans/{roomId}/{timestamp}.png")</li>
 * </ul>
 *
 * <h3>API Contract</h3>
 * All fields are optional — partial updates are allowed. If a field is null, it is not updated.
 * Example:
 * <pre>
 * {
 *   "latitude": 12.9238437,
 *   "longitude": 77.4988752,
 *   "landmarkDescription": "Blue building next to basketball court",
 *   "floorPlanS3Key": "floor-plans/ab201/2025-05-07-1200.png"
 * }
 * </pre>
 *
 * @author SCAS Engineering Team
 * @since 2.0 (Epic 2)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomLocationUpdateDto {

    @DecimalMin("-90")
    @DecimalMax("90")
    @Digits(integer = 3, fraction = 7)
    private BigDecimal latitude;

    @DecimalMin("-180")
    @DecimalMax("180")
    @Digits(integer = 3, fraction = 7)
    private BigDecimal longitude;

    @Size(max = 500)
    private String landmarkDescription;

    @Size(max = 255)
    private String floorPlanS3Key;

}
