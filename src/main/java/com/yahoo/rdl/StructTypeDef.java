//
// This file generated by rdl 1.5.1. Do not modify!
//

package com.yahoo.rdl;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

//
// StructTypeDef - A struct can restrict specific named fields to specific
// types. By default, any field not specified is allowed, and can be of any
// type. Specifying closed means only those fields explicitly
//
public class StructTypeDef {
    public String type;
    public String name;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String comment;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, String> annotations;
    public List<StructFieldDef> fields;
    public boolean closed;

    public StructTypeDef type(String type) {
        this.type = type;
        return this;
    }
    public StructTypeDef name(String name) {
        this.name = name;
        return this;
    }
    public StructTypeDef comment(String comment) {
        this.comment = comment;
        return this;
    }
    public StructTypeDef annotations(Map<String, String> annotations) {
        this.annotations = annotations;
        return this;
    }
    public StructTypeDef fields(List<StructFieldDef> fields) {
        this.fields = fields;
        return this;
    }
    public StructTypeDef closed(boolean closed) {
        this.closed = closed;
        return this;
    }

    @Override
    public boolean equals(Object another) {
        if (this != another) {
            if (another == null || another.getClass() != StructTypeDef.class) {
                return false;
            }
            StructTypeDef a = (StructTypeDef) another;
            if (type == null ? a.type != null : !type.equals(a.type)) {
                return false;
            }
            if (name == null ? a.name != null : !name.equals(a.name)) {
                return false;
            }
            if (comment == null ? a.comment != null : !comment.equals(a.comment)) {
                return false;
            }
            if (annotations == null ? a.annotations != null : !annotations.equals(a.annotations)) {
                return false;
            }
            if (fields == null ? a.fields != null : !fields.equals(a.fields)) {
                return false;
            }
            if (closed != a.closed) {
                return false;
            }
        }
        return true;
    }
}
