//
// This file generated by rdl 1.5.1. Do not modify!
//

package com.yahoo.rdl;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

//
// Resource - A Resource of a REST service
//
public class Resource {
    public String type;
    public String method;
    public String path;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String comment;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<ResourceInput> inputs;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<ResourceOutput> outputs;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public ResourceAuth auth;
    public String expected;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<String> alternatives;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, ExceptionDef> exceptions;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Boolean async;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, String> annotations;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<String> consumes;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<String> produces;
    @RdlOptional
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String name;

    public Resource type(String type) {
        this.type = type;
        return this;
    }
    public Resource method(String method) {
        this.method = method;
        return this;
    }
    public Resource path(String path) {
        this.path = path;
        return this;
    }
    public Resource comment(String comment) {
        this.comment = comment;
        return this;
    }
    public Resource inputs(List<ResourceInput> inputs) {
        this.inputs = inputs;
        return this;
    }
    public Resource outputs(List<ResourceOutput> outputs) {
        this.outputs = outputs;
        return this;
    }
    public Resource auth(ResourceAuth auth) {
        this.auth = auth;
        return this;
    }
    public Resource expected(String expected) {
        this.expected = expected;
        return this;
    }
    public Resource alternatives(List<String> alternatives) {
        this.alternatives = alternatives;
        return this;
    }
    public Resource exceptions(Map<String, ExceptionDef> exceptions) {
        this.exceptions = exceptions;
        return this;
    }
    public Resource async(Boolean async) {
        this.async = async;
        return this;
    }
    public Resource annotations(Map<String, String> annotations) {
        this.annotations = annotations;
        return this;
    }
    public Resource consumes(List<String> consumes) {
        this.consumes = consumes;
        return this;
    }
    public Resource produces(List<String> produces) {
        this.produces = produces;
        return this;
    }
    public Resource name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public boolean equals(Object another) {
        if (this != another) {
            if (another == null || another.getClass() != Resource.class) {
                return false;
            }
            Resource a = (Resource) another;
            if (type == null ? a.type != null : !type.equals(a.type)) {
                return false;
            }
            if (method == null ? a.method != null : !method.equals(a.method)) {
                return false;
            }
            if (path == null ? a.path != null : !path.equals(a.path)) {
                return false;
            }
            if (comment == null ? a.comment != null : !comment.equals(a.comment)) {
                return false;
            }
            if (inputs == null ? a.inputs != null : !inputs.equals(a.inputs)) {
                return false;
            }
            if (outputs == null ? a.outputs != null : !outputs.equals(a.outputs)) {
                return false;
            }
            if (auth == null ? a.auth != null : !auth.equals(a.auth)) {
                return false;
            }
            if (expected == null ? a.expected != null : !expected.equals(a.expected)) {
                return false;
            }
            if (alternatives == null ? a.alternatives != null : !alternatives.equals(a.alternatives)) {
                return false;
            }
            if (exceptions == null ? a.exceptions != null : !exceptions.equals(a.exceptions)) {
                return false;
            }
            if (async == null ? a.async != null : !async.equals(a.async)) {
                return false;
            }
            if (annotations == null ? a.annotations != null : !annotations.equals(a.annotations)) {
                return false;
            }
            if (consumes == null ? a.consumes != null : !consumes.equals(a.consumes)) {
                return false;
            }
            if (produces == null ? a.produces != null : !produces.equals(a.produces)) {
                return false;
            }
            if (name == null ? a.name != null : !name.equals(a.name)) {
                return false;
            }
        }
        return true;
    }

    //
    // sets up the instance according to its default field values, if any
    //
    public Resource init() {
        if (expected == null) {
            expected = "OK";
        }
        return this;
    }
}
