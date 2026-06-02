package com.erainfotech.ums.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record LoginAttemptResponse(
        String eventId,
        OffsetDateTime occurredAt,
        String status,
        String eventType,
        String realmId,
        String clientId,
        String userId,
        String username,
        String ipAddress,
        String error,
        Map<String, String> details
) {
}
