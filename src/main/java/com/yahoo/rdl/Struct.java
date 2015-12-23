/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.rdl;
import java.util.Iterator;
import java.util.Arrays;

/**
 * A Struct is a structured value, with named fields.
 * A mapping of string names to values is maintained, with a predictable order.
 * Because the field order is definite, Structs congruent with each other
 * can be compared to produce a definite order.
 */
public class Struct  implements Iterable<Struct.Field>, java.util.Map<String, Object> {
    private static final int MIN_CAP = 16;
    private static final int MAX_GROWTH = 128;

    private String [] names;
    private Object [] values;
    private int count;

    public Struct(int cap) {
        int n = (cap < MIN_CAP) ? MIN_CAP : cap;
        names = new String[n];
        values = new Object[n];
        count = 0;
    }

    public Struct() {
        this(MIN_CAP);
    }
    
    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public class TypedArrayIterator<T> implements Iterator<T> {
        T [] array;
        int i;
        TypedArrayIterator(T [] a) {
            array = a;
            i = 0;
        }
        public boolean hasNext() {
            return i < count;
        }
        public T next() {
            return array[i++];
        }
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }

    public class TypedArrayIterable<T> implements Iterable<T> {
        T [] array;
        TypedArrayIterable(T [] a) {
            array = a;
        }
        public Iterator<T> iterator() {
            return new TypedArrayIterator<T>(array);
        }
    }

    public Iterable<String> names() {
        return new TypedArrayIterable<String>(names);
    }

    public java.util.Collection<Object> values() {
        java.util.ArrayList<Object> lst = new java.util.ArrayList<Object>(count);
        for (int i = 0; i < count; i++) {
            lst.add(values[i]);
        }
        return lst;
    }

    public class Field {
        private int idx;
        private Field(int i) {
            idx = i;
        }
        public String name() {
            return names[idx];
        }
        public Object value() {
            return values[idx];
        }
    }

    class StructIterator implements Iterator<Struct.Field> {
        int pos;
        private StructIterator() {
            pos = 0;
        }
        public boolean hasNext() {
            return pos < count;
        }
        public Struct.Field next() {
            return new Field(pos++);
        }
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }

    public Iterator<Struct.Field> iterator() {
        return new StructIterator();
    }

    /**
     * An optimized appender. Be careful to not introduce duplicate names!
     * @param name the field name
     * @param val the field value
     */
    public void append(String name, Object val) {
        if (count == names.length) {
            int oldCap = names.length;
            int newCap = (oldCap < MAX_GROWTH) ? oldCap * 2 : oldCap + MAX_GROWTH;
            String [] newNames = new String[newCap];
            System.arraycopy(names, 0, newNames, 0, count);
            Object [] newValues = new Object[newCap];
            System.arraycopy(values, 0, newValues, 0, count);
            names = newNames;
            values = newValues;
        }
        names[count] = name;
        values[count++] = val;
    }

    public Object put(String name, Object val) {
        Object result = null;
        int i = find(name);
        if (i < 0) {
            append(name, val);
        } else {
            result = values[i];
            values[i] = val;
        }
        return result;
    }

    public Object remove(Object name) {
        return remove((String) name);
    }
    public Object remove(String name) {
        Object result = null;
        int i = find(name);
        if (i >= 0) {
            result = values[i];
            count--;
            if (i < count) {
                System.arraycopy(names, i + 1, names, i, count - i);
                System.arraycopy(values, i + 1, values, i, count - i);
            }
            names[count] = null;
            values[count] = null;
        }
        return result;
    }

    public void putAll(java.util.Map<? extends String, ? extends Object> m) {
        for (java.util.Map.Entry<? extends String, ? extends Object> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public Struct with(String key, Object val) {
        put(key, val);
        return this;
    }

    public Struct withNonNull(String key, Object val) {
        if (val != null) {
            put(key, val);
        }
        return this;
    }

    public Struct with(Struct another) {
        putAll(another);
        return this;
    }

    public Struct without(String key) {
        remove(key);
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendToString(sb, "");
        return sb.toString();
    }

    void appendToString(StringBuilder sb, java.lang.String indent) {
        if (isEmpty()) {
            sb.append("{}");
        } else {
            sb.append("{");
            java.lang.String newIndent = null;
            if (indent != null) {
                newIndent = indent + "    ";
                sb.append("\n");
                sb.append(newIndent);
            }
            for (int i = 0; i < count; i++) {
                if (i > 0) {
                    if (newIndent != null) {
                        sb.append(",\n");
                        sb.append(newIndent);
                    } else {
                        sb.append(", ");
                    }
                }
                sb.append(names[i]);
                sb.append(": ");
                Value.appendToString(values[i], sb, newIndent);
            }
            if (newIndent != null) {
                sb.append("\n");
                sb.append(indent);
            }
            sb.append("}");
        }
    }

    public boolean equals(Object another) {
        if (another == this) {
            return true;
        }
        if (another instanceof Struct) {
            Struct other = (Struct) another;
            if (count == other.count) {
                for (int i = 0; i < count; i++) {
                    String k = names[i];
                    Object v = values[i];
                    Object o = other.get(k);
                    if (o != v) {
                        if (o == null) {
                            return false;
                        }
                        if (!Value.equals(v, o)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean has(String name) {
        return find(name) >= 0;
    }

    public String [] sortedNames() {
        String [] result = new String[count];
        System.arraycopy(names, 0, result, 0, count);
        Arrays.sort(result);
        return result;
    }

    private int find(String name) {
        for (int i = 0; i < count; i++) {
            if (name.equals(names[i])) {
                return i;
            }
        }
        return -1;
    }

    public Object get(String name) {
        int i = find(name);
        if (i < 0) {
            return null;
        }
        return values[i];
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        Object v = get(name);
        return (v == null) ? defaultValue : Value.asBoolean(v);
    }
    public boolean getBoolean(String name) {
        return getBoolean(name, false);
    }

    public byte getByte(String name, byte defaultValue) {
        Object v = get(name);
        return (v == null) ? defaultValue : Value.asByte(v);
    }
    public byte getByte(String name) {
        return getByte(name, (byte) 0);
    }

    public short getShort(String name, short defaultValue) {
        Object v = get(name);
        return (v == null) ? defaultValue : Value.asShort(v);
    }
    public short getShort(String name) {
        return getShort(name, (short) 0);
    }

    public int getInt(String name, int defaultValue) {
        Object v = get(name);
        return (v == null) ? defaultValue : Value.asInt(v);
    }
    public int getInt(String name) {
        return getInt(name, 0);
    }

    public long getLong(String name, long defaultValue) {
        Object v = get(name);
        return (v == null) ? defaultValue : Value.asLong(v);
    }
    public long getLong(String name) {
        return getLong(name, 0);
    }

    public float getFloat(String name, float defaultValue) {
        Object v = get(name);
        return (v == null) ? defaultValue : Value.asFloat(v);
    }
    public float getFloat(String name) {
        return getFloat(name, (float) 0.0);
    }

    public double getDouble(String name, double defaultValue) {
        Object v = get(name);
        return (v == null) ? defaultValue : Value.asDouble(v);
    }
    public double getDouble(String name) {
        return getDouble(name, (double) 0.0);
    }

    public byte [] getBytes(String name) {
        Object b = get(name);
        return (b == null) ? null : Value.asBytes(b);
    }

    public String getString(String name) {
        Object o = get(name);
        if (o == null) {
            return null;
        }
        return Value.asString(o);
    }
    public String getString(String name, String defaultValue) {
        Object o = get(name);
        if (o == null) {
            return defaultValue;
        }
        return (String) o;
    }

    public Struct getStruct(String name) {
        return (Struct) get(name);
    }

    public Timestamp getTimestamp(String name) {
        return Timestamp.fromObject(get(name));
    }

    public UUID getUUID(String name) {
        return UUID.fromObject(get(name));
    }

    /*
    public Array getArray(String name) {
        return (Array) get(name);
    }
    
    public Iterable<String> getStrings(String name) {
        Array a = getArray(name);
        if (a == null) {
            a = Array.empty();
        }
        return new Value.TypedIterable<String>(a);
    }
    public Iterable<Struct> getStructs(String name) {
        Array a = getArray(name);
        if (a == null) {
            a = Array.empty();
        }
        return new Value.TypedIterable<Struct>(a);
    }
    */
    // --- Map methods

    class Entry implements java.util.Map.Entry<String, Object> {
        String key;
        Object value;
        Entry(String k, Object v) {
            key = k; value = v;
        }
        public Object setValue(Object v) {
            Object o = value; value = v; return o;
        }
        public void setKey(String k) {
            key = k;
        }
        public Object getValue() {
            return value;
        }
        public String getKey() {
            return key;
        }
    }
    public java.util.Set<java.util.Map.Entry<String, Object>> entrySet() {
        java.util.HashSet<java.util.Map.Entry<String, Object>> set = new java.util.HashSet<java.util.Map.Entry<String, Object>>();
        for (int i = 0; i < count; i++) {
            set.add(new Entry(names[i], values[i]));
        }
        return set;
    }
    public java.util.Set<String> keySet() {
        java.util.HashSet<String> set = new java.util.HashSet<String>();
        for (int i = 0; i < count; i++) {
            set.add(names[i]);
        }
        return set;
    }
    public void clear() {
        count = 0;
    }
    public Object get(Object name) {
        return get((String) name);
    }
    public boolean containsKey(Object name) {
        return has((String) name);
    }
    public boolean containsValue(Object value) {
        if (value == null) {
            for (int i = 0; i < count; i++) {
                if (values[i] == null) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < count; i++) {
                if (Value.equals(value, values[i])) {
                    return true;
                }
            }
        }
        return false;
    }

}
