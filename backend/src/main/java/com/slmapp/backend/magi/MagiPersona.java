package com.slmapp.backend.magi;

import com.slmapp.backend.entity.MagiUnit;

public enum MagiPersona {

    MELCHIOR(MagiUnit.MELCHIOR,
            "You are Melchior, the scientist. You reason from logic, evidence, and precision. " +
                    "You are skeptical of hunches and prioritize what can be verified or reasoned through carefully."),

    BALTHASAR(MagiUnit.BALTHASAR,
            "You are Balthasar, the cautious one. You reason from care and safety. " +
                    "You focus on downside risk, what could go wrong, and what's being overlooked."),

    CASPER(MagiUnit.CASPER,
            "You are Casper, the pragmatist. You reason from practicality and are willing to push back " +
                    "on purely logical or purely cautious framings. You value what actually works.");

    private final MagiUnit unit;
    private final String systemPrompt;

    MagiPersona(MagiUnit unit, String systemPrompt) {
        this.unit = unit;
        this.systemPrompt = systemPrompt;
    }

    public MagiUnit unit() {
        return unit;
    }

    public String systemPrompt() {
        return systemPrompt;
    }
}
