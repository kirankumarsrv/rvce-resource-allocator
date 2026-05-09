package com.rvce.scas.service;

import com.rvce.scas.dto.request.RoomLocationUpdateDto;
import com.rvce.scas.dto.response.RoomLocationDto;
import com.rvce.scas.entity.Room;
import com.rvce.scas.exception.RoomNotFoundException;
import com.rvce.scas.mapper.RoomLocationMapper;
import com.rvce.scas.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * <h3>Purpose</h3>
 * Service for fetching room location data and generating pre-signed S3 URLs
 * for floor plan images. Implements T-301 room location endpoint logic.
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *   <li>Fetch room by ID and return navigation-relevant fields (GPS, building, landmark)</li>
 *   <li>Generate pre-signed S3 URLs for floor plan images with 1-hour TTL</li>
 *   <li>Handle missing rooms gracefully (404) and missing floor plans (null URL)</li>
 * </ul>
 *
 * <h3>S3 URL Generation Strategy</h3>
 * Floor plan images are stored in AWS S3 under the key stored in rooms.floor_plan_s3_key.
 * Rather than returning the S3 key directly, the service generates a pre-signed URL with:
 * <ul>
 *   <li>TTL = 1 hour (configurable via property room.floor-plan.presigned-url.ttl-seconds)</li>
 *   <li>S3 bucket access is restricted to CloudFront via Origin Access Control (OAC)</li>
 *   <li>Pre-signed URL is the only way to access the image — S3 is not publicly readable</li>
 * </ul>
 * This approach:
 * <ul>
 *   <li>Prevents unauthorized S3 access</li>
 *   <li>Ensures the URL expires after the exam (prevents linking to old images)</li>
 *   <li>Allows CloudFront CDN to cache and serve at low latency</li>
 * </ul>
 *
 * <h3>Transaction Behaviour</h3>
 * Read-only; @Transactional(readOnly=true) ensures the transaction is optimized
 * for reads and prevents any accidental writes.
 *
 * <h3>Error Handling</h3>
 * <ul>
 *   <li>Room not found → ResourceNotFoundException (404)</li>
 *   <li>S3Presigner failure → logs warning, returns null for floorPlanUrl (graceful degradation)</li>
 * </ul>
 *
 * @author SCAS Engineering Team
 * @since 2.0 (Epic 2)
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class RoomLocationService {

    private final RoomRepository roomRepository;
    private final RoomLocationMapper roomLocationMapper;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name:scas-rvce-floor-plans}")
    private String s3BucketName;

    @Value("${room.floor-plan.presigned-url.ttl-seconds:3600}")
    private long presignedUrlTtlSeconds;

    /**
     * Fetches the location details and floor plan URL for a room.
     *
     * @param roomId the UUID of the room
     * @return RoomLocationDto with navigation-relevant fields and floor plan URL (if available)
     * @throws RoomNotFoundException if the room does not exist or is inactive
     */
    public RoomLocationDto getLocationById(UUID roomId) {
        Room room = roomRepository.findById(roomId)
            .filter(Room::getIsActive)
            .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        RoomLocationDto dto = roomLocationMapper.toLocationDto(room);

        // Generate pre-signed S3 URL if floor plan exists
        if (room.getFloorPlanS3Key() != null) {
            try {
                String presignedUrl = generatePresignedUrl(room.getFloorPlanS3Key());
                dto.setFloorPlanUrl(presignedUrl);
            } catch (Exception e) {
                log.warn("Failed to generate pre-signed URL for floor plan: {}", room.getFloorPlanS3Key(), e);
                // Graceful degradation: floor plan URL is null, but room data is still returned
                dto.setFloorPlanUrl(null);
            }
        }

        return dto;
    }

    /**
     * Generates a pre-signed S3 URL for a floor plan image with TTL = 1 hour.
     * The URL can only be used for GET requests and expires after the TTL.
     *
     * @param s3Key the S3 object key for the floor plan image
     * @return a pre-signed URL string
     */
    private String generatePresignedUrl(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(s3BucketName)
            .key(s3Key)
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(presignedUrlTtlSeconds))
            .getObjectRequest(getObjectRequest)
            .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * Updates room location data: GPS coordinates, landmark description, and floor plan S3 key.
     * Only SUPER_ADMIN can call this endpoint.
     *
     * @param roomId the room ID
     * @param updateDto the location update request DTO (all fields optional)
     * @return updated RoomLocationDto with current data
     * @throws RoomNotFoundException if the room does not exist or is inactive
     */
    @Transactional
    public RoomLocationDto updateLocation(UUID roomId, RoomLocationUpdateDto updateDto) {
        Room room = roomRepository.findById(roomId)
            .filter(Room::getIsActive)
            .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        // Partial update: only update fields that are provided
        if (updateDto.getLatitude() != null) {
            room.setLatitude(updateDto.getLatitude());
        }
        if (updateDto.getLongitude() != null) {
            room.setLongitude(updateDto.getLongitude());
        }
        if (updateDto.getLandmarkDescription() != null) {
            room.setLandmarkDescription(updateDto.getLandmarkDescription());
        }
        if (updateDto.getFloorPlanS3Key() != null) {
            room.setFloorPlanS3Key(updateDto.getFloorPlanS3Key());
        }

        // Update timestamp (done by @PreUpdate)
        room.setUpdatedAt(Instant.now());

        Room saved = roomRepository.save(room);
        log.info("Room location updated: roomId={}, latitude={}, longitude={}",
            roomId, saved.getLatitude(), saved.getLongitude());

        // Return the updated location with pre-signed URL if applicable
        return getLocationById(roomId);
    }

}
