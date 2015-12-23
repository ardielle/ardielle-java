/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.rdl;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * An standalone implementation of RFC 4122 UUIDs.
 * Supports type 1 (time-based), type 3 (MD5 name based, for URL namespace), type 4 (random),
 * and type 5 (SHA1 name based, for URL namespace) GUIDs.
 * Immutable, like a String.
 *
 * See RFC 4122 (http://www.ietf.org/rfc/rfc4122.txt) for details.
 *
 */
@JsonSerialize(using = UUID.UUIDJsonSerializer.class)
@JsonDeserialize(using = UUID.UUIDJsonDeserializer.class)
public class UUID {

    public static boolean debug = false;

    //constants established by the RFC
    public static final UUID NAMESPACE_X500 = UUID.fromString("6ba7b814-9dad-11d1-80b4-00c04fd430c8");
    public static final UUID NAMESPACE_URL = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");
    public static final UUID NAMESPACE_DNS = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
    public static final UUID NAMESPACE_OID = UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8");

    String repr;
    private UUID(String s) {
        repr = s;
    }
    private UUID(byte [] bytes) {
        repr = UUIDGenerator.toString(bytes);
    }

    public static class UUIDJsonSerializer extends JsonSerializer<UUID> {
        @Override
        public void serialize(UUID value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeString(value.toString());
        }
    }
    public static class UUIDJsonDeserializer extends JsonDeserializer<UUID> {
        @Override
        public UUID deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            String s = jp.getText();
            return UUID.fromString(s);
        }
    }

    @Override
    public boolean equals(Object another) {
        if (another instanceof UUID) {
            return repr.equals(((UUID) another).repr);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return repr.hashCode();
    }

    @Override
    public String toString() {
        return repr;
    }

    public byte [] toBytes() {
        return UUIDGenerator.toBytes(repr);
    }

    public static UUID fromString(String s) {
        UUID u = UUID.parse(s);
        if (u == null) {
            u = UUID.fromURL(s);
            if (u == null) {
                if ("NAMESPACE_URL".equals(s)) {
                    u = NAMESPACE_URL;
                } else if ("NAMESPACE_DNS".equals(s)) {
                    u = NAMESPACE_DNS;
                } else if ("NAMESPACE_OID".equals(s)) {
                    u = NAMESPACE_OID;
                } else if ("NAMESPACE_X500".equals(s)) {
                    u = NAMESPACE_X500;
                }
            }
        }
        return u;
    }

    public static UUID fromBytes(byte [] bytes) {
        if (bytes != null && bytes.length == 16) {
            //there is additional validation we could do here...
            return new UUID(bytes);
        }
        return null;
    }

    public static UUID fromObject(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof UUID) {
            return (UUID) o;
        } else if (o instanceof String) {
            return fromString((String) o);
        } else if (o instanceof byte []) {
            return fromBytes((byte []) o);
        } else {
            return (UUID) o;
        }
    }

    static UUID parse(String s) {
        //this should be optimized for "fast-fail", as it is used as a check to see *if* a string is a UUID.
        int len = s.length();
        //URN syntax: urn:uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6
        String urnPrefix = "urn:uuid:";
        if (len == 45 && s.startsWith(urnPrefix)) {
            s = s.substring(urnPrefix.length());
            len = len - urnPrefix.length();
        }
        //UUID syntax
        if (len == 36 && s.charAt(8) == '-' && s.charAt(13) == '-' && s.charAt(18) == '-' && s.charAt(23) == '-') {
            try {
                Long.parseLong(s.substring(0, 8), 16);
                Integer.parseInt(s.substring(9, 13), 16);
                Integer.parseInt(s.substring(14, 18), 16);
                Integer.parseInt(s.substring(19, 23), 16);
                Long.parseLong(s.substring(24, 36), 16);
                UUID u = new UUID(s);
                int v = u.version();
                if (v != 1 && v != 3 && v != 4 && v != 5) {
                    return null;
                }

                return u;
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
        return null;
    }

    public static boolean isUUID(Object v) {
        if (v instanceof UUID) {
            return parse(String.valueOf(v)) != null;
        }
        return false;
    }

    public long millis() {
        return UUIDGenerator.timestampToMillis(ticks());
    }

    public int version() {
        String s = repr;
        char c = s.charAt(14);
        return (int) (c - '0');
    }

    /**
     * @return ticks whose unit is 100 nanosecond ticks since 00:00:00.00, 15 October 1582
     */
    public long ticks() {
        String s = repr;
        char c = s.charAt(14);
        int ver = (int) (c - '0');
        if (ver != 1) {
            return 0;
        }
        long result = Integer.parseInt(s.substring(15, 18), 16);
        result = (result << 16) + Integer.parseInt(s.substring(9, 13), 16);
        result = (result << 32) + Long.parseLong(s.substring(0, 8), 16);
        return result;
    }

    /*
      public URI toURI() {
      return URI.fromUUID(this);
      }
    */

    public static boolean validate(String s) {
        try {
            parse(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static Object lock = new Object();
    static byte [] node = null;

    public static byte [] bytesFromTime(long ticks) {
        synchronized (lock) {
            if (node == null) {
                //node = UUIDGenerator.getEthernetNode();
                node = UUIDGenerator.getRandomNode();
            }
        }
        return UUIDGenerator.type1(node, ticks);
    }
    public static String stringFromMillis(long millis) {
        long ticks = UUIDGenerator.timestampFromMillis(millis);
        return UUIDGenerator.toString(bytesFromTime(ticks));
    }
    public static UUID fromMillis(long millis) {
        return new UUID(stringFromMillis(millis));
    }

    public static byte [] bytesFromCurrentTime() {
        synchronized (lock) {
            if (node == null) {
                //node = UUIDGenerator.getEthernetNode();
                node = UUIDGenerator.getRandomNode();
            }
        }
        return UUIDGenerator.type1(node);
    }
    public static String stringFromCurrentTime() {
        return UUIDGenerator.toString(bytesFromCurrentTime());
    }
    public static UUID fromCurrentTime() {
        return new UUID(stringFromCurrentTime());
    }

    public static UUID fromURL(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            return fromURL(u);
        } catch (Exception e) {
            return null;
        }
    }

    public static UUID fromURL(java.net.URL url) {
        return new UUID(UUIDGenerator.type3(url.toString(), UUIDGenerator.URL_NAMESPACE));
    }

    public static UUID fromName(UUID namespace, String name) {
        byte [] uuid = UUIDGenerator.type3(name, namespace.toBytes());
        return new UUID(UUIDGenerator.toString(uuid));
    }


    //UUID ordering does not look obvious, as it is not the string representation that is sorted,
    //but instead the underlying byte array, which is shuffled a bit such that
    //time-based UUIDs sort as you would expect.
    public int compareTo(UUID another) {

        byte [] bytes = toBytes();
        byte [] obytes = another.toBytes();
        int b = bytes[0] & 255;
        int ob = obytes[0] & 255;
        if (b == ob) {
            for (int i = 1; i < 16; i++) {
                b = bytes[i] & 255;
                ob = obytes[i] & 255;
                if (b != ob) {
                    break;
                }
            }
        }
        return (b - ob);
    }

    /**
     * Useful utility driver to get UUIDs form the command line.
     * Run with "-h" argument to see usage.
     * @param args command line arguments
     */
    public static void main(String [] args) {
        String ns = null;
        String name = null;
        switch (args.length) {
        case 1:
            ns = null;
            name = args[0];
            break;
        case 2:
            ns = args[0];
            name = args[1];
            break;
        default:
            break;
        }
        if (name == null)  {
            System.out.println(UUID.fromCurrentTime());
        } else if (ns == null) {
            if ("-h".equals(name)) {
                System.out.println("Usage: uuid");
                System.out.println("   Produce a type 1 UUID.");
                System.out.println("Usage: uuid url");
                System.out.println("   Produce a type 3 UUID in the 'URL' namespace for the specified URL");
                System.out.println("Usage: uuid namespace name");
                System.out.println("   Produce a type 3 UUID in the specified namespace for the specified name");
                System.out.println("   The namspace can be either a known UUID or a URL");
                System.out.println("Usage: -h");
                System.out.println("   print this help");
                System.out.println("Reference: http://tools.ietf.org/rfc/rfc4122.txt");
            } else {
                UUID u = UUID.fromString(name); //rather than limit to URL, allow "urn:uuid:" URIs, and names like NAMESPACE_URL
                if (u == null) {
                    System.out.println("Invalid url/name");
                    System.exit(1);
                }
                System.out.println(u);
            }
        } else {
            UUID u = UUID.fromString(ns);
            if (u == null) {
                System.out.println("Namespace not a UUID or absolute URL: " + ns);
                System.exit(1);
            } else {
                System.out.println(UUID.fromName(u, name));
            }
        }
    }
}
