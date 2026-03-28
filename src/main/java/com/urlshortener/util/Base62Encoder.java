package com.urlshortener.util;

import java.security.SecureRandom;

public class Base62Encoder {

    private static final String BASE62_CHARS = 
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String encode(long num) {
        if (num == 0) {
            return "0";
        }

        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(BASE62_CHARS.charAt((int) (num % BASE)));
            num /= BASE;
        }
        return sb.reverse().toString();
    }

    public static long decode(String str) {
        long num = 0;
        int len = str.length();
        for (int i = 0; i < len; i++) {
            num = num * BASE + BASE62_CHARS.indexOf(str.charAt(i));
        }
        return num;
    }

    public static String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_CHARS.charAt(RANDOM.nextInt(BASE)));
        }
        return sb.toString();
    }

    public static String generateShortCode() {
        long timestamp = System.currentTimeMillis() & 0xFFFFFFFFL;
        long random = RANDOM.nextLong() & 0x3FFFFFFFL;
        long combined = (timestamp << 26) | random;
        return encode(combined);
    }

    public static String generateShortCodeWithCounter(long counter) {
        return encode(counter);
    }

    public static boolean isValidBase62(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        return code.chars().allMatch(c -> BASE62_CHARS.indexOf(c) >= 0);
    }
}