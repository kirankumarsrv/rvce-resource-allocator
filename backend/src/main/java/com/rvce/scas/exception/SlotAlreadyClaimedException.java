package com.rvce.scas.exception;

public class SlotAlreadyClaimedException extends RuntimeException {
    public SlotAlreadyClaimedException(String message) {
        super(message);
    }
}
