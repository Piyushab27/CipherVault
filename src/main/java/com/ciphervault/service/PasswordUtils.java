package com.ciphervault.service;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Small extra (not in the syllabus): SHA-256 for the master password, Base64 for saved passwords.
 * Base64 only keeps them from sitting in the table as plain text; it is encoding, not strong encryption.
 */
public final class PasswordUtils {
    public static final String MASK = "•".repeat(6);

    private PasswordUtils() {
    }

    public static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available on the JVM", e);
        }
    }

    public static String sha256Hex(String text) {
        return sha256Hex(text.getBytes(StandardCharsets.UTF_8));
    }

    /** Hash of "username:password". The username acts as a salt so equal passwords give different hashes. */
    public static String hashMaster(String username, char[] password) {
        ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
        byte[] pw = new byte[encoded.remaining()];
        encoded.get(pw);
        byte[] prefix = (username.toLowerCase() + ":").getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[prefix.length + pw.length];
        System.arraycopy(prefix, 0, all, 0, prefix.length);
        System.arraycopy(pw, 0, all, prefix.length, pw.length);
        try {
            return sha256Hex(all);
        } finally {
            Arrays.fill(pw, (byte) 0);
            Arrays.fill(all, (byte) 0);
        }
    }

    /** Hash of the security answer: trimmed and lower-cased, so "Tommy " and "tommy" match. */
    public static String hashAnswer(String username, String answer) {
        return sha256Hex(username.toLowerCase() + ":answer:" + answer.trim().toLowerCase());
    }

    public static boolean hashesEqual(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    public static String encode(String plain) {
        return Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String encoded) {
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
