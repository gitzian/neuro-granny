package org.gitzian.neurogranny.crypto;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.util.encoders.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Creates and verifies individually salted Argon2id password verifiers.
 *
 * <p>Salts defeat shared precomputation. They do not prevent offline guessing
 * of weak passwords by an attacker who has copied the database.
 */
public final class Argon2Verifier {
    public static final int MEMORY_KIB = 19_456;
    public static final int ITERATIONS = 2;
    public static final int LANES = 1;

    private static final int SALT_SIZE_BYTES = 16;
    private static final int HASH_SIZE_BYTES = 32;
    private static final int MIN_MEMORY_KIB = 8_192;
    private static final int MAX_MEMORY_KIB = 65_536;
    private static final int MAX_ITERATIONS = 10;
    private static final int MAX_LANES = 4;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Argon2Verifier() {
    }

    public static String hash(char[] password) {
        if (password.length == 0) {
            throw new IllegalArgumentException("Empty password");
        }

        byte[] salt = new byte[SALT_SIZE_BYTES];
        RANDOM.nextBytes(salt);

        byte[] verifier = derive(password, salt, MEMORY_KIB, ITERATIONS, LANES);
        String encoded = "$argon2id$v=19$m=" + MEMORY_KIB
                + ",t=" + ITERATIONS
                + ",p=" + LANES
                + "$" + Base64.toBase64String(salt)
                + "$" + Base64.toBase64String(verifier);

        Arrays.fill(verifier, (byte) 0);
        return encoded;
    }

    public static boolean verify(char[] password, String encoded) {
        try {
            String[] fields = encoded.split("\\$");
            if (fields.length != 6
                    || !"argon2id".equals(fields[1])
                    || !"v=19".equals(fields[2])) {
                return false;
            }

            String[] parameters = fields[3].split(",");
            if (parameters.length != 3) {
                return false;
            }

            int memoryKiB = Integer.parseInt(parameters[0].substring(2));
            int iterations = Integer.parseInt(parameters[1].substring(2));
            int lanes = Integer.parseInt(parameters[2].substring(2));

            // The encoded verifier is untrusted input. Bound resources before hashing.
            if (memoryKiB < MIN_MEMORY_KIB || memoryKiB > MAX_MEMORY_KIB
                    || iterations < 1 || iterations > MAX_ITERATIONS
                    || lanes < 1 || lanes > MAX_LANES) {
                return false;
            }

            byte[] salt = Base64.decode(fields[4]);
            byte[] expected = Base64.decode(fields[5]);
            if (salt.length != SALT_SIZE_BYTES || expected.length != HASH_SIZE_BYTES) {
                return false;
            }

            byte[] actual = derive(password, salt, memoryKiB, iterations, lanes);
            boolean matches = MessageDigest.isEqual(expected, actual);
            Arrays.fill(actual, (byte) 0);
            return matches;
        } catch (RuntimeException invalidVerifier) {
            return false;
        }
    }

    private static byte[] derive(
            char[] password, byte[] salt, int memoryKiB, int iterations, int lanes) {
        Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(salt)
                .withMemoryAsKB(memoryKiB)
                .withIterations(iterations)
                .withParallelism(lanes)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(parameters);

        byte[] verifier = new byte[HASH_SIZE_BYTES];
        generator.generateBytes(password, verifier);
        return verifier;
    }
}
