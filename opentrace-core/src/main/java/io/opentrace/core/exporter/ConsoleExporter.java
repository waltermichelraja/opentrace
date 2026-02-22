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
            if(span.parentSpanId==0){root=span;}
            childrenMap.computeIfAbsent(span.parentSpanId,k->new ArrayList<>()).add(span);
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("traceId", trace.traceId);
        if(root!=null){result.put("root", buildSpanMap(root,childrenMap,trace));}
        return result;
    }

    private Map<String,Object> buildSpanMap(Span span, Map<Long,List<Span>> childrenMap, Trace trace){
        Map<String,Object> spanMap=new LinkedHashMap<>();
        spanMap.put("name", trace.resolveName(span.nameId));
        spanMap.put("durationMs", span.duration/1_000_000.0);
        List<Map<String,Object>> childrenList=new ArrayList<>();
        List<Span> children=childrenMap.get(span.spanId);
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
                if(!first)sb.append(",");
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