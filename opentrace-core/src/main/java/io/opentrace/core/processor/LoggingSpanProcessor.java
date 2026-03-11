package io.opentrace.core.processor;

import io.opentrace.core.Span;
import io.opentrace.core.Trace;

import java.util.*;

public final class LoggingSpanProcessor implements SpanProcessor{

    @Override
    public void onEnd(Trace trace){
        Span root=findRoot(trace);
        if(root==null){return;}
        String rootName=trace.resolveName(root.getNameId());
        System.out.println("TRACE "+rootName);
        Map<Long,List<Span>> children=buildChildren(trace);
        printChildren(root, children, trace, " ");
    }

    private Span findRoot(Trace trace){
        for(Span span: trace.spans){
            if(span.getParentSpanId()==0){return span;}
        }
        return null;
    }

    private Map<Long,List<Span>> buildChildren(Trace trace){
        Map<Long,List<Span>> map=new HashMap<>();
        for(Span span:trace.spans){
            map.computeIfAbsent(span.getParentSpanId(), k->new ArrayList<>()).add(span);
        }
        return map;
    }

    private void printChildren(Span parent, Map<Long,List<Span>> children, Trace trace, String indent){
        List<Span> list=children.get(parent.getSpanId());
        if(list==null){return;}
        for(int i=0;i<list.size();i++){
            Span span=list.get(i);
            boolean last=i==list.size()-1;
            String branch=last?" └─ ":" ├─ ";
            String name=trace.resolveName(span.getNameId());
            double ms=span.getDuration()/1_000_000.0;
            System.out.printf("%s%s%s (%.2fms)%n",indent, branch, name, ms);
            printChildren(span, children, trace, indent+(last?"   ":" │ "));
        }
    }
}