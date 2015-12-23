/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.tbin;
import com.yahoo.rdl.*;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

/**
 * TBin encoding logic
 */
public class TBinEncoder extends TBin {
    OutputStream raw;
    private BufferedOutputStream out;
    private HashMap<String,Integer> syms; //maps name to id
    private HashMap<String,Integer> types; //maps TypeDef.signature to tag
    private int nextId = 0;
    private int nextTag = FIRST_USER_TAG;
    private int dataVersion = 0;
    private byte [] buf;

    public TBinEncoder(OutputStream out) throws IOException {
        this.raw = out;
        this.syms = new HashMap<String,Integer>(100);
        this.types = primitiveTypes();
        this.out = new BufferedOutputStream(this.raw);
        this.nextId = 0;
        this.nextTag = FIRST_USER_TAG;
        this.buf = new byte[256];
        emitNonNegativeInt(CUR_VERSION_TAG);
    }

    static HashMap<String,Integer> primitiveTypes() {
        HashMap<String,Integer> map = new HashMap<String,Integer>();
        map.put("Null", NULL_TAG);
        map.put("Bool", BOOL_TAG);
        map.put("Int8", INT8_TAG);
        map.put("Int16", INT16_TAG);
        map.put("Int32", INT32_TAG);
        map.put("Int64", INT64_TAG);
        map.put("Float32", FLOAT32_TAG);
        map.put("Float64", FLOAT64_TAG);
        map.put("Bytes", BYTES_TAG);
        map.put("String", STRING_TAG);
        map.put("Timestamp", TIMESTAMP_TAG);
        map.put("Symbol", SYMBOL_TAG);
        map.put("UUID", UUID_TAG);
        map.put("Array", ARRAY_TAG);
        map.put("Map", MAP_TAG);
        map.put("Struct", STRUCT_TAG);
        map.put("Any", ANY_TAG);
        return map;
    }

    public void encode(Object o) throws IOException {
        if (o == null) {
            encodeNull();
        } else if (o instanceof String) {
            encodeString((String)o);
        } else if (o instanceof java.lang.Boolean) {
            encodeBoolean((Boolean)o);
        } else if (o instanceof java.lang.Number) {
            if (o instanceof Integer) {
                encodeInteger((Integer)o);
            } else if (o instanceof Double) {
                encodeDouble((Double)o);
            } else if (o instanceof Long) {
                encodeLong((Long)o);
            } else if (o instanceof Byte) {
                encodeByte((Byte)o);
            } else if (o instanceof Short) {
                encodeShort((Short)o);
            } else if (o instanceof Float) {
                encodeFloat((Float)o);
            } else {
                throw new TBinException("Unsupported Number subtype: " + o.getClass().getName());
            }
        } else if (o instanceof Struct) {
            Struct s = (Struct)o;
            encodeStruct(s.size());
            for (Struct.Field f : s) {
                emitSymbol(f.name());
                encode(f.value());
            }
        } else if (o instanceof Map) {
            Map m = (Map)o;
            encodeMap(m.size());
            for (Object k : m.keySet()) {
                encode(k);
                encode(m.get(k));
            }
        } else if (o instanceof List) {
            List l = (List)o;
            encodeArray(l.size());
            for (Object v : l) {
                encode(v);
            }
        } else if (o instanceof Timestamp) {
            encodeTimestamp((Timestamp)o);
        } else if (o instanceof Symbol) {
            encodeSymbol(((Symbol)o).name);
        } else if (o instanceof UUID) {
            encodeUUID((UUID)o);
        } else {
            encodeObject(o);
        }
        out.flush();
    }

    void encodeObject(Object o) throws IOException {
        //to do: check if implements TBinMarshallable
        //  if so: call it instead
        //to do: check it the class has a static schema() method to get a description in abstract terms
        //  if so: call encodeObject(o, schema) instead
        //            throw new TBinException("Cannot encode type: " + o.getClass().getName());
        TypeDef type = TypeDef.NULL;
        if (o != null) {
            type = TypeDef.forClass(o.getClass()); //this uses reflection
        }
        int tag = encodeTypeDef(type); //this might do nothing
        emitNonNegativeInt(tag);        
        encodeTypedValue(o);
    }

    void encodeTypedValue(Object o) throws IOException {
        if (o == null) {
            throw new TBinException("Cannot encode a missing typed value");
        }
        if (o instanceof String) {
            emitString((String)o);
        } else if (o instanceof java.lang.Boolean) {
            emitBoolean((Boolean)o);
        } else if (o instanceof java.lang.Number) {
            if (o instanceof Integer) {
                emitInt((Integer)o);
            } else if (o instanceof Double) {
                emitDouble((Double)o);
            } else if (o instanceof Long) {
                emitLong((Long)o);
            } else if (o instanceof Byte) {
                emitInt((Byte)o);
            } else if (o instanceof Short) {
                emitInt((Short)o);
            } else if (o instanceof Float) {
                emitFloat((Float)o);
            } else {
                throw new TBinException("Unsupported Number subtype: " + o.getClass().getName());
            }
        } else if (o instanceof Struct) {
            Struct s = (Struct)o;
            encodeStruct(s.size());
            for (Struct.Field f : s) {
                emitSymbol(f.name());
                encode(f.value());
            }
        } else if (o instanceof Map) {
            Map m = (Map)o;
            encodeMap(m.size());
            for (Object k : m.keySet()) {
                encode(k);
                encode(m.get(k));
            }
        } else if (o instanceof List) {
            List l = (List)o;
            encodeArray(l.size());
            for (Object v : l) {
                encode(v);
            }
        } else if (o instanceof Timestamp) {
            emitTimestamp((Timestamp)o);
        } else if (o instanceof Symbol) {
            encodeSymbol(((Symbol)o).name);
        } else if (o instanceof UUID) {
            encodeUUID((UUID)o);
        } else {
            Class<?> cl = o.getClass();
            try {
                int uvariant = 0;
                Field [] flds = cl.getFields();
                for (int fldnum = 0; fldnum < flds.length; fldnum++) {
                    Field f = flds[fldnum];
                    boolean optional = false;
                    for (java.lang.annotation.Annotation anno : f.getDeclaredAnnotations()) {
                        if (anno instanceof RdlOptional) {
                            optional = true;
                        }
                        //if (anno instanceof RdlUnionTag) {
                        if ("variant".equals(f.getName()) && (anno instanceof com.fasterxml.jackson.annotation.JsonIgnore)) {
                            uvariant = ((Enum)f.get(o)).ordinal() + 1;
                            if (uvariant > 0) {
                                emitNonNegativeInt(uvariant);
                                continue;
                            }
                        }

                    }
                    if (uvariant != 0) {
                        if (fldnum != uvariant) {
                            continue;
                        }
                    }
                    int modifiers = f.getModifiers();
                    if ((modifiers & java.lang.reflect.Modifier.PUBLIC) != 0 &&
                        (modifiers & java.lang.reflect.Modifier.STATIC) == 0 &&
                        (modifiers & java.lang.reflect.Modifier.TRANSIENT) == 0) {
                        TypeDef ftype = TypeDef.ANY;
                        Class<?> fclass = f.getType();
                        if (java.util.List.class.isAssignableFrom(fclass)) {
                            Class<?> iclass = (Class<?>) (((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0]);                            
                            java.util.List lst = (java.util.List)f.get(o);
                            if (optional) {
                                if (lst == null) {
                                    encodeNull();
                                    continue;
                                } else {
                                    TypeDef items = TypeDef.forClass(iclass);
                                    String sig = TypeDef.forArray(items).signature;
                                    emitNonNegativeInt(types.get(sig));
                                }
                            }
                            emitNonNegativeInt(lst.size());
                            int n = 0;
                            for (Object item : lst) {
                                n++;
                                encodeTypedValue(item);
                            }
                        } else if (java.util.Map.class.isAssignableFrom(fclass)) {
                            Class<?> kclass = (Class<?>) (((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0]);
                            Class<?> iclass = (Class<?>) (((ParameterizedType) f.getGenericType()).getActualTypeArguments()[1]);
                            java.util.Map map = (java.util.Map)f.get(o);
                            if (optional) {
                                if (map == null) {
                                    encodeNull();
                                    continue;
                                } else {
                                    TypeDef keys = TypeDef.forClass(kclass);
                                    TypeDef items = TypeDef.forClass(iclass);
                                    String sig = TypeDef.forMap(keys, items).signature;
                                    emitNonNegativeInt(types.get(sig));
                                }
                            }
                            emitNonNegativeInt(map.size());
                            for (Object key : map.keySet()) {
                                encodeTypedValue(key);
                                encodeTypedValue(map.get(key));
                            }
                        } else {
                            Object v = f.get(o);
                            if (optional && uvariant == 0) {
                                encode(v);
                            } else {
                                encodeTypedValue(v);
                            }
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new TBinException("Cannot encode object of class " + cl.getName());
            }
        }
        out.flush();
    }
    
    void emitTypeDef(TypeDef td, int tag) throws IOException {
        emitNonNegativeInt(tag);
        switch (td.tag) {
        case STRUCT_TAG:
            emitNonNegativeInt(DEF_STRUCT_TAG);
            emitNonNegativeInt(td.fields.size());
            for (TypeDef.Field f : td.fields) {
                emitString(f.name);
                if (f.optional) {
                    emitNonNegativeInt(ANY_TAG);
                } else {
                    emitNonNegativeInt(types.get(f.type.signature));
                }
            }
            break;
        case ARRAY_TAG:
            emitNonNegativeInt(DEF_ARRAY_TAG);
            emitNonNegativeInt(types.get(td.items.signature));
            break;
        case MAP_TAG:
            emitNonNegativeInt(DEF_MAP_TAG);
            emitNonNegativeInt(types.get(td.keys.signature));
            emitNonNegativeInt(types.get(td.items.signature));
            break;
        case UNION_TAG:
            emitNonNegativeInt(DEF_UNION_TAG);
            emitNonNegativeInt(td.variants.size()); //1 more than the actual number of variants?
            for (TypeDef vtd : td.variants) {
                emitNonNegativeInt(types.get(vtd.signature));
            }
            break;
        case ENUM_TAG:
            emitNonNegativeInt(DEF_ENUM_TAG);
            emitNonNegativeInt(td.symbols.size());
            for (String sym : td.symbols) {
                emitString(sym);
            }
            break;
        }
    }

    int encodeTypeDef(TypeDef td) throws IOException {
        // only emit if not defined already. This excludes all the RDL base types, but includes
        // typedefs for typed arrays/maps, as well as objects
        int tag = td.tag;
        String sig = td.signature;
        if (types.containsKey(sig)) {
            tag = types.get(sig);
        } else {
            switch (td.tag) {
            case STRUCT_TAG:
                if (td.fields != null) {
                    for (TypeDef.Field field : td.fields) {
                        encodeTypeDef(field.type);
                    }
                    tag = nextTag++;
                    types.put(sig, tag);
                    emitTypeDef(td, tag);
                }
                break;
            case ARRAY_TAG:
                if (td.items != TypeDef.ANY) {
                    encodeTypeDef(td.items);
                    tag = nextTag++;
                    types.put(sig, tag);
                    emitTypeDef(td, tag);
                }
                break;
            case UNION_TAG:
                for (TypeDef vtd : td.variants) {
                    encodeTypeDef(vtd);
                }
                tag = nextTag++;
                types.put(sig, tag);
                emitTypeDef(td, tag);
                break;
            case MAP_TAG:
                if (td.keys != TypeDef.ANY || td.items != TypeDef.ANY) {
                    encodeTypeDef(td.keys);
                    encodeTypeDef(td.items);
                    tag = nextTag++;
                    types.put(sig, tag);
                    emitTypeDef(td, tag);
                }
                break;
            case ENUM_TAG:
                tag = nextTag++;
                types.put(sig, tag);
                emitTypeDef(td, tag);
                break;
            default:
                throw new TBinException("Cannot create typedef for this kind of object: " + td);
            }
        }
        return tag;
    }

    public void encodeNull() throws IOException {
        emitNonNegativeInt(NULL_TAG);
    }

    public void encodeBoolean(boolean b) throws IOException {
        emitNonNegativeInt(BOOL_TAG);
        emitBoolean(b);
    }

    public void encodeByte(byte n) throws IOException {
        emitNonNegativeInt(INT8_TAG);
        emitInt(n);
    }

    public void encodeShort(short n) throws IOException {
        emitNonNegativeInt(INT16_TAG);
        emitInt(n);
    }

    public void encodeInteger(int n) throws IOException {
        emitNonNegativeInt(INT32_TAG);
        emitInt(n);
    }

    public void encodeLong(long n) throws IOException {
        emitNonNegativeInt(INT64_TAG);
        emitLong(n);
    }

    public void encodeFloat(float n) throws IOException {
        emitNonNegativeInt(FLOAT32_TAG);
        emitFloat(n);
    }

    public void encodeDouble(double n) throws IOException {
        emitNonNegativeInt(FLOAT64_TAG);
        emitDouble(n);
    }

    public void encodeBytes(byte [] b) throws IOException {
        emitNonNegativeInt(BYTES_TAG);
        emitBytes(b);
    }

    public void encodeString(String s) throws IOException {
        byte [] utf8 = utf8Bytes(s);
        int utflen = utf8.length;
        if (utflen <= TINY_STR_MAXLEN) {
            emitNonNegativeInt(TINY_STR_TAG + utflen);
        } else {
            emitNonNegativeInt(STRING_TAG);
            emitNonNegativeInt(utflen);
        }
        //println(hex(utf8, utflen)); //debug
        out.write(utf8, 0, utflen);
    }

    public void encodeTimestamp(Timestamp ts) throws IOException {
        emitNonNegativeInt(TIMESTAMP_TAG);
        emitTimestamp(ts);
    }

    void emitTimestamp(Timestamp ts) throws IOException {
        double secondsSinceEpoch = (double)ts.millis() / 1000.0;
        emitDouble(secondsSinceEpoch);
    }

    public void encodeSymbol(String s) throws IOException {
        emitNonNegativeInt(SYMBOL_TAG);
        emitSymbol(s);
    }

    public void encodeUUID(UUID uuid) throws IOException {
        emitNonNegativeInt(UUID_TAG);
        emitUUID(uuid);
    }

    void emitUUID(UUID uuid) throws IOException {
        emitBytes(uuid.toBytes());
    }

    public void encodeArray(int count) throws IOException {
        //int tag = nextDatumTag(ARRAY_TAG, count); //might return -1 if no tag is to be emitted
        //if (tag >= 0) {
        //    emitNonNegativeInt(tag);
        //} //else the type already knows what it is
        emitNonNegativeInt(ARRAY_TAG);
        emitNonNegativeInt(count);
    }

    public void encodeMap(int count) throws IOException {
        //non-typed version
        emitNonNegativeInt(MAP_TAG);
        emitNonNegativeInt(count);
    }

    public void encodeStruct(int count) throws IOException {
        //non-typed version
        emitNonNegativeInt(STRUCT_TAG);
        emitNonNegativeInt(count);
    }

    public void encodeField(String name) throws IOException {
        emitSymbol(name);
    }


    private void emitBoolean(boolean b) throws IOException {
        emitNonNegativeInt(b? 1 : 0);
    }

    private void emitInt(int n) throws IOException {
        emitNonNegativeInt((n << 1) ^ (n >> 31));
    }

    private void emitNonNegativeInt(int n) throws IOException {
        int nn = n;
        int len = 0;
        if ((n & ~0x7f) != 0) {
            buf[len++] = (byte)((n | 0x80) & 0xff);
            n >>>= 7;
            if (n > 0x7F) {
                buf[len++] = (byte)((n | 0x80) & 0xff);
                n >>>= 7;
                if (n > 0x7F) {
                    buf[len++] = (byte)((n | 0x80) & 0xff);
                    n >>>= 7;
                    if (n > 0x7F) {
                        buf[len++] = (byte)((n | 0x80) & 0xff);
                        n >>>= 7;
                    }
                }
            }
        }
        buf[len++] = (byte)n;
        //println(hex(buf, len)); //debug
        out.write(buf, 0, len);
    }

    private void emitLong(long n) throws IOException {
        n = (n << 1) ^ (n >> 63);
        int len = 0;
        if ((n & ~0x7fL) != 0) {
            buf[len++] = (byte)((n | 0x80) & 0xff);
            n >>>= 7;
            if (n > 0x7f) {
                buf[len++] = (byte)((n | 0x80) & 0xff);
                n >>>= 7;
                if (n > 0x7f) {
                    buf[len++] = (byte)((n | 0x80) & 0xff);
                    n >>>= 7;
                    if (n > 0x7f) {
                        buf[len++] = (byte)((n | 0x80) & 0xff);
                        n >>>= 7;
                        if (n > 0x7f) {
                            buf[len++] = (byte)((n | 0x80) & 0xff);
                            n >>>= 7;
                            if (n > 0x7f) {
                                buf[len++] = (byte)((n | 0x80) & 0xff);
                                n >>>= 7;
                                if (n > 0x7f) {
                                    buf[len++] = (byte)((n | 0x80) & 0xff);
                                    n >>>= 7;
                                    if (n > 0x7f) {
                                        buf[len++] = (byte)((n | 0x80) & 0xff);
                                        n >>>= 7;
                                        if (n > 0x7f) {
                                            buf[len++] = (byte)((n | 0x80) & 0xff);
                                            n >>>= 7;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        buf[len++] = (byte)n;
        //println(hex(buf, len)); //debug
        out.write(buf, 0, len);
    }

    private void emitFloat(float n) throws IOException {
        int bits = Float.floatToIntBits(n);
        buf[0] = (byte)(bits >> 24);
        buf[1] = (byte)(bits >> 16);
        buf[2] = (byte)(bits >> 8);
        buf[3] = (byte)(bits);
        //println(hex(buf, 4)); //debug
        out.write(buf, 0, 4);
    }

    private void emitDouble(double n) throws IOException {
        long bits = Double.doubleToLongBits(n);
        buf[0] = (byte)(bits >> 56);
        buf[1] = (byte)(bits >> 48);
        buf[2] = (byte)(bits >> 40);
        buf[3] = (byte)(bits >> 32);
        buf[4] = (byte)(bits >> 24);
        buf[5] = (byte)(bits >> 16);
        buf[6] = (byte)(bits >> 8);
        buf[7] = (byte)(bits);
        //println(hex(buf, 8)); //debug
        out.write(buf, 0, 8);
    }

    private byte [] utf8Bytes(String s) throws IOException {
        return s.getBytes("UTF-8");
    }

    private void emitString(String s) throws IOException {
        byte [] utf8 = utf8Bytes(s);
        int utflen = utf8.length;
        emitNonNegativeInt(utflen);
        //println(hex(utf8, utflen)); //debug
        out.write(utf8, 0, utflen);
    }

    private void emitBytes(byte [] b) throws IOException {
        emitBytes(b, b.length);
    }

    private void emitBytes(byte [] b, int n) throws IOException {
        if (n > 0) {
            //println(hex(b, n)); //debug
            out.write(b, 0, n);
        }
    }

    private void emitSymbol(String name) throws IOException {
        Integer id = syms.get(name);
        if (id == null) {
            id = nextId++;
            syms.put(name, id);
            emitNonNegativeInt(id.intValue());
            emitString(name);
        } else {
            emitNonNegativeInt(id.intValue()); //use actual stream state to know if it has been defined already. No limit.
        }
    }

}
