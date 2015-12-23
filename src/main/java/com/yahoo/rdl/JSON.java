/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.rdl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;

/**
 * Simple wrappers for JSON handling
 */
public class JSON {

    static final ObjectMapper mapper = init();

    static ObjectMapper init() {
        ObjectMapper om = new ObjectMapper();
        om.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om;
    }

    public static byte [] bytes(Object o) {
        try {
            Class<?> cls = (o == null)? Object.class : o.getClass();
            return mapper.writerWithView(cls).writeValueAsBytes(o);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String string(Object o) {
        try {
            Class<?> cls = (o == null)? Object.class : o.getClass();
            return mapper.writerWithView(cls).writeValueAsString(o);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String indented(Object o) {
        try {
            return mapper.writerWithView(o.getClass()).with(SerializationFeature.INDENT_OUTPUT).writeValueAsString(o);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T fromBytes(byte [] jsonData, Class<T> dataType) {
        try {
            return mapper.reader(dataType).readValue(jsonData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T fromString(String jsonData, Class<T> dataType) {
        try {
            return mapper.reader(dataType).readValue(jsonData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
