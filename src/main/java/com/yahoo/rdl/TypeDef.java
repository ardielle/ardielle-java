//
// This file generated by rdl 1.5.1. Do not modify!
//

package com.yahoo.rdl;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

//
// TypeDef - TypeDef is the basic type definition.
//
public class TypeDef {
    public String type;
    public String name;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String comment;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, String> annotations;

    public TypeDef type(String type) {
        this.type = type;
        return this;
    }
    public TypeDef name(String name) {
        this.name = name;
        return this;
    }
    public TypeDef comment(String comment) {
        this.comment = comment;
        return this;
    }
    public TypeDef annotations(Map<String, String> annotations) {
        this.annotations = annotations;
        return this;
    }

    @Override
    public boolean equals(Object another) {
        if (this != another) {
            if (another == null || another.getClass() != TypeDef.class) {
                return false;
            }
            TypeDef a = (TypeDef) another;
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
        }
        return true;
    }
}
