/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.rdl;

import java.util.Iterator;
import java.util.List;

/**
 * A Simple Array object for generic data. More convenient for JSON arrays, which can
 * easily be heterogeneous.
 */
public final class Array implements Iterable<Object>, List<Object> {

    private static final int MIN_CAP = 16;
    private static final int MAX_GROWTH = 128;

    private Object[] values;
    private int count;

    public Array(int cap) {
        int n = (cap < MIN_CAP) ? MIN_CAP : cap;
        values = new Object[n];
        count = 0;
    }

    public Array() {
        this(MIN_CAP);
    }

    public Array(List<Object> lst) {
        this(lst.size());
        addAll(lst);
    }

    public static Array empty() {
        return new Array();
    }

    public static Array from(Iterable<?> items) {
        if (items == null) {
            return null;
        }
        Array a = new Array();
        for (Object obj : items) {
            a.add(obj);
        }
        return a;
    }

    public boolean add(Object o) {
        if (count == values.length) {
            int oldCap = values.length;
            int newCap = (oldCap < MAX_GROWTH) ? oldCap * 2 : oldCap + MAX_GROWTH;
            Object[] newValues = new Object[newCap];
            System.arraycopy(values, 0, newValues, 0, count);
            values = newValues;
        }
        values[count++] = o;
        return true;
    }

    public boolean addAll(java.util.Collection<? extends Object> i) {
        boolean changed = false;
        // public void addAll(Iterable<Object> i) {
        for (Object o : i) {
            add(o);
            changed = true;
        }
        return changed;
    }

    public Array with(Object o) {
        add(o);
        return this;
    }

    public Array without(Object o) {
        remove(o);
        return this;
    }

    public Array slice(int start, int end) {
        int count = size();
        if (start < 0) {
            start = 0;
        }
        if (end > count || end < 0) {
            end = count;
        }
        if (end < start) {
            end = start;
        }
        if (end > start) {
            Array a = new Array(end - start);
            for (int i = start; i < end; i++) {
                a.add(get(i));
            }
            return a;
        } else {
            return empty();
        }
    }

    public Array copy() {
        return slice(0, -1);
    }

    public int size() {
        return count;
    }

    public Object get(int idx) {
        return values[idx];
    }

    public String getString(int idx) {
        return (String) get(idx);
    }

    public Struct getStruct(int idx) {
        return (Struct) get(idx);
    }

    public Array getArray(int idx) {
        return (Array) get(idx);
    }

    public Timestamp getTimestamp(int idx) {
        return Timestamp.fromObject(get(idx));
    }

    public byte[] getBytes(int idx) {
        return (byte[]) get(idx);
    }

    public boolean getBoolean(int idx) {
        return Value.asBoolean(get(idx));
    }

    public byte getByte(int idx) {
        return Value.asByte(get(idx));
    }

    public short getShort(int idx) {
        return Value.asShort(get(idx));
    }

    public int getInt(int idx) {
        return Value.asInt(get(idx));
    }

    public long getLong(int idx) {
        return Value.asLong(get(idx));
    }

    public float getFloat(int idx) {
        return Value.asFloat(get(idx));
    }

    public double getDouble(int idx) {
        return Value.asDouble(get(idx));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendToString(sb, "");
        return sb.toString();
    }

    void appendToString(StringBuilder sb, java.lang.String indent) {
        int count = size();
        if (count > 0) {
            sb.append("[");
            java.lang.String newIndent = null;
            if (count == 1) {
                Object o = get(0);
                if (!(o instanceof Array) && !(o instanceof Struct)) {
                    Value.appendToString(o, sb, null);
                    sb.append("]");
                    return;
                }
            }
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
                Object item = get(i);
                if (item != null) {
                    Value.appendToString(item, sb, newIndent);
                } else {
                    sb.append("null");
                }
            }
            if (indent != null) {
                sb.append("\n");
                sb.append(indent);
            }
            sb.append("]");
        } else {
            sb.append("[]");
        }
    }

    public class TypedArrayIterator<T> implements Iterator<T> {
        T[] array;
        int i;

        TypedArrayIterator(T[] a) {
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

    public Iterator<Object> iterator() {
        return new TypedArrayIterator<Object>(values);
    }

    public Iterable<Struct> asStructs() {
        return new Value.TypedIterable<Struct>(this);
    }

    public Iterable<String> asStrings() {
        return new Value.TypedIterable<String>(this);
    }


    public boolean equals(Object another) {
        if (another == this) {
            return true;
        }
        if (another instanceof Array) {
            Array other = (Array) another;
            if (count == other.count) {
                for (int i = 0; i < count; i++) {
                    if (!Value.equals(values[i], other.values[i])) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    // --- List methods

    public List<Object> subList(int fromIndex, int toIndex) {
        if (toIndex >= count) {
            throw new IndexOutOfBoundsException();
        }
        int size = toIndex - fromIndex;
        if (size <= 0) {
            return Array.empty();
        }
        Array a = new Array(size);
        for (int i = fromIndex; i < toIndex; i++) {
            a.add(values[i]);
        }
        return a;
    }

    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < count; i++) {
                if (values[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < count; i++) {
                if (o.equals(values[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = count - 1; i >= 0; i--) {
                if (values[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = count - 1; i >= 0; i--) {
                if (o.equals(values[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void clear() {
        count = 0;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean remove(Object o) {
        int i = indexOf(o);
        if (i < 0) {
            return false;
        }
        remove(i);
        return true;
    }

    public Object remove(int index) {
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException();
        }
        Object result = values[index];
        count--;
        int tail = count - index;
        if (tail > 0) {
            System.arraycopy(values, index + 1, values, index, tail);
        }
        values[count] = null;
        return result;
    }

    public boolean containsAll(java.util.Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    public boolean removeAll(java.util.Collection<?> c) {
        boolean changed = false;
        Object[] newValues = new Object[values.length];
        int n = 0;
        for (int i = 0; i < count; i++) {
            if (!c.contains(values[i])) {
                newValues[n++] = values[i];
            } else {
                changed = true;
            }
        }
        values = newValues;
        count = n;
        return changed;

    }

    public boolean retainAll(java.util.Collection<?> c) {
        boolean changed = false;
        Object[] newValues = new Object[values.length];
        int n = 0;
        for (int i = 0; i < count; i++) {
            if (c.contains(values[i])) {
                newValues[n++] = values[i];
            } else {
                changed = true;
            }
        }
        values = newValues;
        count = n;
        return changed;
    }

    public Object set(int index, Object element) {
        if (index >= count || index < 0) {
            throw new IndexOutOfBoundsException();
        }
        Object result = values[index];
        values[index] = element;
        return result;
    }

    public void add(int index, Object element) {
        if (index > count || index < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (index == count) {
            add(element);
        } else {
            if (count == values.length) {
                int oldCap = values.length;
                int newCap = (oldCap < MAX_GROWTH) ? oldCap * 2 : oldCap + MAX_GROWTH;
                Object[] newValues = new Object[newCap];
                System.arraycopy(values, 0, newValues, 0, index);
                System.arraycopy(values, index, newValues, index + 1, count - index);
                values = newValues;
            } else {
                System.arraycopy(values, index, values, index + 1, count - index);
            }
            values[index] = element;
            count++;
        }
    }

    public boolean addAll(int index, java.util.Collection<? extends Object> c) {
        boolean changed = c.size() > 0;
        if (index > count || index < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (index == count) {
            addAll(c);
        } else {
            int oldCap = values.length;
            int cSize = c.size();
            int newSize = count + cSize;
            if (newSize >= oldCap) {
                int newCap = oldCap + cSize;
                Object[] newValues = new Object[newCap];
                System.arraycopy(values, 0, newValues, 0, index);
                System.arraycopy(values, index, newValues, index + cSize, count - index);
                values = newValues;
            } else {
                System.arraycopy(values, index, values, index + cSize, count - index);
            }
            for (Object o : c) {
                values[index++] = o;
            }
        }
        return changed;
    }

    public class ListArrayIterator implements java.util.ListIterator<Object> {
        int i;

        ListArrayIterator(int start) {
            i = start;
        }

        public boolean hasNext() {
            return i < count;
        }

        public boolean hasPrevious() {
            return i > 0;
        }

        public Object next() {
            return values[i++];
        }

        public int nextIndex() {
            return i;
        }

        public int previousIndex() {
            return i - 1;
        }

        public Object previous() {
            return values[--i];
        }

        public void add(Object o) {
            throw new UnsupportedOperationException();
        }

        public void set(Object o) {
            throw new UnsupportedOperationException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public java.util.ListIterator<Object> listIterator() {
        return new ListArrayIterator(0);
    }

    public java.util.ListIterator<Object> listIterator(int index) {
        return new ListArrayIterator(index);
    }

    public Object[] toArray() {
        Object[] result = new Object[count];
        System.arraycopy(values, 0, result, 0, count);
        return result;
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }
}
