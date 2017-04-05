package com.bluetooth.le.utils;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by caoxuanphong on    4/28/16.
 */
public class ByteUtils {

    public static byte[] stringToByteArray(String string) {
        return string.getBytes();
    }

    public static byte[] integerToByteArray(int number) {
        return ByteBuffer.allocate(4).putInt(number).array();
    }

    public static byte[] longToByteArray(long number) {
        return ByteBuffer.allocate(8).putLong(number).array();
    }

    public static byte[] add2ByteArray(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        return c;
    }

    public static byte[] append(byte[] src, byte[] bytes, int startPos) {
        int j = 0;
        for (int i = startPos; i < startPos + bytes.length; i++) {
            src[i] = bytes[j++];
        }

        return src;
    }

    public static byte[] subByteArray(byte[] src, int startPos, int num) {
        int endPos;

        if (startPos + num > src.length) {
            endPos = src.length;
        } else {
            endPos = startPos + num;
        }

        return Arrays.copyOfRange(src, startPos, endPos );
    }

    // the first byte the most significant
    public static long toLongFirstMostSignificant(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value = (value << 8) + (bytes[i] & 0xff);
        }

        return value;
    }

    // the first byte the least significant
    public static long toLongFirstLeastSignificant(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value += ((long) bytes[i] & 0xffL) << (8 * i);
        }

        return value;
    }

    public static String[] toHex(byte[] a) {
        String[] s = new String[a.length];
        for (int  i =0; i < a.length; i++) {
            s[i] = "0x" + Integer.toHexString((a[i] & 0xff));
        }

        return s;
    }

    public static boolean compare2Array(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) return false;

        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) return false;
        }

        return true;
    }
}
