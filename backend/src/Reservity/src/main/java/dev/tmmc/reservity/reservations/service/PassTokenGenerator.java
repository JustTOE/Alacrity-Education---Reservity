package dev.tmmc.reservity.reservations.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates the QR payload — 256 random bits, hex-encoded to 64 lowercase
 * characters. Unguessable; the door scanner POSTs this verbatim to
 * {@code /api/reservations/checkin?token=...} (M10).
 */
@Component
public class PassTokenGenerator {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int BYTES = 32;          // 256 bits
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public String generate() {
        byte[] buf = new byte[BYTES];
        RNG.nextBytes(buf);
        char[] out = new char[BYTES * 2];
        for (int i = 0; i < BYTES; i++) {
            int b = buf[i] & 0xff;
            out[i * 2]     = HEX[b >>> 4];
            out[i * 2 + 1] = HEX[b & 0x0f];
        }
        return new String(out);
    }
}
