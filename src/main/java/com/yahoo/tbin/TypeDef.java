/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.tbin;
import com.yahoo.rdl.*;
import static com.yahoo.tbin.TBin.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.*;

/**
 * TBin-specific type tags
 */
public class TypeDef {
    public int tag;
    public List<Field> fields;
    public TypeDef items;
    public TypeDef keys;
    public List<TypeDef> variants;
    public List<String> symbols;

    String signature; //lazily created, used for key in hashmap
    
    static class Field {
        String name;
        TypeDef type;
        boolean optional;
        Field(String name, TypeDef type, boolean opt) {
            this.name = name;
            this.type = type;
            this.optional = opt;
        }
        public String toString() {
            return "<Field " + name + " " + type + " " + optional + ">";
        }
    }

    static TypeDef ANY = forBaseType(ANY_TAG);
    static TypeDef NULL = forBaseType(NULL_TAG);
    static TypeDef BOOL = forBaseType(BOOL_TAG);
    static TypeDef INT8 = forBaseType(INT8_TAG);
    static TypeDef INT16 = forBaseType(INT16_TAG);
    static TypeDef INT32 = forBaseType(INT32_TAG);
    static TypeDef INT64 = forBaseType(INT64_TAG);
    static TypeDef FLOAT32 = forBaseType(FLOAT32_TAG);
    static TypeDef FLOAT64 = forBaseType(FLOAT64_TAG);
    static TypeDef BYTES = forBaseType(BYTES_TAG);
    static TypeDef STRING = forBaseType(STRING_TAG);
    static TypeDef TIMESTAMP = forBaseType(TIMESTAMP_TAG);
    static TypeDef SYMBOL = forBaseType(SYMBOL_TAG);
    static TypeDef UUID = forBaseType(UUID_TAG);

    static TypeDef ARRAY = forBaseType(ARRAY_TAG);
    static TypeDef MAP = forBaseType(MAP_TAG);
    static TypeDef STRUCT = forBaseType(STRUCT_TAG);

    //no base enum or union typedefs, they must come from the stream



    public String toString() {
        return signature;
    }

    //    TypeDef(int tag) {
    //        this.tag = tag;
    //        this.signature = initSignature();
    //    }

    private TypeDef() {
    }

    static TypeDef forBaseType(int tag) {
        TypeDef td = new TypeDef();
        td.tag = tag;
        return td.initSignature();
    }

    static TypeDef forStruct(List<Field> fields) {
        TypeDef td = new TypeDef();
        td.tag = TBin.STRUCT_TAG;
        td.fields = fields;
        return td.initSignature();
    }
    static TypeDef forArray(TypeDef items) {
        TypeDef td = new TypeDef();
        td.tag = TBin.ARRAY_TAG;
        td.items = items;
        return td.initSignature();
    }
    static TypeDef forMap(TypeDef keys, TypeDef items) {
        TypeDef td = new TypeDef();
        td.tag = TBin.MAP_TAG;
        td.keys = keys;
        td.items = items;
        return td.initSignature();
    }
    static TypeDef forEnum(List<String> syms) {
        TypeDef td = new TypeDef();
        td.tag = TBin.ENUM_TAG;
        td.symbols = syms;
        return td.initSignature();
    }
    static TypeDef forUnion(List<TypeDef> variants) {
        TypeDef td = new TypeDef();
        td.tag = TBin.UNION_TAG;
        td.variants = variants;
        return td.initSignature();
    }

    String tagName(int tag) {
        if ((tag & TINY_STR_TAG_MASK) == TINY_STR_TAG) {
            return "String";
        }
        switch (tag) {
        case NULL_TAG:
            return "Null";
        case BOOL_TAG:
            return "Bool";
        case INT8_TAG:
            return "Int8";
        case INT16_TAG:
            return "Int16";
        case INT32_TAG:
            return "Int32";
        case INT64_TAG:
            return "Int64";
        case FLOAT32_TAG:
            return "Float32";
        case FLOAT64_TAG:
            return "Float64";
        case BYTES_TAG:
            return "Bytes";
        case STRING_TAG:
            return "String";
        case TIMESTAMP_TAG:
            return "Timestamp";
        case SYMBOL_TAG:
            return "Symbol";
        case UUID_TAG:
            return "UUID";
        case ARRAY_TAG:
            return "Array";
        case MAP_TAG:
            return "Map";
        case STRUCT_TAG:
            return "Struct";
        case ANY_TAG:
            return "Any";
        case ENUM_TAG:
            return "Enum";
        default:
            return "0x" + TBin.hexByte(tag);
        }
    }

    static TypeDef forTag(int tag) {
        switch (tag) {
        case NULL_TAG:
            return TypeDef.NULL;
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
        case BYTES_TAG:
            return TypeDef.BYTES;
        case STRING_TAG:
            return TypeDef.STRING;
        case TIMESTAMP_TAG:
            return TypeDef.TIMESTAMP;
        case SYMBOL_TAG:
            return TypeDef.SYMBOL;
        case UUID_TAG:
            return TypeDef.UUID;
        case ANY_TAG:
            return TypeDef.ANY;
        default:
            return null;
        }
    }

    private TypeDef initSignature() {
        String s, sitems, skeys;
        int i;
        switch (tag) {
        case STRUCT_TAG:
            if (fields == null) {
                signature = tagName(tag); //naked struct, implies map[rdl.Symbol]rdl.Any
                return this;
            } else {
                s = tagName(tag) + "{";
                i = 0;
                for (Field f : fields) {
                    if (i++ > 0) {
                        s += ",";
                    }
                    s += f.name;
                    s += ":";
                    s += f.type.toString();
                }
                signature = s + "}";
                return this;
            }
        case ARRAY_TAG:
            if (items == null) {
                items = ANY;
            }
            signature = tagName(tag) + "<" + items.toString() + ">";
            return this;
        case MAP_TAG:
            if (keys == null) {
                keys = ANY;
            }
            if (items == null) {
                items = ANY;

            }
            signature = tagName(tag) + "<" + keys.toString() + "," + items.toString() + ">";
            return this;
        case ANY_TAG:
            signature = "Any";
            return this;
        case UNION_TAG:
            s = "Union<";
            i = 0;
            for (TypeDef t : variants) {
                if (i++ > 0) {
                    s += ",";
                }
                s += t.toString();
            }
            signature = s + ">";
            return this;
        case ENUM_TAG:
            s = "Enum<";
            i = 0;
            for (String t : symbols) {
                if (i > 0) {
                    if (i > 1) {
                        s += ",";
                    }
                    s += t;
                }
                i++;
            }
            signature = s + ">";
            return this;
        default:
            signature = tagName(tag);
            return this;
        }
    }

    private static Map<String,TypeDef> cache = initCache();

    private static Map<String,TypeDef> initCache() {
        HashMap<String,TypeDef> map = new HashMap<String,TypeDef>();
        map.put("java.lang.Boolean", BOOL);
        map.put("boolean", BOOL);
        map.put("java.lang.Byte", INT8);
        map.put("java.lang.Short", INT16);
        map.put("java.lang.Integer", INT32);
        map.put("int", INT32);
        map.put("java.lang.Long", INT64);
        map.put("java.lang.Float", FLOAT32);
        map.put("java.lang.Double", FLOAT64);
        map.put("[byte", BYTES);
        map.put("java.lang.String", STRING);
        map.put("com.yahoo.rdl.Timestamp", TIMESTAMP);
        map.put("com.yahoo.rdl.Symbol", SYMBOL);
        map.put("com.yahoo.rdl.UUID", UUID);
        map.put("java.lang.Object", ANY);
        return map;
    }

    static final String KEYWORD_PREFIX = "_"; //i.e. if a field is "default" in RDL, this prefix is used in Java

    static <T> List<String> enumSymbols(Class<T> e) {
        ArrayList<String> lst = new ArrayList<>();
        for (T item : e.getEnumConstants()) {
            lst.add(String.valueOf(item));
        }
        return lst;
    }

    public static TypeDef forClass(Class<?> cl) throws TBinException {
        String clName = cl.getName();
        TypeDef td = cache.get(clName);
        if (td == null) {
            if (java.util.List.class.isAssignableFrom(cl)) {
                //due to type erasure, we don't know the item type, so items will have to be tagged
                return ARRAY;
            } else if (cl.isEnum()) {
                List<String> syms = enumSymbols(cl);
                return TypeDef.forEnum(syms);
            }
            List<TypeDef.Field> fields = new ArrayList<TypeDef.Field>();
            List<TypeDef> variants = null;
            java.lang.reflect.Field [] flds = cl.getFields();
            for (int fldnum = 0; fldnum < flds.length; fldnum++) {
                java.lang.reflect.Field f = flds[fldnum];
                boolean optional = false;
                for (java.lang.annotation.Annotation anno : f.getDeclaredAnnotations()) {
                    if (anno instanceof RdlOptional) {
                        optional = true;
                    } else {
                        //if (anno instanceof RdlUnionTag) {
                        if ("variant".equals(f.getName()) && (anno instanceof com.fasterxml.jackson.annotation.JsonIgnore)) {
                            variants = new ArrayList<TypeDef>();
                            //System.out.println("---------- Union Class: " + cl.getName());
                        }
                    }
                }
                if (variants != null) {
                    if (fldnum == 0) {
                        continue;
                    }
                    //System.out.println(fldnum + "\tFIELD: " + f.getName());
                }
                int modifiers = f.getModifiers();
                if ((modifiers & java.lang.reflect.Modifier.PUBLIC) != 0 &&
                    (modifiers & java.lang.reflect.Modifier.STATIC) == 0 &&
                    (modifiers & java.lang.reflect.Modifier.TRANSIENT) == 0) {
                    String fname = f.getName();
                    if (fname.startsWith(KEYWORD_PREFIX)) {
                        fname = fname.substring(1);
                    }
                    TypeDef ftype = ANY;
                    Class<?> fclass = f.getType();
                    if (fclass != Object.class) {
                        if (java.util.List.class.isAssignableFrom(fclass)) {
                            Class<?> iclass = (Class<?>) (((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0]);
                            TypeDef fitems = TypeDef.forClass(iclass);
                            if (fitems == null) {
                                fitems = ANY;
                            }
                            ftype = TypeDef.forArray(fitems);
                        } else if (java.util.Map.class.isAssignableFrom(fclass)) {
                            Class<?> kclass = (Class<?>) (((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0]);
                            Class<?> iclass = (Class<?>) (((ParameterizedType) f.getGenericType()).getActualTypeArguments()[1]);
                            TypeDef fkeys = TypeDef.forClass(kclass);
                            if (fkeys == null) {
                                fkeys = ANY;
                            }
                            TypeDef fitems = TypeDef.forClass(iclass);
                            if (fitems == null) {
                                fitems = ANY;
                            }
                            ftype = TypeDef.forMap(fkeys, fitems);
                        } else {
                            ftype = TypeDef.forClass(fclass);
                            if (ftype == null) {
                                throw new TBinException("Cannot map class to TBin: " + fclass.getName());
                            }
                        }
                    }
                    if (variants != null) {
                        //System.out.println(fldnum + "\t  -> included as " + ftype);
                        variants.add(ftype);
                    } else {
                        fields.add(new TypeDef.Field(fname, ftype, optional));
                    }
                }
            }
            if (variants != null) {
                td = TypeDef.forUnion(variants);
                //System.out.println("--> " + td);
            } else {
                if (fields.size() == 0) {
                    throw new RuntimeException("Empty struct: " + cl.getName());
                }
                td = TypeDef.forStruct(fields);
            }
        }
        return td;
    }

}
