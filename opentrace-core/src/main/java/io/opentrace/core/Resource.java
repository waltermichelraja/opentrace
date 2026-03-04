package io.opentrace.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Resource{
    private final Map<String,String> attributes;

    public Resource(Map<String,String> attributes){
        this.attributes=new LinkedHashMap<>(attributes);
    }

    public Map<String,String> getAttributes(){
        return Collections.unmodifiableMap(attributes);
    }

}