/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.rdl;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A class to assist building Schemas programatically.
 */
public class SchemaBuilder {
    Schema schema;

    public SchemaBuilder(String name) {
        schema = new Schema().name(name);
        schema.types = new ArrayList<Type>();
        schema.resources = new ArrayList<Resource>();
    }

    public SchemaBuilder namespace(String namespace) {
        schema.namespace = namespace;
        return this;
    }

    public SchemaBuilder version(int version) {
        schema.version = version;
        return this;
    }

    public SchemaBuilder comment(String comment) {
        schema.comment = comment;
        return this;
    }

    public StringTypeBuilder stringType(String tname) {
        return new StringTypeBuilder(tname);
    }

    public NumberTypeBuilder numberType(String tname, String sname) {
        return new NumberTypeBuilder(tname, sname);
    }

    public StructTypeBuilder structType(String tname) {
        return new StructTypeBuilder(tname, "Struct");
    }

    public StructTypeBuilder structType(String tname, String sname) {
        return new StructTypeBuilder(tname, sname);
    }

    public EnumTypeBuilder enumType(String tname) {
        return new EnumTypeBuilder(tname);
    }

    public UnionTypeBuilder unionType(String tname) {
        return new UnionTypeBuilder(tname);
    }

    public class StructTypeBuilder {
        StructTypeDef td;
        StructTypeBuilder(String name, String superName) {
            td = new StructTypeDef().type(superName).name(name).fields(new ArrayList<StructFieldDef>());
            Type t = new Type(td);
            schema.types.add(t);
        }
        public StructTypeBuilder comment(String comment) {
            td.comment = comment;
            return this;
        }
        public StructTypeBuilder field(String fname, String ftype, boolean optional, String comment) {
            return field(fname, ftype, optional, comment, null);
        }
        public StructTypeBuilder field(String fname, String ftype, boolean optional, String comment, Object _default) {
            StructFieldDef fd = new StructFieldDef()
                .name(fname)
                .type(ftype)
                .optional(optional)
                .comment(comment)
                ._default(_default);
            td.fields.add(fd); 
           return this;
        }
        public StructTypeBuilder arrayField(String fname, String fitems, boolean optional, String comment) {
            StructFieldDef fd = new StructFieldDef()
                .name(fname)
                .type("Array")
                .items(fitems)
                .optional(optional)
                .comment(comment);
            td.fields.add(fd);
            return this;
        }
        public StructTypeBuilder mapField(String fname, String fkeys, String fitems, boolean optional, String comment) {
            StructFieldDef fd = new StructFieldDef()
                .name(fname)
                .type("Map")
                .keys(fkeys)
                .items(fitems)
                .optional(optional)
                .comment(comment);
            td.fields.add(fd);
            return this;
        }
    }

    public class EnumTypeBuilder {
        EnumTypeDef td;
        EnumTypeBuilder(String name) {
            td = new EnumTypeDef().type("Enum").name(name).elements(new ArrayList<EnumElementDef>());
            Type t = new Type(td);
            schema.types.add(t);
        }
        public EnumTypeBuilder comment(String comment) {
            td.comment = comment;
            return this;
        }
        public EnumTypeBuilder element(String sym) {
            return element(sym, null);
        }
        public EnumTypeBuilder element(String sym, String comment) {
            EnumElementDef ed = new EnumElementDef().symbol(sym);
            if (comment != null) {
                ed.comment(comment);
            }
            td.elements.add(ed); 
           return this;
        }
    }

    public class UnionTypeBuilder {
        UnionTypeDef td;
        UnionTypeBuilder(String name) {
            td = new UnionTypeDef().type("Union").name(name).variants(new ArrayList<String>());
            Type t = new Type(td);
            schema.types.add(t);
        }
        public UnionTypeBuilder comment(String comment) {
            td.comment = comment;
            return this;
        }
        public UnionTypeBuilder variant(String variant) {
            td.variants.add(variant);
           return this;
        }
    }

    public class StringTypeBuilder {
        StringTypeDef td;
        StringTypeBuilder(String name) {
            td = new StringTypeDef().type("String").name(name);
            Type t = new Type(td);
            schema.types.add(t);
        }
        public StringTypeBuilder comment(String comment) {
            td.comment = comment;
            return this;
        }
        public StringTypeBuilder pattern(String pattern) {
            td.pattern = pattern;
            return this;
        }
        public StringTypeBuilder maxSize(int size) {
            td.maxSize = size;
            return this;
        }
        //to do: minSize, values, annotations
    }

    public class NumberTypeBuilder {
        NumberTypeDef td;
        NumberTypeBuilder(String name, String supername) {
            td = new NumberTypeDef().type(supername).name(name);
            Type t = new Type(td);
            schema.types.add(t);
        }
        public NumberTypeBuilder comment(String comment) {
            td.comment = comment;
            return this;
        }
        public NumberTypeBuilder min(int min) {
            td.min = new Number(min);
            return this;
        }
        public NumberTypeBuilder min(long min) {
            td.min = new Number(min);
            return this;
        }
        public NumberTypeBuilder min(double min) {
            td.min = new Number(min);
            return this;
        }
        public NumberTypeBuilder max(int max) {
            td.max = new Number(max);
            return this;
        }
        public NumberTypeBuilder max(long max) {
            td.max = new Number(max);
            return this;
        }
        public NumberTypeBuilder max(double max) {
            td.max = new Number(max);
            return this;
        }
    }

    public ResourceBuilder resource(String rtype, String rmethod, String rpath) {
        return new ResourceBuilder(rtype, rmethod, rpath);
    }

    public class ResourceBuilder {
        Resource rez;
        ResourceBuilder(String rtype, String rmethod, String rpath) {
            rez = new Resource().type(rtype).method(rmethod).path(rpath);
            schema.resources.add(rez);
        }
        public ResourceBuilder comment(String comment) {
            rez.comment = comment;
            return this;
        }
        public ResourceBuilder name(String name) {
            rez.name = name;
            return this;
        }
        private ResourceInput addInput(String iname, String itype, String comment) {
            ResourceInput in = new ResourceInput()
                .name(iname)
                .type(itype);
            if (comment != null && comment.length() > 0) {
                in.comment(comment);
            }
            if (rez.inputs == null) {
                rez.inputs = new ArrayList<ResourceInput>();
            }
            rez.inputs.add(in);
            return in;
        }
        public ResourceBuilder input(String iname, String itype, String comment) {
            addInput(iname, itype, comment);
            return this;
        }
        public ResourceBuilder pathParam(String iname, String itype, String comment) {
            ResourceInput in = addInput(iname, itype, comment);
            in.pathParam(true);
            return this;
        }
        public ResourceBuilder queryParam(String iparam, String iname, String itype, Object _default, String comment) {
            ResourceInput in = addInput(iname, itype, comment);
            in.queryParam(iparam);
            if (_default != null) {
                in._default(_default);
            } else {
                in.optional(true);
            }
            return this;
        }
        public ResourceBuilder headerParam(String iparam, String iname, String itype, Object _default, String comment) {
            ResourceInput in = addInput(iname, itype, comment);
            in.header(iparam);
            in.optional(true);
            if (_default != null) {
                in._default(_default);
            } else {
                in.optional(true);
            }
            return this;
        }
        public ResourceBuilder output(String header, String name, String type, String comment) {
            ResourceOutput out = new ResourceOutput()
                .header(header)
                .name(name)
                .type(type);
            if (comment != null && comment.length() > 0) {
                out.comment(comment);
            }
            if (rez.outputs == null) {
                rez.outputs = new ArrayList<ResourceOutput>();
            }
            rez.outputs.add(out);
            return this;
        }
        public ResourceBuilder auth(String action, String resource) {
            return auth(action, resource, false);
        }
        public ResourceBuilder auth(String action, String resource, boolean authenticate) {
            return auth(action, resource, authenticate, null);
        }
        public ResourceBuilder auth(String action, String resource, boolean authenticate, String domain) {
            ResourceAuth auth = new ResourceAuth()
                .action(action)
                .resource(resource)
                .authenticate(authenticate);
            if (domain != null && domain.length() > 0) {
                auth.domain(domain);
            }
            rez.auth = auth;
            return this;
        }
        public ResourceBuilder expected(String sym) {
            rez.expected = sym;
            return this;
        }
        public ResourceBuilder exception(String sym, String type, String comment) {
            ExceptionDef exc = new ExceptionDef().type(type);
            if (comment != null && comment.length() > 0) {
                exc.comment(comment);
            }
            if (rez.exceptions == null) {
                rez.exceptions = new HashMap<String, ExceptionDef>();
            }
            rez.exceptions.put(sym, exc);
            return this;
        }
        public ResourceBuilder async() {
            rez.async = true;
            return this;
        }
    }

    public Schema build() {
        if (schema.types != null && schema.types.size() == 0) {
            schema.types = null;
        }
        if (schema.resources != null && schema.resources.size() == 0) {
            schema.resources = null;
        }
        return schema;
    }
}
