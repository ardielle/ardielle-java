/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.rdl;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * The RDL Symbol primitive type.
 *
 */
@JsonSerialize(using = Symbol.SymbolJsonSerializer.class)
@JsonDeserialize(using = Symbol.SymbolJsonDeserializer.class)
public final class Symbol {

    public final String name;

    private Symbol(String n) {
        name = n;
    }

    private static ConcurrentHashMap<String, Symbol> symtab = new ConcurrentHashMap<String, Symbol>();

    public static Symbol intern(String name) {
        Symbol sym = symtab.get(name);
        if (sym == null) {
            sym = new Symbol(name);
            symtab.put(name, sym);
        }
        return sym;
    }

    public static class SymbolJsonSerializer extends JsonSerializer<Symbol> {
        @Override
        public void serialize(Symbol value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeString(value.name);
        }
    }
    public static class SymbolJsonDeserializer extends JsonDeserializer<Symbol> {
        @Override
        public Symbol deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            String s = jp.getText();
            return intern(s);
        }
    }

    @Override
    public String toString() {
        return name;
    }

}
