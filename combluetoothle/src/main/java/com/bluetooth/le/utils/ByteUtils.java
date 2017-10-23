package com.bluetooth.le.utils;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static android.R.attr.end;
import static android.R.attr.x;
import static android.R.attr.y;

/**
 * Created by caoxuanphong on    4/28/16.
 */
public class ByteUtils {
    private static final String TAG = "ByteUtils";

    /**
     * Create byte array from list of integer
     * <p>
     * Ex: ByteUtils.createByteArray(1,2,3,4,5,100, 500);
     * Result: [0x1, 0x2, 0x3, 0x4, 0x5, 0x64, 0xf4]
     *
     * @param numbers
     * @return
     */
    public static byte[] createByteArray(int... numbers) {
        byte[] array = new byte[numbers.length];

        for (int i = 0; i < numbers.length; i++) {
            array[i] = (byte) numbers[i];
        }

        return array;
    }

    /**
     * Convert a String into byte array
     *
     * @param string
     * @return
     */
    public static byte[] stringToByteArray(String string) {
        return string.getBytes();
    }

    /**
     * Convert interger into byte array
     *
     * @param number
     * @param order
     * @return
     */
    public static byte[] integerToByteArray(int number, ByteOrder order) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(order);
        return byteBuffer.putInt(number).array();
    }

    /**
     * Convert long into byte array
     *
     * @param number
     * @param order
     * @return
     */
    public static byte[] longToByteArray(long number, ByteOrder order) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.order(order);
        return byteBuffer.putLong(number).array();
    }

    /**
     * Convert short into byte array
     *
     * @param number
     * @param order
     * @return
     */
    public static byte[] shortToByteArray(short number, ByteOrder order) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.order(order);
        return byteBuffer.putShort(number).array();
    }

    /**
     * Convert float into byte array
     *
     * @param number
     * @param order
     * @return
     */
    public static byte[] floatToByteArray(float number, ByteOrder order) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(order);
        return byteBuffer.putFloat(number).array();
    }

    /**
     * Convert double into byte array
     *
     * @param number
     * @param order
     * @return
     */
    public static byte[] doubleToByteArray(double number, ByteOrder order) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.order(order);
        return byteBuffer.putDouble(number).array();
    }

    /**
     * Convert byte array into float
     *
     * @param b
     * @param order
     * @return
     */
    public static float toFloat(byte[] b, ByteOrder order) {
        return ByteBuffer.wrap(b).order(order).getFloat();
    }

    /**
     * Convert byte array into integer
     *
     * @param b
     * @param order
     * @return
     */
    public static int toInteger(byte[] b, ByteOrder order) {
        return ByteBuffer.wrap(b).order(order).getInt();
    }

    /**
     * Convert  byte array into long
     *
     * @param b
     * @param order
     * @return
     */
    public static long toLong(byte[] b, ByteOrder order) {
        return ByteBuffer.wrap(b).order(order).getLong();
    }

    public static double toDouble(byte[] b, ByteOrder order) {
        return ByteBuffer.wrap(b).order(order).getDouble();
    }

    public static short toShort(byte[] b, ByteOrder order) {
        return ByteBuffer.wrap(b).order(order).getShort();
    }

    public static String toString(byte[] b) {
        return new String(b, StandardCharsets.UTF_8);
    }

    public static String toString(byte[] b, Charset charset) {
        return new String(b, charset);
    }

    /**
     * Concatenate 2 byte array into new byte array
     *
     * @param a
     * @param b
     * @return
     */
    public static byte[] concatenate(byte[] a, byte[] b) {
        if (a == null && b == null) return null;
        if (a == null && b != null) return b;
        if (a != null && b == null) return a;

        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        return c;
    }

    /**
     * Add 1 byte into the end of byte array
     *
     * @param a
     * @param b
     * @return
     */
    public static byte[] addByte(byte[] a, byte b) {
        byte[] c;
        if (a == null) {
            return new byte[]{b};
        } else {
            c = new byte[a.length + 1];
        }


        if (a != null) {
            System.arraycopy(a, 0, c, 0, a.length);
            System.arraycopy(new byte[]{b}, 0, c, a.length, 1);
        } else {
            c[0] = b;
        }

        return c;
    }

    /**
     * Merge 2 byte array
     *
     * @param src
     * @param bytes
     * @param startPos
     * @return
     */
    public static byte[] merge(byte[] src, byte[] bytes, int startPos) {
        int j = 0;
        for (int i = startPos; i < startPos + bytes.length; i++) {
            src[i] = bytes[j++];
        }

        return src;
    }

    /**
     * Get sub of array
     *
     * @param src
     * @param startPos
     * @param num
     * @return
     */
    public static byte[] subByteArray(byte[] src, int startPos, int num) {
        if (startPos < 0 || num <= 0) {
            return null;
        }

        int endPos;

        if (startPos + num > src.length) {
            endPos = src.length;
        } else {
            endPos = startPos + num;
        }

        return Arrays.copyOfRange(src, startPos, endPos);
    }


    /**
     * Append array to an array
     *
     * @param src
     * @param bytes
     * @param startPos
     * @return
     */
    public static byte[] append(byte[] src, byte[] bytes, int startPos) {
        int j = 0;
        for (int i = startPos; i < startPos + bytes.length; i++) {
            src[i] = bytes[j++];
        }

        return src;
    }

    /**
     * Convert byte array into string array of hex
     *
     * @param a
     * @return
     */
    public static String toHexString(byte[] a) {
        if (a == null) return null;

        String[] s = new String[a.length];
        for (int i = 0; i < a.length; i++) {
            s[i] = "0x" + Integer.toHexString((a[i] & 0xff));
        }

        return Arrays.toString(s);
    }

    public static String toIntegerString(byte[] a) {
        if (a == null) return null;

        return Arrays.toString(a);
    }

    /**
     * Compare 2 byte array contain same data
     *
     * @param b1
     * @param b2
     * @return
     */
    public static boolean compare2Array(byte[] b1, byte[] b2) {
        if (b1 == null && b2 == null) return true;

        if (b1 == null || b2 == null) return false;

        if (b1 == b2) return true;

        if (b1.length != b2.length) return false;

        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) return false;
        }

        return true;
    }

    /**
     * Check @child byte array is contain in @src byte array
     *
     * @param src
     * @param child
     * @return
     */
    public static boolean isContain(byte[] src, byte[] child) {
        if (src == null && child == null) return true;
        if (src == null || child == null) return false;

        if (src == child) return true;

        if (child.length > src.length) {
            return false;
        } else if (child.length == src.length) {
            for (int i = 0; i < src.length; i++) {
                if (src[i] != child[i]) {
                    return false;
                }
            }
            return true;
        } else {
            for (int i = 0; i < src.length; i++) {
                if (i > child.length - 1) return false;

                if (src[i] == child[i]) {
                    byte[] sub = subByteArray(src, i, child.length);
                    if (isContain(sub, child)) return true;
                }
            }
        }

        return false;
    }

}
