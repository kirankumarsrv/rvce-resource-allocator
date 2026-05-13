package com.rvce.scas.dto.response;

import lombok.Data;

/**
 * Soft validation warning for a bench placement.
 */
@Data
public class SeatWarningDto {

    private String type;
    private String message;
    private String benchNumber;
    private String hallId;
    private String detail;
}