package com.chatapp.util;

import java.util.Base64;

public class Base64Util {
    public static String encode(byte[] data) {
        if (data == null) return null;
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] decode(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        return Base64.getDecoder().decode(base64);
    }
}


