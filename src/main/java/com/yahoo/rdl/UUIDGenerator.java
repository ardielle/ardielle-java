/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.rdl;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.UUID;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.security.MessageDigest;


/**
 * UUIDGenerator implements type 1, 3, 4,and 5 UUID generation.
 * The code understands standard UUID string format, byte array, and java.util.UUID.
 */
class UUIDGenerator {

    /** The URL namespace, per RFC, in our optimized byte (non-standard) byte order */
    static private final byte [] xURL_NAMESPACE = {17, -47, -99, -83, 107, -89, -72, 17, -128, -76, 0, -64, 79, -44, 48, -56};
    static public final byte [] URL_NAMESPACE = standardBytes(xURL_NAMESPACE);

    static byte [] standardBytes(byte [] sortableBytes) {
        byte [] std = new byte[16];
        copyToStandardBytes(sortableBytes, std);
        return std;
    }
    /** copy the bytes to the target in the same order as the character representation (per RFC). */
    static private void copyToStandardBytes(byte [] b, byte [] result) {
        System.arraycopy(b, 8, result, 8, 8);
        result[0] = b[4];
        result[1] = b[5];
        result[2] = b[6];
        result[3] = b[7];
        result[4] = b[2];
        result[5] = b[3];
        result[6] = b[0];
        result[7] = b[1];
    }

    /** Copy the bytes from the standard byte order (per RFC) to useful sortable order */
    static private void copyFromStandardBytes(byte [] b, byte [] result) {
        System.arraycopy(b, 8, result, 8, 8);
        result[4] = b[0];
        result[5] = b[1];
        result[6] = b[2];
        result[7] = b[3];
        result[2] = b[4];
        result[3] = b[5];
        result[0] = b[6];
        result[1] = b[7];
    }

    private static Random rng = new Random();
    private static int clockSeq = rng.nextInt() & 0x3FFF;

    /**
     * Return a 6 byte random value, usable as a node in uuid3/uuid5 routines
     * @return 6 byte random value
     */
    public static byte [] getRandomNode() {
        byte [] result = new byte[6];
        rng.nextBytes(result);
        return result;
    }

    /**
     * Return a 6 byte ethernet hardware (MAC) address for the first ethernet
     * interface encountered on the machine.
     * @return 6 byte ethernet hardware address
     */
    @SuppressWarnings("rawtypes")
    public static byte [] getEthernetNode() {
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface nif = (NetworkInterface) e.nextElement();
                if (nif.getDisplayName().startsWith("e")) {
                    byte [] node = nif.getHardwareAddress();
                    return node;
                }
            }
        } catch (SocketException e) {
        }
        return null;
    }

    /**
     * Turns a timestamp (100 nanosecond ticks since 00:00:00.00, 15 October 1582) back into a java milliseconds
     * since Jan 1 1970 number.
     * @param timestamp the timestamp value
     * @return the corresponding millis value
     */
    public static long timestampToMillis(long timestamp) {
        return ((timestamp / 10000) - 12219292800000L);
    }

    /**
     * Convert millis (milliseconds since Jan 1 1970) into a timestamp (100 nanosecond ticks since 00:00:00.00, 15 October 1582)
     * @param millis the millis value
     * @return the correspnding timestamp value
     */
    public static long timestampFromMillis(long millis) {
        return ((millis + 12219292800000L) * 10000);
    }

    /**
     * Return a timestamp. This implementation fills in the resolution with a
     * counter, and when that counter overflows, it blocks until the next system
     * clock tick.
     * @return the number of 100 nanosecond ticks since 00:00:00.00, 15 October 1582
     */
    public static long timestamp() {
        long now = System.currentTimeMillis();
        int ticks = 0;
        if (now < prevMillis) {
            //clock went backwards. this is handled by waiting, not by adjusting clock_seq.
            long sleepMillis = prevMillis - now + 1;
            System.err.println("WARNING: clock went backwards, pausing for " + sleepMillis + " ms");
            pause(sleepMillis);
            return timestamp(); //try again
        }
        if (now == prevMillis) {
            ticks = counter.incrementAndGet();
            if (ticks == (10000 - JITTER)) {
                //System.err.println("WARNING: too many calls to timestamp(), must pause at millis = " + now);
                pause(1);
                return timestamp(); //try again
            }
        } else {
            prevMillis = now;
            ticks = ticks % JITTER; //reuse the low 8 bits as a kind of random starting point
            counter.set(ticks); //when millis changes, we start at a random place, so multiple VMs will tend not to collide
        }
        return timestampFromMillis(now) + ticks;
    }
    private static long prevMillis = 0;
    private static final int JITTER = 256;
    private static AtomicInteger counter = new AtomicInteger(rng.nextInt() % JITTER);
    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Return the byte array representing a version 1 (time-based) UUID.
     * @param node the 6 element byte array used as the "node" field. Defaults to the first MAC address.
     * @return a the 16 bytes, in Comparable order
     */
    public static byte [] type1(byte [] node) {
        return type1(node, timestamp());
    }
    public static byte [] type1(byte [] node, long ticks) {
        byte [] b = new byte[16];
        b[4] = (byte) (ticks >> 24);
        b[5] = (byte) (ticks >> 16);
        b[6] = (byte) (ticks >> 8);
        b[7] = (byte) (ticks);
        b[3] = (byte) (ticks >> 32);
        b[2] = (byte) (ticks >> 40);
        b[0] = (byte) (((ticks >> 56) & 0x0f) + 0x10); //the version for time-based UUID is 1, the high nibble of time will not overflow until the year 5236.
        b[1] = (byte) (ticks >> 48);

        int clk = 0x8000 + clockSeq;
        b[8] = (byte) (clk >> 8);
        b[9] = (byte) (clk);

        System.arraycopy(node, 0, b, 10, 6);
        return standardBytes(b);
    }

    /**
     * Create a UUID based on a version 3 (MD5-name-based) UUID for the given data and namespace
     * @param str the string from which to form this UUID
     * @param namespace the namespace UUID bytes
     * @return a type 3 name based GUID
     */
    public static byte [] type3(String str, byte [] namespace) {
        byte [] data = str.getBytes();
        byte []  context = new byte[16 + data.length];
        System.arraycopy(namespace, 0, context, 0, 16);
        System.arraycopy(data, 0, context, 16, data.length);
        try {
            MessageDigest hash = MessageDigest.getInstance("MD5");
            byte [] digest = hash.digest(context);
            byte [] b = new byte[16];
            System.arraycopy(digest, 0, b, 0, 16);
            b[0] = (byte) ((digest[6] & 0x0f) | 0x30);
            b[1] = digest[7];
            b[2] = digest[4];
            b[3] = digest[5];
            b[4] = digest[0];
            b[5] = digest[1];
            b[6] = digest[2];
            b[7] = digest[3];
            b[8] = (byte) ((digest[8] & 0x3f) | 0x80);
            return standardBytes(b);
        } catch (Exception e) {
            return null;
        }
    }

    public static byte [] type4() {
        //not optimized
        return toBytes(UUID.randomUUID().toString());
    }

    public static byte [] type5(String uri, byte [] namespace) {
        byte [] data = uri.getBytes();
        byte []  context = new byte[16 + data.length];
        System.arraycopy(namespace, 0, context, 0, 16);
        System.arraycopy(data, 0, context, 16, data.length);
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA1");
            byte [] digest = hash.digest(context);
            byte [] b = new byte[16];
            System.arraycopy(digest, 0, b, 0, 16);
            b[0] = (byte) ((digest[6] & 0x0f) | 0x50);
            b[1] = digest[7];
            b[2] = digest[4];
            b[3] = digest[5];
            b[4] = digest[0];
            b[5] = digest[1];
            b[6] = digest[2];
            b[7] = digest[3];
            b[8] = (byte) ((digest[8] & 0x3f) | 0x80);
            return standardBytes(b);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Return a byte array representation of the UUID. This byte array
     * is arranged such that type 1 UUIDs can be compared sensibly.
     *
     * @param uuid the UUID
     * @return the equivalent byte array of 16 bytes
     */
    public static byte [] toBytes(java.util.UUID uuid) {
        long hi = uuid.getMostSignificantBits();
        long lo = uuid.getLeastSignificantBits();
        byte [] b = new byte[16];
        b[0] = (byte) (hi >> 8); //version, time_hi_hi
        b[1] = (byte) (hi); //time_hi_lo
        b[2] = (byte) (hi >> 24); //time_mid_hi
        b[3] = (byte) (hi >> 16); //time_mid_lo
        b[4] = (byte) (hi >> 56); //time_lo_hi
        b[5] = (byte) (hi >> 48); //time_lo_mid_hi
        b[6] = (byte) (hi >> 40); //time_lo_mid_lo
        b[7] = (byte) (hi >> 32); //time_lo_lo

        //and the low order bits don't really matter, they have no real order in them
        b[8] = (byte) (lo >> 56);
        b[9] = (byte) (lo >> 48);
        b[10] = (byte) (lo >> 40);
        b[11] = (byte) (lo >> 32);
        b[12] = (byte) (lo >> 24);
        b[13] = (byte) (lo >> 16);
        b[14] = (byte) (lo >> 8);
        b[15] = (byte) (lo);

        return standardBytes(b);
    }

    public static byte [] toBytes(String uuid) {
        byte [] std = new byte[16];
        byte [] b = new byte[16];
        int j = 0;
        for (int i = 0; i < 8; i += 2) {
            std[j++] = (byte) java.lang.Integer.parseInt(uuid.substring(i, i + 2), 16);
        }
        for (int i = 9; i < 13; i += 2) {
            std[j++] = (byte) java.lang.Integer.parseInt(uuid.substring(i, i + 2), 16);
        }
        for (int i = 14; i < 18; i += 2) {
            std[j++] = (byte) java.lang.Integer.parseInt(uuid.substring(i, i + 2), 16);
        }
        for (int i = 19; i < 23; i += 2) {
            std[j++] = (byte) java.lang.Integer.parseInt(uuid.substring(i, i + 2), 16);
        }
        for (int i = 24; i < 36; i += 2) {
            std[j++] = (byte) java.lang.Integer.parseInt(uuid.substring(i, i + 2), 16);
        }
        return std;
    }

    public static java.util.UUID toJavaUUID(byte [] std) {
        byte [] b = std;
        long l0 = (((long) b[4] & 255) << 56) +
            (((long) b[5] & 255) << 48) +
            (((long) b[6] & 255) << 40) +
            (((long) b[7] & 255) << 32) +
            (((long) b[2] & 255) << 24) +
            (((long) b[3] & 255) << 16) +
            (((long) b[0] & 255) << 8) +
            (((long) b[1] & 255));
        long l1 = (((long) b[8] & 255) << 56) +
            (((long) b[9] & 255) << 48) +
            (((long) b[10] & 255) << 40) +
            (((long) b[11] & 255) << 32) +
            (((long) b[12] & 255) << 24) +
            (((long) b[13] & 255) << 16) +
            (((long) b[14] & 255) << 8) +
            (((long) b[15] & 255));
        return new java.util.UUID(l0, l1);
    }

    public static UUID toUUID(String uuid) {
        return UUID.fromString(uuid);
    }

    public static String toString(byte [] std) {
        //byte [] std = new byte[16];
        //copyToStandardBytes(bytes, std);
        char [] hex = new char[32];
        int i = 0;
        for (byte b : std) {
            hex[i++] = Character.forDigit((b >> 4) & 15, 16);
            hex[i++] = Character.forDigit(b & 15, 16);
        }
        StringBuilder sb = new StringBuilder();
        for (i = 0; i < 8; i++) {
            sb.append(hex[i]);
        }
        sb.append('-');
        for (i = 8; i < 12; i++) {
            sb.append(hex[i]);
        }
        sb.append('-');
        for (i = 12; i < 16; i++) {
            sb.append(hex[i]);
        }
        sb.append('-');
        for (i = 16; i < 20; i++) {
            sb.append(hex[i]);
        }
        sb.append('-');
        for (i = 20; i < 32; i++) {
            sb.append(hex[i]);
        }
        return sb.toString();
    }

}
