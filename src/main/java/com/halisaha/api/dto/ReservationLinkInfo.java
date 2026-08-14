package com.halisaha.api.dto;

public record ReservationLinkInfo(
        String type,
        String status,
        String summary
) {
    public static final ReservationLinkInfo NONE = new ReservationLinkInfo("NONE", null, null);
}
