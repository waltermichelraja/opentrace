package io.opentrace.core.exporter;

import io.opentrace.core.Trace;
import io.opentrace.core.Span;

import java.util.*;

public final class ConsoleExporter implements SpanExporter{
    @Override
    public void export(List<Trace> traces){
        for(Trace trace:traces){
            Map<String,Object> traceMap=buildTraceMap(trace);
            System.out.println(toJson(traceMap));
        }
    }

    private Map<String,Object> buildTraceMap(Trace trace){
        Map<Long,List<Span>> childrenMap=new HashMap<>();
        Span root=null;
        for(Span span:trace.spans){
            if(span.getParentSpanId()==0){root=span;}
            childrenMap.computeIfAbsent(span.getParentSpanId(), k->new ArrayList<>()).add(span);
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("traceId", trace.traceId);
        result.put("serviceName", trace.serviceName);
        result.put("environment", trace.environment);
        result.put("serviceVersion", trace.serviceVersion);
        if(root!=null){result.put("root", buildSpanMap(root, childrenMap, trace));}
        return result;
    }

    private Map<String,Object> buildSpanMap(Span span, Map<Long,List<Span>> childrenMap, Trace trace){
        Map<String,Object> spanMap=new LinkedHashMap<>();
        spanMap.put("name", trace.resolveName(span.getNameId()));
        spanMap.put("durationMs", span.getDuration()/1_000_000.0);
        spanMap.put("status", span.getStatus().name());
        if(span.getStatus()==io.opentrace.core.SpanStatus.ERROR){
            Map<String,Object> errorMap=new LinkedHashMap<>();
            errorMap.put("type", span.getErrorType());
            errorMap.put("message", span.getErrorMessage());
            spanMap.put("error", errorMap);
        }
        Map<String,Object> attributes=span.getAttributes();
        if(attributes!=null && !attributes.isEmpty()){
            spanMap.put("attributes", attributes);
        }
        List<Map<String,Object>> childrenList=new ArrayList<>();
        List<Span> children=childrenMap.get(span.getSpanId());
        if(children!=null){
            for(Span child:children){
                childrenList.add(buildSpanMap(child, childrenMap, trace));
            }
        }
        spanMap.put("children", childrenList);
        return spanMap;
    }

    private String toJson(Object obj){
        if(obj instanceof Map){
            StringBuilder sb=new StringBuilder();
            sb.append("{");
            boolean first=true;
            for(Object entryObj:((Map<?,?>)obj).entrySet()){
                Map.Entry<?,?> entry=(Map.Entry<?,?>)entryObj;
                if(!first){sb.append(",");}
                first=false;
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(toJson(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if(obj instanceof List){
            StringBuilder sb=new StringBuilder();
            sb.append("[");
            boolean first=true;
            for(Object item:(List<?>)obj){
                if(!first){sb.append(",");}
                first=false;
                sb.append(toJson(item));
            }
            sb.append("]");
            return sb.toString();
        }
        if(obj instanceof String){return "\"" + obj + "\"";}
        return String.valueOf(obj);
    }
}