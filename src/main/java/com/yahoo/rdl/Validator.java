/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.rdl;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Schema validation logic.
 */
public class Validator {

    public static class Result {
        public final boolean valid;
        public final String error;
        Result(boolean v, String e) {
            this.valid = v;
            this.error = e;
        }
        public String toString() {
            if (valid) {
                return "<Validation OK>";
            } else {
                return "<Validation error: " + error + ">";
            }
        }
    }

    static Result valid() {
        return new Result(true, null);
    }

    static Result error(String context, String msg) {
        if (context.length() > 0) {
            msg = msg + " in " + context;
        }
        return new Result(false, msg);
    }

    Schema schema;
    HashMap<String,Type> types;
    public Validator(Schema schema) {
        this.schema = schema;
        this.types = new HashMap<String,Type>();
        this.types.put("Bool", new Type(BaseType.Bool));
        this.types.put("Int8", new Type(BaseType.Int8));
        this.types.put("Int16", new Type(BaseType.Int16));
        this.types.put("Int32", new Type(BaseType.Int32));
        this.types.put("Int64", new Type(BaseType.Int64));
        this.types.put("Float32", new Type(BaseType.Float32));
        this.types.put("Float64", new Type(BaseType.Float64));
        this.types.put("Bytes", new Type(BaseType.Bytes));
        this.types.put("String", new Type(BaseType.String));
        this.types.put("Timestamp", new Type(BaseType.Timestamp));
        this.types.put("UUID", new Type(BaseType.UUID));
        this.types.put("Array", new Type(BaseType.Array));
        this.types.put("Map", new Type(BaseType.Map));
        this.types.put("Struct", new Type(BaseType.Struct));
        this.types.put("Enum", new Type(BaseType.Enum));
        this.types.put("Union", new Type(BaseType.Union));
        this.types.put("Any", new Type(BaseType.Any));
        for (Type t : schema.types) {
            String name = null;
            switch (t.variant) {
            case BaseType:
                name = String.valueOf(t.BaseType);
                break;
            case StructTypeDef:
                name = t.StructTypeDef.name;
                break;
            case MapTypeDef:
                name = t.MapTypeDef.name;
                break;
            case ArrayTypeDef:
                name = t.ArrayTypeDef.name;
                break;
            case EnumTypeDef:
                name = t.EnumTypeDef.name;
                break;
            case UnionTypeDef:
                name = t.UnionTypeDef.name;
                break;
            case StringTypeDef:
                name = t.StringTypeDef.name;
                break;
            case BytesTypeDef:
                name = t.BytesTypeDef.name;
                break;
            case NumberTypeDef:
                name = t.NumberTypeDef.name;
                break;
            case AliasTypeDef:
                name = t.AliasTypeDef.name;
                break;
            }
            this.types.put(name, t);
        }
    }

    Type type(String name) {
        return types.get(name);
    }

    public Result validate(Object data, String typename) {
        return validate(data, typename, typename, "data");
    }

    public Result validate(Object data, String typename, String alias, String context) {
        Type t = type(typename);
        if (t == null) {
            return error("", "No such type: " + typename);
        }
        return validate(data, t, alias, context);
    }

    public Result validate(Object data, Type t, String alias, String context) {
        switch (t.variant) {
        case BaseType:
            return validateBaseType(data, t.BaseType, alias, context);
        case StructTypeDef:
            return validateStructType(data, t.StructTypeDef, context);
        case MapTypeDef:
            return validateMapType(data, t.MapTypeDef, context);
        case ArrayTypeDef:
            return validateArrayType(data, t.ArrayTypeDef, context);
        case EnumTypeDef:
            return validateEnumType(data, t.EnumTypeDef, context);
        case UnionTypeDef:
            return validateUnionType(data, t.UnionTypeDef, context);
        case StringTypeDef:
            return validateStringType(data, t.StringTypeDef, context);
        case BytesTypeDef:
            break; //fix me
        case NumberTypeDef:
            return validateNumberType(data, t.NumberTypeDef, context);
        case AliasTypeDef:
            return validate(data, t.AliasTypeDef.type, t.AliasTypeDef.name, context);
        }
        return error(context, "NYI: " + t.variant);
    }

    Type fieldType(String type, String items, String keys) {
        if ("Array".equalsIgnoreCase(type)) {
            ArrayTypeDef atype = new ArrayTypeDef().type(type);
            if (items != null) {
                atype.items(items);
            }
            return new Type(atype);
        }
        return type(type);
    }

    @SuppressWarnings({"unchecked" })
    Result validateMapType(Object data, MapTypeDef typedef, String context) {
        if (data instanceof Map) {
            Map<Object,Object> map = (Map<Object,Object>)data;
            int size = map.size();
            if (typedef.size != null) {
                if (size != typedef.size) {
                    return error(context, "Bad map size for type " + typedef.name + ", expected " + typedef.size + ", got " + size);
                }
            }
            if (typedef.minSize != null) {
                if (size < typedef.minSize) {
                    return error(context, "Bad map size for type " + typedef.name + ", expected no smaller than " + typedef.minSize + ", got " + size);
                }
            }
            if (typedef.maxSize != null) {
                if (size < typedef.maxSize) {
                    return error(context, "Bad map size for type " + typedef.name + ", expected no larger than " + typedef.maxSize + ", got " + size);
                }
            }
            Type kt = type(typedef.keys);
            if (kt == null) {
                return error("", "No such type: " + typedef.keys);
            }
            Type it = type(typedef.items);
            if (it == null) {
                return error("", "No such type: " + typedef.items);
            }
            int i = 0;
            //no reflection help here, although a struct field could get the item type from a generic.
            //this is dumb and slow, if the array is homogenous. If heterogeneous, not much choice.
            for (Map.Entry<Object,Object> e : map.entrySet()) {
                Result tmp = validate(e.getKey(), kt, typedef.keys, context + "[key]value");
                if (!tmp.valid) {
                    return tmp;
                }
                tmp = validate(e.getValue(), it, typedef.items, context + "[" + i + "]");
                if (!tmp.valid) {
                    return tmp;
                }
            }
        }
        return valid();
    }
    
    Result validateArrayType(Object data, ArrayTypeDef typedef, String context) {
        if (data instanceof List) {
            List lst = (List)data;
            if (typedef.size != null) {
                if (lst.size() != typedef.size) {
                    return error(context, "Bad array size for type " + typedef.name + ", expected " + typedef.size + ", got " + lst.size());
                }
            }
            if (typedef.minSize != null) {
                if (lst.size() < typedef.minSize) {
                    return error(context, "Bad array size for type " + typedef.name + ", expected no smaller than " + typedef.minSize + ", got " + lst.size());
                }
            }
            if (typedef.maxSize != null) {
                if (lst.size() < typedef.maxSize) {
                    return error(context, "Bad array size for type " + typedef.name + ", expected no larger than " + typedef.maxSize + ", got " + lst.size());
                }
            }
            if (typedef.items != null) {
                Type it = type(typedef.items);
                if (it == null) {
                    return error("", "No such type: " + typedef.items);
                }
                int i = 0;
                //no reflection help here, although a struct field could get the item type from a generic.
                //this is dumb and slow, if the array is homogenous. If heterogeneous, not much choice.
                for (Object item : lst) {
                    Result tmp = validate(item, it, typedef.items, context + "[" + i + "]");
                    if (!tmp.valid) {
                        return tmp;
                    }
                }
            }
        }
        return valid();
    }
    
    Result validateStructType(Object data, StructTypeDef typedef, String context) {
        if (data instanceof Map) {
            Map map = (Map)data;
            List<StructFieldDef> fields = new ArrayList<StructFieldDef>();
            flattenFields(typedef, fields);
            for (StructFieldDef field : fields) {
                if (!map.containsKey(field.name)) {
                    if (!field.optional && field._default == null) {
                        return error(context, "Missing required field '" + field.name + "' for type " + typedef.name);
                    }
                } else {
                    Type ftype = fieldType(field.type, field.items, field.keys);
                    Result tmp = validate(map.get(field.name), ftype, field.type, context + "." + field.name);
                    if (!tmp.valid) {
                        return tmp;
                    }
                }
            }
            return valid();
        }
        return validateObject(data, typedef, context);
    }

    String pretty(Object o) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_NULL_MAP_VALUES);
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
        
    }

    String javaFieldName(String rdlFieldName) {
        switch (rdlFieldName) {
        case "default":
            return "_default";
        default:
            return rdlFieldName;
        }
    }

    void flattenFields(StructTypeDef typedef, List<StructFieldDef> fields) {
        if (!typedef.type.equalsIgnoreCase("Struct")) {
            Type t = type(typedef.type);
            flattenFields(t.StructTypeDef, fields);
        }
        fields.addAll(typedef.fields);
    }

    Result validateObject(Object data, StructTypeDef typedef, String context) {
        Class<?> cl = data.getClass();
        List<StructFieldDef> fields = new ArrayList<StructFieldDef>();
        flattenFields(typedef, fields);
        for (StructFieldDef f : fields) {
            String fname = f.name;
            fname = javaFieldName(fname);
            try {
                java.lang.reflect.Field field = cl.getField(fname);
                try {
                    Object fdata = field.get(data);
                    if (fdata == null) {
                        if (f._default != null) {
                            field.set(data, f._default);
                            continue;
                        } else if (!f.optional) {
                            return error(context, "Missing required field: " + fname + " for type " + typedef.name);
                        } else {
                            continue;
                        }
                    } else {
                        if (f.keys != null) {
                            MapTypeDef td = new MapTypeDef().type("Map").keys(f.keys).items(f.items);
                            return validateMapType(fdata, td, context);
                        } else if (f.items != null) {
                            ArrayTypeDef td = new ArrayTypeDef().type("Array").items(f.items);
                            return validateArrayType(fdata, td, context);
                        }
                        Result tmp = validate(fdata, f.type, f.type, context + "." + fname);
                        if (!tmp.valid) {
                            return tmp;
                        }
                    }
                } catch (IllegalAccessException e) {
                    return error(context, "Inaccessible field in object: " + fname + " for type " + typedef.name);
                }
            } catch (NoSuchFieldException e) {
                return error(context, "Missing field in object: " + fname + " for type " + typedef.name);
            }
        }
        return valid();
    }

    Result validateStringType(Object data, StringTypeDef typedef, String context) {
        if (data != null) {
            if (data instanceof String) {
                String s = (String)data;
                int len = s.length();
                if (typedef.maxSize != null) {
                    if (len > typedef.maxSize) {
                        return error(context, "String larger than maxSize of " + typedef.maxSize + " for type " + typedef.name);
                    }
                }
                if (typedef.minSize != null) {
                    if (len < typedef.minSize) {
                        return error(context, "String smaller than minSize of " + typedef.minSize + " for type " + typedef.name);
                    }
                }
                if (typedef.pattern != null) {
                    if (!s.matches(typedef.pattern)) {
                        return error(context, "String pattern mismatch (expected \"" + typedef.pattern + "\")  for type " + typedef.name);
                    }
                }
            }
        }
        return valid();
    }

    long longValueOf(com.yahoo.rdl.Number n) {
        switch (n.variant) {
        case Int8:
            return (long)n.Int8;
        case Int16:
            return (long)n.Int16;
        case Int32:
            return (long)n.Int32;
        case Int64:
            return (long)n.Int64;
        default:
            return 0;
        }
    }

    double doubleValueOf(com.yahoo.rdl.Number n) {
        switch (n.variant) {
        case Float32:
            return (double)n.Float32;
        case Float64:
            return (double)n.Float64;
        default:
            return 0;
        }
    }

    Result checkNumber(java.lang.Number n, com.yahoo.rdl.Number ref, boolean checkMin, String context, String tname) {
        switch (ref.variant) {
        case Int8:
        case Int16:
        case Int32:
        case Int64:
            long nref = longValueOf(ref);
            if (checkMin) {
                if (n.longValue() < nref) {
                    return error(context, "Number smaller than min of " + nref + " for type " + tname);
                }
            } else {
                if (n.longValue() > nref) {
                    return error(context, "Number larger than max of " + nref + " for type " + tname);
                }
            }
            return null;
        case Float32:
        case Float64:
        default:
            double dref = doubleValueOf(ref);
            if (checkMin) {
                if (n.doubleValue() < dref) {
                    return error(context, "Number smaller than min of " + dref + " for type " + tname);
                }
            } else {
                if (n.doubleValue() > dref) {
                    return error(context, "Number larger than max of " + dref + " for type " + tname);
                }
            }
            return null;
        }
    }

    Result validateNumberType(Object data, NumberTypeDef typedef, String context) {
        if (data != null) {
            if (data instanceof java.lang.Number) {
                java.lang.Number n = ((java.lang.Number)data);
                if (typedef.max != null) {
                    Result r = checkNumber(n, typedef.max, false, context, typedef.name);
                    if (r != null) {
                        return r;
                    }
                }
                if (typedef.min != null) {
                    Result r = checkNumber(n, typedef.min, true, context, typedef.name);
                    if (r != null) {
                        return r;
                    }
                }
            }
        }
        return valid();
    }

    Result validateEnumType(Object data, EnumTypeDef typedef, String context) {
        if (data instanceof String) {
            String sym = (String)data;
            for (EnumElementDef ed : typedef.elements) {
                if (sym.equals(ed.symbol)) {
                    return valid();
                }
            }
            return error(context, "Not a valid " + typedef.name + ", " + data.getClass().getName());
        }
        return valid();
    }

    Result validateUnionType(Object data, UnionTypeDef typedef, String context) {
        if (data == null) {
            return valid();
        }
        if (data instanceof Map) {
            Map map = (Map)data;
            for (String variant : typedef.variants) {
                if (map.containsKey(variant)) {
                    return validate(map.get(variant), variant, variant, context + "<" + variant + ">");
                }
            }
        } else {
            Class<?> cl = data.getClass();
            for (String variant : typedef.variants) {
                //special _default?
                try {
                    java.lang.reflect.Field field = cl.getField(variant);
                    try {
                        Object vdata = field.get(data);
                        if (vdata != null) {
                            return validate(vdata, variant, variant, context + "<" + variant + ">");
                        }
                    } catch (IllegalAccessException e) {
                        return error(context, "Inaccessible field in object: " + variant + " for type " + typedef.name);
                    }
                } catch (NoSuchFieldException e) {
                    return error(context, "Missing field in object: " + variant + " for type " + typedef.name);
                }
            }
        }
        return error(context, "Not a valid " + typedef.name + ": " + data);
    }

    //    Result validateBaseType(Object data, BaseType typedef, String context) {
    //        return validateBaseType(data, typedef, String.valueOf(typedef), context);
    //    }

    Result validateBaseType(Object data, BaseType typedef, String alias, String context) {
        switch (typedef) {
        case Bool:
            if (data instanceof Boolean) {
                return valid();
            }
            break;
        case Int8:
            if (data instanceof Byte) {
                return valid();
            }
            if (data instanceof Integer) {
                int n = ((Integer)data).intValue();
                if (n <= 127 && n >= -128) {
                    return valid();
                }
            }
            break;
        case Int16:
            if ((data instanceof Short) || (data instanceof Byte)) {
                return valid();
            }
            if (data instanceof Integer) {
                int n = ((Integer)data).intValue();
                if (n <= 32767 && n >= -32768) {
                    return valid();
                }
            }
            break;
        case Int32:
            if ((data instanceof Integer) || (data instanceof Short) || (data instanceof Byte)) {
                return valid();
            }
            break;
        case Int64:
            if ((data instanceof Long) || (data instanceof Integer) || (data instanceof Short) || (data instanceof Byte)) {
                return valid();
            }
            break;
        case Float32:
            if (data instanceof Float) {
                return valid();
            }
            break;
        case Float64:
            if (data instanceof Double) {
                return valid();
            }
            break;
        case String:
            if (data instanceof String) {
                return valid();
            }
            break;
        case Bytes:
            if (data instanceof byte []) {
                return valid();
            }
            break;
        case Timestamp:
            if (data instanceof Timestamp) {
                return valid();
            } else if (data instanceof String) {
                Timestamp ts = Timestamp.fromString((String)data);
                if (ts != null) {
                    return valid();
                }
            }
            break;
        case UUID:
            if (data instanceof UUID) {
                return valid();
            } else if (data instanceof String) {
                UUID u = UUID.fromString((String)data);
                if (u != null) {
                    return valid();
                }
            }
            break;
        case Array:
            if (data == null || data instanceof List) {
                return valid();
            }
            break;
        case Map:
            if (data == null || data instanceof Map) {
                return valid();
            }
            break;
        case Any:
            return valid();
        }
        String s = "null";
        if (data != null) {
            s = data.getClass().getName();
        }
        return error(context, "Not a valid " + alias + ", " + s);
    }
}
