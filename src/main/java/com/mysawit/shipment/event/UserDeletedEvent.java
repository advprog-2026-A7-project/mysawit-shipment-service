package com.mysawit.shipment.event;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDeletedEvent {
    private String userId;
    private String role;
    private String previousMandorId;
    private Instant occurredAt;
}
