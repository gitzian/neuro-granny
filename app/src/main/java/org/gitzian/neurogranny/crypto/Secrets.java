package org.gitzian.neurogranny.crypto;

import org.gitzian.neurogranny.model.Memory;
import org.gitzian.neurogranny.model.SecretMode;

/** Single secret boundary; never log inputs or store secrets in review events. */
public final class Secrets {
    private Secrets() {
    }

    public static boolean verify(Memory m, char[] answer) {
        if (m.mode == SecretMode.RECOVERABLE) {
            char[] expected = m.plain.toCharArray();
            int diff = expected.length ^ answer.length;
            for (int i = 0; i < Math.min(expected.length, answer.length); i++) diff |= expected[i] ^ answer[i];
            java.util.Arrays.fill(expected, '\0');
            return diff == 0;
        }
        return Argon2Verifier.verify(answer, m.verifier);
    }

    public static void setSecret(Memory m, char[] value, SecretMode mode) {
        if (value.length == 0) throw new IllegalArgumentException("Secret cannot be empty");
        m.mode = mode;
        if (mode == SecretMode.VERIFY_ONLY) {
            m.verifier = Argon2Verifier.hash(value);
            m.plain = "";
        } else {
            m.plain = new String(value);
            m.verifier = "";
        }
    }
}
