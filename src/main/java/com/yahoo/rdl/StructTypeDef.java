//
// This file generated by rdl 1.4.8-SNAPSHOT
//

package com.yahoo.rdl;
import java.util.Map;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

//
// StructTypeDef - A struct can restrict specific named fields to specific
// types. By default, any field not specified is allowed, and can be of any
// type. Specifying closed means only those fields explicitly
//
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class StructTypeDef {
    public String type;
    public String name;
    @RdlOptional
    public String comment;
    @RdlOptional
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
