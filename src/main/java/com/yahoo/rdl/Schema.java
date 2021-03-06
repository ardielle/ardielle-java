//
// This file generated by rdl 1.5.1. Do not modify!
//

package com.yahoo.rdl;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

//
// Schema - A Schema is a container for types and resources. It is
// self-contained (no external references). and is the output of the RDL parser.
//
public class Schema {
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String namespace;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String name;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Integer version;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String comment;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<Type> types;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<Resource> resources;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String base;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, String> annotations;

    public Schema namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }
    public Schema name(String name) {
        this.name = name;
        return this;
    }
    public Schema version(Integer version) {
        this.version = version;
        return this;
    }
    public Schema comment(String comment) {
        this.comment = comment;
        return this;
    }
    public Schema types(List<Type> types) {
        this.types = types;
        return this;
    }
    public Schema resources(List<Resource> resources) {
        this.resources = resources;
        return this;
    }
    public Schema base(String base) {
        this.base = base;
        return this;
    }
    public Schema annotations(Map<String, String> annotations) {
        this.annotations = annotations;
        return this;
    }

    @Override
    public boolean equals(Object another) {
        if (this != another) {
            if (another == null || another.getClass() != Schema.class) {
                return false;
            }
            Schema a = (Schema) another;
            if (namespace == null ? a.namespace != null : !namespace.equals(a.namespace)) {
                return false;
            }
            if (name == null ? a.name != null : !name.equals(a.name)) {
                return false;
            }
            if (version == null ? a.version != null : !version.equals(a.version)) {
                return false;
            }
            if (comment == null ? a.comment != null : !comment.equals(a.comment)) {
                return false;
            }
            if (types == null ? a.types != null : !types.equals(a.types)) {
                return false;
            }
            if (resources == null ? a.resources != null : !resources.equals(a.resources)) {
                return false;
            }
            if (base == null ? a.base != null : !base.equals(a.base)) {
                return false;
            }
            if (annotations == null ? a.annotations != null : !annotations.equals(a.annotations)) {
                return false;
            }
        }
        return true;
    }
}
