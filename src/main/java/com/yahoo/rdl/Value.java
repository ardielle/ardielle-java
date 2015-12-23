/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.rdl;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Value oriented utilities
 */
public class Value {

    public static boolean asBoolean(Object o) {
        if (o == null) {
            return false;
        }
        return ((Boolean) o).booleanValue();
    }
    public static byte asByte(Object o) {
        if (o == null) {
            return 0;
        }
        return ((java.lang.Number) o).byteValue();
    }
    public static short asShort(Object o) {
        if (o == null) {
            return 0;
        }
        return ((java.lang.Number) o).shortValue();
    }
    public static int asInt(Object o) {
        if (o == null) {
            return 0;
        }
        return ((java.lang.Number) o).intValue();
    }
    public static long asLong(Object o) {
        if (o == null) {
            return 0;
        } else if (o instanceof String) {
            try {
                return Long.parseLong((String) o);
            } catch (NumberFormatException e) {
                //fall through and fail with type mismatch
            }
        } else if (o instanceof Timestamp) {
            return ((Timestamp) o).millis();
        }
        return ((java.lang.Number) o).longValue();
    }
    public static float asFloat(Object o) {
        if (o == null) {
            return 0;
        }
        return ((java.lang.Number) o).floatValue();
    }
    public static double asDouble(Object o) {
        if (o == null) {
            return 0;
        }
        return ((java.lang.Number) o).doubleValue();
    }
    @SuppressWarnings("rawtypes")
    public static String asString(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof byte []) {
            return encodeBase64((byte []) o, true);
        } else if (o instanceof Enum) {
            return ((Enum) o).name();
        } else {
            return String.valueOf(o);
        }
    }
    public static Timestamp asTimestamp(Object o) {
        if (o == null) {
            return null;
        }
        return Timestamp.fromString(o.toString());
    }
    public static UUID asUUID(Object o) {
        if (o == null) {
            return null;
        }
        return UUID.fromString(o.toString());
    }

    public static byte [] asBytes(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof String) {
            try {
                return ((String) o).getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                //fall through and fail with type mismatch
            }
        } else if (o instanceof UUID) {
            return ((UUID) o).toBytes();
        }
        return (byte []) o;
    }

    public static void appendToString(Object o, StringBuilder sb, String indent) {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof Struct) {
            ((Struct) o).appendToString(sb, indent);
        } else if (o instanceof Array) {
            ((Array) o).appendToString(sb, indent);
        } else if (o instanceof String) {
            escapeString(sb, (String) o);
        } else if (o instanceof Timestamp) {
            escapeString(sb, o.toString());
        } else if (o instanceof UUID) {
            escapeString(sb, o.toString());
        } else {
            sb.append(o.toString());
        }
    }

    static final char [] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    public static void escapeString(StringBuilder sb, String s) {
        if (s == null) {
            sb.append("\"\"");
        } else {
            int max = s.length();
            char [] chars = new char[max];
            s.getChars(0, max, chars, 0);
            sb.append('"');
            for (int i = 0; i < max; i++) {
                char c = chars[i];
                if (c == '\\') {
                    sb.append("\\\\");
                } else if (c == '"') {
                    sb.append("\\\"");
                } else if (c == '\b') {
                    sb.append("\\b");
                } else if (c == '\n') {
                    sb.append("\\n");
                } else if (c == '\r') {
                    sb.append("\\r");
                } else if (c == '\f') {
                    sb.append("\\f");
                } else if (c == '\t') {
                    sb.append("\\t");
                } else if (c < ' ') {
                    sb.append("\\u");
                    sb.append(HEX[(c >> 12) & 15]);
                    sb.append(HEX[(c >> 8) & 15]);
                    sb.append(HEX[(c >> 4) & 15]);
                    sb.append(HEX[(c) & 15]);
                } else {
                    sb.append(c);
                }
            }
            sb.append('"');
        }
    }

    public static boolean equals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        if (o1 instanceof byte []) {
            if (o2 instanceof byte []) {
                return Arrays.equals((byte[]) o1, (byte[]) o2);
            }
            return false;
        } else if (o1 instanceof java.lang.Number) {
            if (o2 instanceof java.lang.Number) {
                if (o1 instanceof Integer) {
                    return ((java.lang.Number) o2).intValue() == ((Integer) o1).intValue();
                } else if (o1 instanceof Double) {
                    return ((java.lang.Number) o2).doubleValue() == ((Double) o1).doubleValue();
                } else if (o1 instanceof Long) {
                    return ((java.lang.Number) o2).longValue() == ((Long) o1).longValue();
                } else if (o1 instanceof Float) {
                    return ((java.lang.Number) o2).floatValue() == ((Float) o1).floatValue();
                } else if (o1 instanceof Short) {
                    return ((java.lang.Number) o2).shortValue() == ((Short) o1).shortValue();
                } else if (o1 instanceof Byte) {
                    return ((java.lang.Number) o2).byteValue() == ((Byte) o1).byteValue();
                }
            }
            return false;
        } else {
            return o1.equals(o2);
        }
    }
    public static String encodeBase64(byte [] bytes, boolean urlSafe) {
        //note: Y64 is used inside Y!, it instead does "+/=" => "._-", which seems gratuitously
        //different than what most of the industry uses ("+/=" => "-_.") Why?!
        try {
            String b64 = javax.xml.bind.DatatypeConverter.printBase64Binary(bytes);
            if (urlSafe) {
                b64 = b64.replace("+", "-").replace("/", "_").replace("=", ".");
            }
            return b64;
        } catch (Exception err) {
            return null;
        }
    }

    public static byte [] decodeBase64(String encoded, boolean urlSafe) {
        try {
            if (urlSafe) {
                encoded = encoded.replace(".", "=").replace("_", "/").replace("-", "+");
            }
            return javax.xml.bind.DatatypeConverter.parseBase64Binary(encoded);
        } catch (Exception e) {
            return null;
        }
    }

    public static class TypedIterator<T> implements Iterator<T> {
        Array values;
        int i;
        TypedIterator(Array a) {
            values = a;
            i = 0;
        }
        public boolean hasNext() {
            return i < values.size();
        }
        @SuppressWarnings("unchecked")
        public T next() {
            return (T) values.get(i++);
        }
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }

    public static class TypedIterable<T> implements Iterable<T> {
        Array array;
        TypedIterable(Array a) {
            array = a;
        }
        public Iterator<T> iterator() {
            return new TypedIterator<T>(array);
        }
    }

    public static class TypedIterableIterable<T> implements Iterable<T> {
        Iterable<Object> it;
        TypedIterableIterable(Iterable<Object> i) {
            it = i;
        }
        public Iterator<T> iterator() {
            return new TypedIterableIterator<T>(it.iterator());
        }
    }

    public static class TypedIterableIterator<T> implements Iterator<T> {
        Iterator<Object> it;
        TypedIterableIterator(Iterator<Object> i) {
            it = i;
        }
        public boolean hasNext() {
            return it.hasNext();
        }
        @SuppressWarnings("unchecked")
        public T next() {
            return (T) it.next();
        }
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }


}
