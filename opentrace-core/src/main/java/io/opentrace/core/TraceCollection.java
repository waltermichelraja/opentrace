package io.opentrace.core;

import java.util.List;

public final class TraceCollection{
    private final List<Trace> traces;

    public TraceCollection(List<Trace> traces){
        this.traces=traces;
    }

    @Override
    public String toString(){
        StringBuilder sb=new StringBuilder();
        sb.append("{\"traces\":[");
        boolean first=true;
        for(Trace t: traces){
            if(!first){sb.append(",");}
            first=false;
            sb.append(t.toJson());
        }
        sb.append("]}");
        return sb.toString();
    }
}