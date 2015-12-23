/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.tbin;
import com.yahoo.rdl.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Wrapper for basic TBin functionality. TBin is a binary encoding scheme.
 */
public class TBin {

    //constructor only available in this package
    TBin() { }

    /**
     * Encode the object into TBin, reflecting as needed.
     * @param o the object to encode
     * @return a byte array containing the tbin encoding
     */
    public static byte [] bytes(Object o) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            TBinEncoder enc = new TBinEncoder(out);
            enc.encode(o);
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte [] bytes(Object o, TypeDef sig) {
        //this one should be optimized for one pass, avoiding reflection where possible
        //the one pass still must walk the object to emit. Unless it implements Marshallable?
        throw new RuntimeException("TBin.bytes(Object, TypeDef) - not implemented");
    }

    public static byte [] bytes(Object o, TypeRegistry reg, String typeName) {
        //this one must reflect and walk the object, probably twice to 1) create signature, and 2) emit values
        throw new RuntimeException("TBin.bytes(Object, TypeRegistry, String) - not implemented");
    }

    /**
     * Decode the TBin bytes, producing a generic representation of the data.
     * @param tbinData the TBin-encoded data to decode
     * @return the decoded object
     */
    public static Object fromBytes(byte [] tbinData) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(tbinData);
            TBinDecoder dec = new TBinDecoder(in);
            return dec.decode();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decode the TBin bytes, mapping the result onto the target class.
     * If the target class is Object, the generic decode is done instead.
     * @param <T> the type of data expected
     * @param tbinData the TBin-encoded data to decode
     * @param dataType the class to decode as.
     * @return the decoded object
     */
    public static <T> T fromBytes(byte [] tbinData, Class<T> dataType) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(tbinData);
            TBinDecoder dec = new TBinDecoder(in);
            return dec.decode(dataType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static final int CURRENT_VERSION = 1;

    static final int NULL_TAG           = 0x00; // "NULL_TAG"
    static final int BOOL_TAG           = 0x01; // "BOOL_TAG varint(b? 1 : 0)"
    static final int INT8_TAG           = 0x02; // "INT8_TAG varint(n)"
    static final int INT16_TAG          = 0x03; // "INT16_TAG varint(n)"
    static final int INT32_TAG          = 0x04; // "INT32_TAG varint(n)"
    static final int INT64_TAG          = 0x05; // "INT64_TAG varint(n)"
    static final int FLOAT32_TAG        = 0x06; // "FLOAT32_TAG 4_bytes"
    static final int FLOAT64_TAG        = 0x07; // "FLOAT64_TAG 8_bytes"
    static final int BYTES_TAG          = 0x08; // "BYTES_TAG varint(len) byte*"
    static final int STRING_TAG         = 0x09; // "STRING_TAG varint(utflen) utf8bytes*"
    static final int TIMESTAMP_TAG      = 0x0a; // "TIMESTAMP_TAG double" - represented as seconds since epoch
    static final int SYMBOL_TAG         = 0x0b; // "SYMBOL_TAG varint(id) [string(name)]"
    static final int UUID_TAG           = 0x0c; // "UUID_TAG byte[16]" = written as 16 bytes
    static final int ARRAY_TAG          = 0x0d; // "ARRAY_TAG varint(size) value*"
    static final int MAP_TAG            = 0x0e; // "MAP_TAG varint(size) (<value> <value>)*
    static final int STRUCT_TAG         = 0x0f; // "STRUCT_TAG varint(size) (<symbol> <value>)* - keys are symbol-compatible strings
    static final int ANY_TAG            = 0x10; // used only as a value for array and map items and keys, in an extensible type.

    static final int DEF_ARRAY_TAG      = 0x11;
    static final int DEF_MAP_TAG        = 0x12;
    static final int DEF_STRUCT_TAG     = 0x13;
    static final int DEF_UNION_TAG      = 0x14;
    static final int DEF_ENUM_TAG       = 0x15;

    static final int UNION_TAG          = 0x16;
    static final int ENUM_TAG           = 0x17;

    //A version tag should be the first tag in the stream. v1..v8 are thus supported, after which additional byte(s)
    //will be required. Bits encode (version - 1), i.e. currently encoded "0001 0000".
    static final int VERSION_TAG        = 0x18; // "0001 1xxx"
    static final int VERSION_TAG_MASK   = 0xf8;
    static final int VERSION_DATA_MASK  = 0x07;
    static final int MIN_VERSION_TAG    = VERSION_TAG;
    static final int CUR_VERSION_TAG    = VERSION_TAG + (CURRENT_VERSION - 1); // "0001 1000
    static final int MAX_VERSION_TAG    = VERSION_TAG + VERSION_DATA_MASK;
    static final int MAX_VERSION        = VERSION_DATA_MASK + 3;

    static final int TINY_STR_TAG       = 0x20; // "001x xxxx" <utf8byte>*
    static final int TINY_STR_TAG_MASK  = 0xe0;
    static final int TINY_STR_DATA_MASK = 0x1f;
    static final int TINY_STR_MAXLEN    = TINY_STR_DATA_MASK;

    static final int FIRST_USER_TAG     = 0x40; //0x40..0x7f fit in a single byte, others take more. The tag is an unsigned  varint.


    final private static char[] hexDigits = "0123456789abcdef".toCharArray();

    public static String hexByte(byte b) {
        return "" + hexDigits[(b >>> 4) & 15] + hexDigits[b & 15];
    }
    public static String hexByte(int b) {
        return hexByte((byte)b);
    }

    public static String hex(byte [] bytes, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<count; i++) {
            byte b = bytes[i];
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(hexDigits[(b >>> 4) & 15]);
            sb.append(hexDigits[b & 15]);
        }
        return sb.toString();
    }

    static void println(Object o) { System.out.println(o); }
    void panic(Object msg) {
        try {
            throw new RuntimeException("Panic: " + msg);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}