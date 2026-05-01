package com.rvce.scas.exception;

/**
 * Exception thrown when a user attempts to claim or book an exam seat/slot that is
 * already allocated to another user.
 *
 * <p><strong>When it occurs:</strong> During exam seating allocation, if a student
 * attempts to claim a seat that another student has already reserved or a concurrent
 * request claims the same seat.</p>
 *
 * <p><strong>Purpose:</strong> Prevents double-booking of exam seats and ensures
 * each slot is assigned to at most one student.</p>
 *
 * <p><strong>Concurrency:</strong> May occur due to race conditions if two users
 * submit allocation requests simultaneously. The application should handle this gracefully
 * and suggest the user choose a different available slot.</p>
 *
 * <p><strong>Client Response:</strong> Returns HTTP 409 (Conflict) with a message
 * suggesting the user select a different available slot.</p>
 *
 * <p><strong>Example:</strong></p>
 * <pre>
 *   throw new SlotAlreadyClaimedException("This exam seat is already allocated to another student.");
 * </pre>
 *
 * @author RVCE SCAS Team
 * @see ExamController
 */
public class SlotAlreadyClaimedException extends RuntimeException {
    /**
     * Constructs a SlotAlreadyClaimedException with a detailed message.
     *
     * @param message human-readable message explaining which slot is already claimed
     */
    public SlotAlreadyClaimedException(String message) {
        super(message);
    }
}
