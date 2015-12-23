/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.tbin;
import com.yahoo.rdl.*;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

/**
 * TBin decoding logic
 */
public class TBinDecoder extends TBin {

    private InputStream raw;
    private BufferedInputStream in;
    private ArrayList<String> syms;
    private ArrayList<TypeDef> types;
    private byte [] buf;
    private int dataVersion;
    private int currentCount;

    public TBinDecoder(InputStream in) {
        this.raw = in;
        this.in = new BufferedInputStream(this.raw);
        this.syms = new ArrayList<String>(1000);
        this.types = new ArrayList<TypeDef>();
        this.buf = new byte[256]; //if tokens are smaller than this (they usually are), allocation is avoided
        this.dataVersion = 0;
        this.currentCount = 0;
    }

    public void close() throws IOException {
        in.close();
        raw.close();
    }

    @SuppressWarnings({"unchecked"})
    public <T> T decode(Class<T> dataClass) throws IOException {
        if (dataClass == Object.class) {
            return (T)decode();
        }
        TypeDef dataType = nextType();
        return decode(dataType, dataClass);
    }

    //public <T> T decode(TypeDef type, Class<T> dataClass) throws IOException {
    //    println("=========== decode(" + type + ", " + dataClass.getName() + ")");
    //    T result = decode2(type, dataClass);
    //    println("===========> " + result);
    //    return result;
    //}
    @SuppressWarnings({"unchecked", "rawtypes" })
    public <T> T decode(TypeDef type, Class<T> dataClass) throws IOException {
        T result;
        int max;
        Struct struct;
        switch (type.tag) {
        case BOOL_TAG:
            return (T) new Boolean(nextBoolean());
        case INT8_TAG:
            return (T) new Byte(nextByte());
        case INT16_TAG:
            return (T) new Short(nextShort());
        case INT32_TAG:
            return (T) new Integer(nextInt());
        case INT64_TAG:
            return (T) new Long(nextLong());
        case FLOAT32_TAG:
            return (T) new Float(nextFloat());
        case FLOAT64_TAG:
            return (T) new Double(nextDouble());
        case BYTES_TAG:
            return (T) nextByteArray();
        case STRING_TAG:
            return (T) nextString();
        case TIMESTAMP_TAG:
            return (T) nextTimestamp();
        case SYMBOL_TAG:
            return (T) nextSymbol();
        case UUID_TAG:
            return (T) nextUUID();
        case ARRAY_TAG:
            return decodeArray(type, dataClass);
        case MAP_TAG:
            return decodeMap(type, dataClass);
        case STRUCT_TAG:
            return decodeStruct(type, dataClass);
        case UNION_TAG:
            return decodeUnion(type, dataClass);
        case ANY_TAG:
            return decode(nextType(), dataClass);
        case NULL_TAG:
            return null;
        default:
            throw new TBinException("Unsupported type in TBin stream: " + type);
        }
    }

    @SuppressWarnings({"unchecked"})
    <T> T decodeArray(TypeDef otype, Class<T> oclass) throws IOException {
        int count = nextCount(otype);
        if (java.util.List.class.isAssignableFrom(oclass)) {
            try {
                List list = (List)ArrayList.class.newInstance();
                for (int i=0; i<count; i++) {
                    list.add(decode(otype.items, Object.class)); //type erasure means we don't know the item class
                }
                return (T)list;
            } catch (InstantiationException|IllegalAccessException e) {
            }
        } else if (oclass == Object.class || oclass == Array.class) {
            Array ary = new Array();
            for (int i = 0; i < count; i++) {
                TypeDef itemType = nextItemType(otype, i);
                Object o = decode(itemType);
                ary.add(o);
            }
            return (T)ary;
        }
        throw new TBinException("Cannot instantiate array as target object of class " + oclass.getName());
    }

    @SuppressWarnings({"unchecked"})
    <T> T decodeMap(TypeDef otype, Class<T> oclass) throws IOException {
        int count = nextCount(otype);
        if (java.util.Map.class.isAssignableFrom(oclass)) {
            try {
                Map map = (Map)HashMap.class.newInstance();
                for (int i=0; i<count; i++) {
                    Object key = decode(otype.keys, Object.class);
                    Object val = decode(otype.items, Object.class);
                    map.put(key, val); //type erasure means we don't know the key or item class -- this might throw
                }
                return (T)map;
            } catch (InstantiationException|IllegalAccessException e) {
            }
        } else {
            try {
                T o = oclass.newInstance();
                for (int i = 0; i < count; i++) {
                    Object key = decode(otype.keys, Object.class);
                    String fname = null;
                    if (key instanceof String) {
                        fname = (String)key;
                    } else if (key instanceof Symbol) {
                        fname = ((Symbol)key).name;
                    } else {
                        throw new TBinException("Cannot instantiate map as target object of class " + oclass.getName());
                    }
                    Object val = decode(otype.items, Object.class);
                    TypeDef ftype = otype.items;
                    Field f = oclass.getDeclaredField(fname);
                    if (f != null) {
                        f.setAccessible(true);
                        f.set(o, val);
                    }
                }
                return o;
            } catch (InstantiationException|IllegalAccessException|NoSuchFieldException e) {
                throw new TBinException("Cannot instantiate map as target object of class " + oclass.getName());
            }
        }
        throw new TBinException("Cannot instantiate map as target object of class " + oclass.getName());
    }

    @SuppressWarnings({"unchecked"})
    <T> T decodeUnion(TypeDef otype, Class<T> oclass) throws IOException {
        int variant = readNonNegativeInt();
        TypeDef utype = otype.variants.get(variant-1);
        try {
            T union = (T)oclass.newInstance();
            Field[] fields = oclass.getFields();
            Field vfield = fields[0];
            Field ufield = fields[variant];
            Class<?> uclass = ufield.getType();
            vfield.setAccessible(true);
            String n = ufield.getName();
            vfield.set(union, Enum.valueOf((Class<Enum>)vfield.getType(), ufield.getName()));
            ufield.set(union, decode(utype, uclass));
            return union;
        } catch (InstantiationException|IllegalAccessException e) {
            throw new TBinException("Cannot instantiate map as target object of class " + oclass.getName());
        }
    }

    @SuppressWarnings({"unchecked"})
    <T> T decodeStruct(TypeDef otype, Class<T> oclass) throws IOException {
        if (oclass == Struct.class || oclass == Object.class) {
            Struct s = new Struct();
            int fcount = nextCount(otype);
            for (int i = 0; i < fcount; i++) {
                String fname = nextItemName(otype, i);
                TypeDef ftype = nextItemType(otype, i);
                Object fval = decode(ftype, Object.class);
                s.put(fname, fval);
            }
            return (T)s;
        }
        T o;
        try {
            o = oclass.newInstance();
            int fcount = (otype.fields != null)? otype.fields.size() : nextCount(otype);
            for (int i = 0; i < fcount; i++) {
                String fname = nextItemName(otype, i);
                TypeDef ftype = nextItemType(otype, i);
                fname = sanitizeFieldName(fname);
                Field f = oclass.getDeclaredField(fname);
                f.setAccessible(true);
                Class<?> fclass = f.getType();
                decodeStructField(o, f, ftype, fclass, oclass);
            }
        } catch (InstantiationException|IllegalAccessException|NoSuchFieldException e) {
            e.printStackTrace();
            throw new TBinException("Cannot instantiate target object of class " + oclass.getName());
        }
        return o;
    }

    String sanitizeFieldName(String fname) {
        if ("default".equals(fname)) {
            return TypeDef.KEYWORD_PREFIX + fname;
        }
        return fname;
    }

    @SuppressWarnings({"unchecked"})
    <T,F> void decodeStructField(T o, Field f, TypeDef ftype, Class<F> fclass, Class<T> oclass) throws IOException, IllegalAccessException, InstantiationException {
        int count;
        switch (ftype.tag) {
        case BOOL_TAG:
            f.setBoolean(o, nextBoolean());
            break;
        case INT8_TAG:
            f.setByte(o, nextByte());
            break;

        case INT16_TAG:
            f.setShort(o, nextShort());
            break;
        case INT32_TAG:
            f.setInt(o, nextInt());
            break;
        case INT64_TAG:
            f.setLong(o, nextLong());
            break;
        case FLOAT32_TAG:
            f.setFloat(o, nextFloat());
            break;
        case FLOAT64_TAG:
            f.setDouble(o, nextDouble());
            break;
        case BYTES_TAG:
            f.set(o, nextByteArray());
            break;
        case STRING_TAG:
            f.set(o, nextString());
            break;
        case TIMESTAMP_TAG:
            f.set(o, nextTimestamp());
            break;
        case SYMBOL_TAG:
            f.set(o, nextSymbol());
            break;
        case UUID_TAG:
            f.set(o, nextUUID());
            break;
        case ARRAY_TAG:
            count = nextCount(ftype);
            if (java.util.List.class.isAssignableFrom(fclass)) {
                Class<?> iclass = (Class<?>) (((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0]);
                List list = (List)ArrayList.class.newInstance();
                for (int i=0; i<count; i++) {
                    decodeInit(ftype.items);
                    list.add(decode(ftype.items, iclass));
                }
                f.set(o, list);
            } else {
                throw new TBinException("Cannot instantiate array as target object of class " + fclass.getName());
            }
            break;
        case MAP_TAG:
            count = nextCount(ftype);
            if (java.util.Map.class.isAssignableFrom(fclass)) {
                Class<?> kclass = (Class<?>) (((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0]);
                Class<?> iclass = (Class<?>) (((ParameterizedType) f.getGenericType()).getActualTypeArguments()[1]);
                Map map = (Map)HashMap.class.newInstance();
                for (int i=0; i<count; i++) {
                    map.put(decode(ftype.keys, kclass), decode(ftype.items, iclass));
                }
                f.set(o, map);
            } else {
                throw new TBinException("Cannot instantiate array as target object of class " + fclass.getName());
            }
            break;
        case STRUCT_TAG:
            f.set(o, decodeStruct(ftype, fclass));
            break;
        case ANY_TAG:
            ftype = nextType(); //read the tag that ANY force.
            if (ftype.tag == ARRAY_TAG) {
                count = nextCount(ftype);
                if (java.util.List.class.isAssignableFrom(fclass)) {
                    Class<?> iclass = (Class<?>) (((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0]);
                    List list = (List)ArrayList.class.newInstance();
                    for (int i=0; i<count; i++) {
                        list.add(decode(ftype.items, iclass));
                    }
                    f.set(o, list);
                    break;
                } else {
                    throw new TBinException("Cannot instantiate array as target object of class " + fclass.getName());
                }
            }
            f.set(o, decode(ftype, fclass));
            break;
        default:
            panic("FIX THIS: " + ftype);
        }
    }

    public Object decode() throws IOException {
        return decode(nextType());
    }

    public Object decode(TypeDef type) throws IOException {
        int max;
        Struct struct;
        switch (type.tag) {
        case BOOL_TAG:
            return nextBoolean();
        case INT8_TAG:
            return nextByte();
        case INT16_TAG:
            return nextShort();
        case INT32_TAG:
            return nextInt();
        case INT64_TAG:
            return nextLong();
        case FLOAT32_TAG:
            return nextFloat();
        case FLOAT64_TAG:
            return nextDouble();
        case BYTES_TAG:
            return nextByteArray();
        case STRING_TAG:
            return nextString();
        case TIMESTAMP_TAG:
            return nextTimestamp();
        case SYMBOL_TAG:
            return nextSymbol();
        case UUID_TAG:
            return nextUUID();
        case ARRAY_TAG:
            max = nextCount(type);
            Array ary = new Array();
            for (int i = 0; i < max; i++) {
                TypeDef itemType = nextItemType(type, i);
                Object o = decode(itemType);
                ary.add(o);
            }
            return ary;
        case MAP_TAG:
            max = nextCount(type);
            HashMap<Object,Object> map = new HashMap<Object,Object>();
            for (int i = 0; i < max; i++) {
                TypeDef ktype = nextItemKeyType(type, i);
                Object k = decode(ktype);
                TypeDef itype = nextItemType(type, i);
                Object v = decode(itype);
                map.put(k, v);
            }
            return map;
        case STRUCT_TAG:
            max = nextCount(type);
            struct = new Struct(max);
            for (int i = 0; i < max; i++) {
                String fname = nextItemName(type, i);
                TypeDef ftype = nextItemType(type, i);
                Object o = decode(ftype);
                struct.append(fname, o);
            }
            return struct;
        case UNION_TAG:
            max = readNonNegativeInt();
            TypeDef utype = type.variants.get(max-1);
            switch (utype.tag) {
            case STRUCT_TAG:
                if (utype.fields != null) {
                    currentCount = utype.fields.size();
                } else {
                    currentCount = readNonNegativeInt();
                }
                break;
            default:
                throw new RuntimeException("HERE: not handled in union setup: " + utype);
            }
            return decode(utype);
        case ENUM_TAG:
            panic("fix enum here");
        case ANY_TAG:
            return decode(nextType());
        case NULL_TAG:
            return null;
        default:
            throw new TBinException("Unsupported type in TBin stream: " + type);
        }
    }

    // --------------------------------------

    // read a multi-byte unsigned integer. Each byte contains 7 bits of integer data, and the
    // top bit is used as a marker: the last byte has its top bit clear, all others have it set.
    // Thus, values of 0..127 are represented as a single byte.
    int readNonNegativeInt() throws IOException {
        int n = 0;
        int b;
        int shift = 0;
        do {
            b = in.read();
            if (b >= 0) {
                //println("[read " + hexByte(b) + "]");
                n |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    return n;
                }
            } else {
                throw new TBinException("unexpected end of stream");
            }
            shift += 7;
        } while (shift < 32);
        throw new TBinException("Invalid int encoding");
    }

    int readInt() throws IOException {
        int n = readNonNegativeInt();
        return (n >>> 1) ^ -(n & 1); // back to two's-complement
    }

    long readLong() throws IOException {
        long n = 0;
        int b;
        int shift = 0;
        do {
            b = in.read();
            if (b >= 0) {
                //println("[read " + hexByte(b) + "]");
                n |= (b & 0x7FL) << shift;
                if ((b & 0x80) == 0) {
                    return (n >>> 1) ^ -(n & 1); // back to two's-complement
                }
            } else {
                throw new TBinException("Cannot read long, unexpected end of data");
            }
            shift += 7;
        } while (shift < 64);
        throw new IOException("Invalid long encoding");
    }
    
    double readDouble() throws IOException {
        long bits = readLong();
        return Double.longBitsToDouble(bits);
    }

    byte []  readBytes(byte [] b) throws IOException {
        return readBytes(b, b.length);
    }

    byte [] readBytes(byte [] b, int count) throws IOException {
        int remaining = count, offset = 0;
        while (remaining > 0) {
            int i = in.read(b, offset, remaining);
            if (i < 0) {
                throw new TBinException("unexpected end of data");
            }
            remaining -= i;
            offset += i;
        }
        //println("[read " + hex(b, count) + "]");
        return b;
    }

    String utf8String(byte [] b, int len) throws IOException {
        return new String(b, 0, len, "UTF-8");
    }

    String readString() throws IOException {
        int n = readNonNegativeInt();
        return readString(n);
    }

    String readString(int n) throws IOException {
        byte [] b = (n < buf.length)? buf : new byte[n];
        return utf8String(readBytes(b, n), n);
    }

    Symbol readSymbol() throws IOException {
        int id = readNonNegativeInt();
        if (id == syms.size()) {
            //first time we've seen it, expect the name to follow
            String name = readString();
            syms.add(name);
            return Symbol.intern(name);
        }
        return Symbol.intern(syms.get(id));
    }

    private void decodeInit(TypeDef type) throws IOException {
        switch (type.tag) {
            case BYTES_TAG:
            case STRING_TAG:
            case ARRAY_TAG:
            case MAP_TAG:
                currentCount = readNonNegativeInt();
                break;
            case STRUCT_TAG:
                if (type.fields != null) {
                    currentCount = type.fields.size();
                } else {
                    currentCount = readNonNegativeInt();
                }
                break;
            case UUID_TAG:
                currentCount = 16;
                break;
        }
    }

    /**
     * The primary parser sequencing call. fetch and return the next tag.
     * @return the tag for the next item of input
     * @throws IOException on bad TBin stream or any underlying I/O error.
     */
    public TypeDef nextType() throws IOException {
        int tag = -1;
        TypeDef type = null;
        while (true) {
            while (tag < 0) {
                tag = readNonNegativeInt();
                if (dataVersion == 0) {
                    if ((tag & VERSION_TAG_MASK) == VERSION_TAG) {
                        dataVersion = (tag & VERSION_DATA_MASK) + 1;
                        if (dataVersion > CURRENT_VERSION)
                            throw new TBinException("TBin version not yet supported: " + dataVersion);
                        tag = -1;
                    } else {
                        throw new TBinException("unexpected tag in stream, cannot determine TBin version: " + tag);
                    }
                }
            }
            if ((tag & TINY_STR_TAG_MASK) == TINY_STR_TAG) {
                currentCount = tag & TINY_STR_DATA_MASK;
                return TypeDef.STRING;
            }
            switch (tag) {
            case NULL_TAG:
                return TypeDef.NULL;
            case BYTES_TAG:
                currentCount = readNonNegativeInt();
                return TypeDef.BYTES;
            case STRING_TAG:
                currentCount = readNonNegativeInt();
                return TypeDef.STRING;
            case ARRAY_TAG:
                currentCount = readNonNegativeInt();
                if (type == null) {
                    return TypeDef.ARRAY;
                }
                return type;
            case MAP_TAG:
                currentCount = readNonNegativeInt();
                if (type == null) {
                    return TypeDef.MAP;
                }
                return type;
            case STRUCT_TAG:
                currentCount = readNonNegativeInt();
                if (type == null) {
                    return TypeDef.STRUCT;
                }
                return type;
            case UUID_TAG:
                currentCount = 16;
                return TypeDef.UUID;
            case BOOL_TAG:
                return TypeDef.BOOL;
            case INT8_TAG:
                return TypeDef.INT8;
            case INT16_TAG:
                return TypeDef.INT16;
            case INT32_TAG:
                return TypeDef.INT32;
            case INT64_TAG:
                return TypeDef.INT64;
            case FLOAT32_TAG:
                return TypeDef.FLOAT32;
            case FLOAT64_TAG:
                return TypeDef.FLOAT64;
            case TIMESTAMP_TAG:
                return TypeDef.TIMESTAMP;
            case SYMBOL_TAG:
                return TypeDef.SYMBOL;
            default:
                int idx = tag-FIRST_USER_TAG;
                if (idx >= types.size()) {
                    decodeTypeDef(tag); //the tag was a definition. start over after we define it
                    tag = -1;
                    break;
                } else {
                    type = types.get(idx);
                    if (type.tag == STRUCT_TAG) {
                        if (type.fields != null) {
                            currentCount = type.fields.size();
                        } else {
                            currentCount = readNonNegativeInt();
                        }
                        return type;
                    } else if (type.tag == ARRAY_TAG || type.tag == MAP_TAG) {
                        currentCount = readNonNegativeInt();
                        return type;
                    } else {
                        throw new RuntimeException("NYI: user-defined types derived from tag " + hexByte(tag));
                    }
                }
            }
        }
    }

    public int nextCount(TypeDef type) {
        return currentCount;
    }

    //idx may be ignored, but if the type is a struct with a definition, it is used to index into its field defs.
    public String nextItemName(TypeDef type, int idx) throws IOException {
        switch (type.tag) {
        case STRUCT_TAG:
            if (type.fields != null) {
                return type.fields.get(idx).name;
            }
            return nextSymbol().name;
        default:
            panic("NOPE");
            return null;
        }
    }

    //idx may be ignored, but if the type is a struct with a definition, it is used to index into its field defs.
    public TypeDef nextItemKeyType(TypeDef type, int idx) throws IOException {
        switch (type.tag) {
        case MAP_TAG:
            if (type.keys != null) {
                return type.keys;
            }
            panic("no key type for a map?");
            return null;
        case STRUCT_TAG:
            return TypeDef.SYMBOL;
        default:
            panic("NOPE");
            return null;
        }
    }

    public TypeDef nextItemType(TypeDef type, int idx) throws IOException {
        TypeDef itype = null;
        switch (type.tag) {
        case ARRAY_TAG:
        case MAP_TAG:
            if (type.items != null && type.items != TypeDef.ANY) {
                itype = type.items;
            } else {
                return nextType();
            }
            break;
        case STRUCT_TAG:
            if (type.fields != null) {
                itype = type.fields.get(idx).type;
            } else {
                return nextType();
            }
            break;
        default:
            throw new TBinException("nextItemType called but type is not Array, Map, or Struct");
        }
        switch (itype.tag) {
        case BYTES_TAG:
        case STRING_TAG:
        case ARRAY_TAG:
        case MAP_TAG:
            currentCount = readNonNegativeInt();
            break;
        case UUID_TAG:
            currentCount = 16;
            break;
        case STRUCT_TAG:
            if (itype.fields != null) {
                currentCount = itype.fields.size();
            } else {
                currentCount = readNonNegativeInt();
            }
            break;
        }
        return itype;
    }

    void decodeTypeDef(int tag) throws IOException {
        int baseTag = readNonNegativeInt();
        int i, size;
        switch (baseTag) {
        case DEF_STRUCT_TAG:
            decodeStructTypeDef();
            break;
        case DEF_ARRAY_TAG:
            decodeArrayTypeDef();
            break;
        case DEF_MAP_TAG:
            decodeMapTypeDef();
            break;
        case DEF_ENUM_TAG:
            decodeEnumTypeDef();
            break;
        case DEF_UNION_TAG:
            decodeUnionTypeDef();
            break;
        default:
            panic("decodeTypeDef, baseTag: " + baseTag);
            throw new TBinException("Only struct-based typedefs are permitted: " + baseTag);
        }
    }

    void decodeStructTypeDef() throws IOException {
        int size = readNonNegativeInt();
        List<TypeDef.Field> fields = new ArrayList<TypeDef.Field>(size);
        for (int i=0; i<size; i++) {
            String fname = readString();
            TypeDef ftype = decodeType();
            fields.add(new TypeDef.Field(fname, ftype, ftype == TypeDef.ANY));
        }
        TypeDef type = TypeDef.forStruct(fields);
        types.add(type);
    }

    void decodeArrayTypeDef() throws IOException {
        TypeDef items = decodeType();
        TypeDef type = TypeDef.forArray(items);
        types.add(type);
    }

    void decodeMapTypeDef() throws IOException {
        TypeDef keys = decodeType();
        TypeDef items = decodeType();
        TypeDef type = TypeDef.forMap(keys, items);
        types.add(type);
    }

    void decodeEnumTypeDef() throws IOException {
        int size = readNonNegativeInt();
        List<String> syms = new ArrayList<>(size);
        syms.add("");
        for (int i=0; i<size; i++) {
            syms.add(readString());
        }
        TypeDef type = TypeDef.forEnum(syms);
        types.add(type);
    }

    void decodeUnionTypeDef() throws IOException {
        int size = readNonNegativeInt();
        List<TypeDef> variants = new ArrayList<>(size);
        for (int i=0; i<size; i++) {
            variants.add(decodeType());
        }
        TypeDef type = TypeDef.forUnion(variants);
        types.add(type);
    }

    TypeDef decodeType() throws IOException {
        int tag = readNonNegativeInt();
        if (tag >= FIRST_USER_TAG) {
            int idx = tag - FIRST_USER_TAG;
            if (idx >= types.size()) {
                throw new TBinException("ref to a undefined tag: 0x" + hexByte(tag));
            }
            return types.get(idx);
        }
        TypeDef result = TypeDef.forTag(tag);
        if (result == null) {
            throw new TBinException("ref to a undefined type: 0x" + hexByte(tag));
        }
        return result;
    }


    /**
     * @return the boolean value (Bool)
     * @throws IOException on bad TBin stream or any underlying I/O error.
     */
    public boolean nextBoolean() throws IOException {
        return readInt() != 0;
    }

    /**
     * @return the next byte (Int8)
     * @throws IOException on bad TBin stream or any underlying I/O error.
     */
    public byte nextByte() throws IOException {
        byte b = (byte)readInt();
        return b;
    }

    /**
     * @return the next short (Int16)
     * @throws IOException on bad TBin stream or any underlying I/O error.
     */
    public short nextShort() throws IOException {
        short n = (short)readInt();
        return n;
    }

    /**
     * @return the next int (Int32)
     * @throws IOException on bad TBin stream or any underlying I/O error.
     */
    public int nextInt() throws IOException {
        int n = readInt();
        return n;
    }

    /**
     * @return the next long (Int64)
     * @throws IOException on bad TBin stream or any underlying I/O error.
     */
    public long nextLong() throws IOException {
        return readLong();
    }

    /**
     * @return the next float (Float32)
     * @throws IOException on bad TBin stream or any underlying I/O error.
     */
    public float nextFloat() throws IOException {
        int bits = readInt();
        return Float.intBitsToFloat(bits);
    }

    /**
     * @return the next double (Float64)
     * @throws IOException on bad TBin stream or any underlying I/O error.
     */
    public double nextDouble() throws IOException {
        return readDouble();
    }

    /**
     * @return the next byte array
     * @throws IOException on bad TBin stream or any underlying I/O error.
     */
    public byte [] nextByteArray() throws IOException {
        return readBytes(new byte[currentCount], currentCount);
    }

    /**
     * @return the next String
     * @throws IOException on bad TBin stream or any underlying I/O error.
     */
    public String nextString() throws IOException {
        return readString(currentCount);
    }

    /**
     * @return the next Timestamp
     * @throws IOException on bad TBin stream or any underlying I/O error.
     */
    public Timestamp nextTimestamp() throws IOException {
        double d = readDouble();
        return Timestamp.fromMillis((long)(d * 1000.0));
    }

    /**
     * @return the next Symbol
     * @throws IOException on bad TBin stream or any underlying I/O error.
     */
    public Symbol nextSymbol() throws IOException {
        return readSymbol();
    }

    /**
     * @return the next UUID
     * @throws IOException on bad TBin stream or any underlying I/O error.
     */
    public UUID nextUUID() throws IOException {
        return UUID.fromBytes(readBytes(buf, currentCount));
    }


}
