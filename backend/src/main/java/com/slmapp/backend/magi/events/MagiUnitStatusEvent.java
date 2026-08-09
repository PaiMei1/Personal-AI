package com.slmapp.backend.magi.events;

import com.slmapp.backend.entity.MagiUnit;

import java.util.UUID;

public record MagiUnitStatusEvent(UUID messageId, MagiUnit unit, String phase, int round) {
}
