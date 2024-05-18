package com.sedmelluq.discord.lavaplayer.container.wav;

import java.util.Arrays;

public enum WaveFormatType {
    // https://www.mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/Docs/Pages%20from%20mmreg.h.pdf
    WAVE_FORMAT_UNKNOWN(0x0000),
    WAVE_FORMAT_PCM(0x0001),
    WAVE_FORMAT_EXTENSIBLE(0xFFFE);

    final int code;

    WaveFormatType(int code) {
        this.code = code;
    }

    static WaveFormatType getByCode(int code) {
        return Arrays.stream(values()).filter(type -> type.code == code).findFirst()
            .orElse(WAVE_FORMAT_UNKNOWN);
    }
}
