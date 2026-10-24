package io.opentrace.core;

import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public final class Trace{
    public final long traceId;
    public final List<Span> spans;
    private final NameRegistry nameRegistry;
    public final Resource resource;

    public Trace(long traceId, List<Span> spans, NameRegistry nameRegistry, Resource resource){
        this.traceId=traceId;
        this.spans=spans;
        this.nameRegistry=nameRegistry;
        this.resource=resource;
    }

    public String resolveName(int nameId){
        return nameRegistry.name(nameId);
    }

    public Span getRoot(){
        Set<Long> ids=new HashSet<>();
        for(Span s: spans){ids.add(s.getSpanId());}
        Span orphan=null;
        for(Span s: spans){
            if(s.getParentSpanId()==0){return s;}
            if(orphan==null && !ids.contains(s.getParentSpanId())){orphan=s;}
        }
        return orphan;
    }

    public String toJson(){
        StringBuilder sb=new StringBuilder();
        sb.append("{\"traceId\":").append(traceId).append(",\"resource\":{");
        boolean first=true;
        for(Map.Entry<String,String> e: resource.getAttributes().entrySet()){
            if(!first){sb.append(",");}
            first=false;
            sb.append(Json.quote(e.getKey())).append(":").append(Json.quote(e.getValue()));
        }
        sb.append("},");
        Span root=getRoot();
        if(root!=null){
            sb.append("\"root\":");
            appendSpanJson(sb,root);
        }else{
            sb.append("\"root\":null");
        }
        sb.append("}");
        return sb.toString();
    }

    private void appendSpanJson(StringBuilder sb, Span span){
        sb.append("{\"name\":").append(Json.quote(resolveName(span.getNameId())));
        sb.append(",\"durationMs\":").append(span.getDuration()/1_000_000.0);
        sb.append(",\"status\":").append(Json.quote(span.getStatus().name()));
        if(span.getStatus()==SpanStatus.ERROR){
            sb.append(",\"error\":{\"type\":").append(Json.quote(span.getErrorType()));
            sb.append(",\"message\":").append(Json.quote(span.getErrorMessage())).append("}");
        }
        Map<String,Object> attrs=span.getAttributes();
        if(!attrs.isEmpty()){
            sb.append(",\"attributes\":{");
            boolean first=true;
            for(Map.Entry<String,Object> e: attrs.entrySet()){
                if(!first){sb.append(",");}
                first=false;
                sb.append(Json.quote(e.getKey())).append(":").append(Json.value(e.getValue()));
            }
            sb.append("}");
        }
        List<SpanEvent> events=span.getEvents();
        synchronized(events){
            if(!events.isEmpty()){
                sb.append(",\"events\":[");
                boolean first=true;
                for(SpanEvent ev: events){
                    if(!first){sb.append(",");}
                    first=false;
                    sb.append("{\"name\":").append(Json.quote(ev.getName()));
                    sb.append(",\"timestampNs\":").append(ev.getTimestamp()).append("}");
                }
                sb.append("]");
            }
        }
        List<SpanLink> links=span.getLinks();
        if(!links.isEmpty()){
            sb.append(",\"links\":[");
            boolean first=true;
            for(SpanLink l: links){
                if(!first){sb.append(",");}
                first=false;
                sb.append("{\"traceId\":").append(l.getTraceId());
                sb.append(",\"spanId\":").append(l.getSpanId()).append("}");
            }
            sb.append("]");
        }
        sb.append(",\"children\":[");
        boolean firstChild=true;
        for(Span child: spans){
            if(child!=span && child.getParentSpanId()==span.getSpanId()){
                if(!firstChild){sb.append(",");}
                firstChild=false;
                appendSpanJson(sb,child);
            }
        }
        sb.append("]}");
    }
}