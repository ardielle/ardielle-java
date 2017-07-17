//
// This file generated by rdl 1.4.14. Do not modify!
//

package com.yahoo.rdl;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

//
// ResourceInput - ResourceOutput defines input characteristics of a Resource
//
public class ResourceInput {
    public String name;
    public String type;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String comment;
    public boolean pathParam;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String queryParam;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String header;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String pattern;
    @com.fasterxml.jackson.annotation.JsonProperty("default")
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Object _default;
    public boolean optional;
    public boolean flag;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String context;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, String> annotations;

    public ResourceInput name(String name) {
        this.name = name;
        return this;
    }
    public ResourceInput type(String type) {
        this.type = type;
        return this;
    }
    public ResourceInput comment(String comment) {
        this.comment = comment;
        return this;
    }
    public ResourceInput pathParam(boolean pathParam) {
        this.pathParam = pathParam;
        return this;
    }
    public ResourceInput queryParam(String queryParam) {
        this.queryParam = queryParam;
        return this;
    }
    public ResourceInput header(String header) {
        this.header = header;
        return this;
    }
    public ResourceInput pattern(String pattern) {
        this.pattern = pattern;
        return this;
    }
    public ResourceInput _default(Object _default) {
        this._default = _default;
        return this;
    }
    public ResourceInput optional(boolean optional) {
        this.optional = optional;
        return this;
    }
    public ResourceInput flag(boolean flag) {
        this.flag = flag;
        return this;
    }
    public ResourceInput context(String context) {
        this.context = context;
        return this;
    }
    public ResourceInput annotations(Map<String, String> annotations) {
        this.annotations = annotations;
        return this;
    }

    @Override
    public boolean equals(Object another) {
        if (this != another) {
            if (another == null || another.getClass() != ResourceInput.class) {
                return false;
            }
            ResourceInput a = (ResourceInput) another;
            if (name == null ? a.name != null : !name.equals(a.name)) {
                return false;
            }
            if (type == null ? a.type != null : !type.equals(a.type)) {
                return false;
            }
            if (comment == null ? a.comment != null : !comment.equals(a.comment)) {
                return false;
            }
            if (pathParam != a.pathParam) {
                return false;
            }
            if (queryParam == null ? a.queryParam != null : !queryParam.equals(a.queryParam)) {
                return false;
            }
            if (header == null ? a.header != null : !header.equals(a.header)) {
                return false;
            }
            if (pattern == null ? a.pattern != null : !pattern.equals(a.pattern)) {
                return false;
            }
            if (_default == null ? a._default != null : !_default.equals(a._default)) {
                return false;
            }
            if (optional != a.optional) {
                return false;
            }
            if (flag != a.flag) {
                return false;
            }
            if (context == null ? a.context != null : !context.equals(a.context)) {
                return false;
            }
            if (annotations == null ? a.annotations != null : !annotations.equals(a.annotations)) {
                return false;
            }
        }
        return true;
    }
}
