package com.slmapp.backend.magi.events;

import com.slmapp.backend.entity.MagiUnit;

import java.util.UUID;

public record MagiUnitResultEvent(UUID messageId, MagiUnit unit, int round, String answer, MagiUnit votedFor) {
}
