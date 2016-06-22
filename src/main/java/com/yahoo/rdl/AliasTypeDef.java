//
// This file generated by rdl 1.4.8-SNAPSHOT
//

package com.yahoo.rdl;
import java.util.Map;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

//
// AliasTypeDef - AliasTypeDef is used for type definitions that add no
// additional attributes, and thus just create an alias
//
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class AliasTypeDef {
    public String type;
    public String name;
    @RdlOptional
    public String comment;
    @RdlOptional
    public Map<String, String> annotations;

    public AliasTypeDef type(String type) {
        this.type = type;
        return this;
    }
    public AliasTypeDef name(String name) {
        this.name = name;
        return this;
    }
    public AliasTypeDef comment(String comment) {
        this.comment = comment;
        return this;
    }
    public AliasTypeDef annotations(Map<String, String> annotations) {
        this.annotations = annotations;
        return this;
    }

    @Override
    public boolean equals(Object another) {
        if (this != another) {
            if (another == null || another.getClass() != AliasTypeDef.class) {
                return false;
            }
            AliasTypeDef a = (AliasTypeDef) another;
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
