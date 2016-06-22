//
// This file generated by rdl 1.4.8-SNAPSHOT
//

package com.yahoo.rdl;
import java.util.Map;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

//
// MapTypeDef - Map types can be restricted by key type, item type and size
//
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class MapTypeDef {
    public String type;
    public String name;
    @RdlOptional
    public String comment;
    @RdlOptional
    public Map<String, String> annotations;
    public String keys;
    public String items;
    @RdlOptional
    public Integer size;
    @RdlOptional
    public Integer minSize;
    @RdlOptional
    public Integer maxSize;

    public MapTypeDef type(String type) {
        this.type = type;
        return this;
    }
    public MapTypeDef name(String name) {
        this.name = name;
        return this;
    }
    public MapTypeDef comment(String comment) {
        this.comment = comment;
        return this;
    }
    public MapTypeDef annotations(Map<String, String> annotations) {
        this.annotations = annotations;
        return this;
    }
    public MapTypeDef keys(String keys) {
        this.keys = keys;
        return this;
    }
    public MapTypeDef items(String items) {
        this.items = items;
        return this;
    }
    public MapTypeDef size(Integer size) {
        this.size = size;
        return this;
    }
    public MapTypeDef minSize(Integer minSize) {
        this.minSize = minSize;
        return this;
    }
    public MapTypeDef maxSize(Integer maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    @Override
    public boolean equals(Object another) {
        if (this != another) {
            if (another == null || another.getClass() != MapTypeDef.class) {
                return false;
            }
            MapTypeDef a = (MapTypeDef) another;
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
            if (keys == null ? a.keys != null : !keys.equals(a.keys)) {
                return false;
            }
            if (items == null ? a.items != null : !items.equals(a.items)) {
                return false;
            }
            if (size == null ? a.size != null : !size.equals(a.size)) {
                return false;
            }
            if (minSize == null ? a.minSize != null : !minSize.equals(a.minSize)) {
                return false;
            }
            if (maxSize == null ? a.maxSize != null : !maxSize.equals(a.maxSize)) {
                return false;
            }
        }
        return true;
    }

    //
    // sets up the instance according to its default field values, if any
    //
    public MapTypeDef init() {
        if (keys == null) {
            keys = "String";
        }
        if (items == null) {
            items = "Any";
        }
        return this;
    }
}
