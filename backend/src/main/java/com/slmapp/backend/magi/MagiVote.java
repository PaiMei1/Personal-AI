package com.slmapp.backend.magi;

import com.slmapp.backend.entity.MagiUnit;

public record MagiVote(MagiUnit voter, MagiUnit votedFor, double confidence, String justification) {
}