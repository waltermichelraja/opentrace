package io.opentrace.core;

import java.util.List;

public final class Trace{
    public final long traceId;
    public final List<Span> spans;
    private final NameRegistry nameRegistry;
    public final String serviceName;
    public final String environment;
    public final String serviceVersion;

    Trace(long traceId,List<Span> spans, NameRegistry nameRegistry, String serviceName, String environment, String serviceVersion){
        this.traceId=traceId;
        this.spans=spans;
        this.nameRegistry=nameRegistry;
        this.serviceName=serviceName;
        this.environment=environment;
        this.serviceVersion=serviceVersion;
    }

    public String resolveName(int nameId){
        return nameRegistry.name(nameId);
    }

    public String toJson(){
        StringBuilder sb=new StringBuilder();
        sb.append("{");
        sb.append("\"traceId\":").append(traceId).append(",");
        sb.append("\"serviceName\":\"").append(serviceName).append("\",");
        sb.append("\"environment\":\"").append(environment).append("\",");
        sb.append("\"serviceVersion\":\"").append(serviceVersion).append("\",");
        Span root=findRoot();
        if(root!=null){
            sb.append("\"root\":");
            appendSpanJson(sb,root);
        }else{sb.append("\"root\":null");}
        sb.append("}");
        return sb.toString();
    }

    private Span findRoot(){
        for(Span span:spans){
            if(span.getParentSpanId()==0){
                return span;
            }
        }
        return null;
    }

    private void appendSpanJson(StringBuilder sb,Span span){
        sb.append("{");
        sb.append("\"name\":\"").append(resolveName(span.getNameId())).append("\",");
        sb.append("\"durationMs\":").append(span.getDuration()/1_000_000.0).append(",");
        sb.append("\"status\":\"").append(span.getStatus().name()).append("\"");
        if(span.getStatus()==SpanStatus.ERROR){
            sb.append(",\"error\":{");
            sb.append("\"type\":\"").append(span.getErrorType()).append("\",");
            sb.append("\"message\":\"").append(span.getErrorMessage()).append("\"}");
        }
        if(span.getAttributes()!=null && !span.getAttributes().isEmpty()){
            sb.append(",\"attributes\":{");
            boolean first=true;
            for(var entry:span.getAttributes().entrySet()){
                if(!first){sb.append(",");}
                first=false;
                sb.append("\"").append(entry.getKey()).append("\":");
                Object value=entry.getValue();
                if(value instanceof String){
                    sb.append("\"").append(value).append("\"");
                }else{sb.append(value);}
            }
            sb.append("}");
        }
        sb.append(",\"children\":[");
        boolean firstChild=true;
        for(Span child:spans){
            if(child.getParentSpanId()==span.getSpanId()){
                if(!firstChild){sb.append(",");}
                firstChild=false;
                appendSpanJson(sb,child);
            }
        }
        sb.append("]");
        sb.append("}");
    }
}