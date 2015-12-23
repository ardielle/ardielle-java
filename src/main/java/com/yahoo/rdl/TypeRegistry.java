/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.rdl;
import java.util.Map;
import java.util.HashMap;

/**
 * A class to look up types in a schema.
 */
public class TypeRegistry {
    Schema schema;
    Map<String,Type> types;

    /**
     * Create a new TypeRegistry based on the specified Schema
     * @param schema the schema containing the types
     */
    public TypeRegistry(Schema schema) {
        this.schema = schema;
        this.types = new HashMap<String,Type>();
        if (schema.types != null) {
            for (Type t : schema.types) {
                String name = typeName(t);
                types.put(name, t);
            }
        }
    }

    /**
     * findType - return the type in the schema with a matching name
     * @param name the name of the type fo look up
     * @return the type for the name, or null if not found.
     */
    public Type findType(String name) {
        return types.get(name);
    }

    /**
     * typeName - given a Type (a Union type), return the name of the type.
     * @param type the type in question
     * @return the name of the type
     */
    public static String typeName(Type type) {
        switch (type.variant) {
        case BaseType:
        case StructTypeDef:
            return type.StructTypeDef.name;
        case MapTypeDef:
            return type.MapTypeDef.name;
        case ArrayTypeDef:
            return type.ArrayTypeDef.name;
        case EnumTypeDef:
            return type.EnumTypeDef.name;
        case UnionTypeDef:
            return type.UnionTypeDef.name;
        case StringTypeDef:
            return type.StringTypeDef.name;
        case BytesTypeDef:
            return type.BytesTypeDef.name;
        case NumberTypeDef:
            return type.NumberTypeDef.name;
        case AliasTypeDef:
            return type.AliasTypeDef.name;
        }
        return null;
    }

}
