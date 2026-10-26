package io.opentrace.core;

import io.opentrace.core.exporter.SpanExporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TestSupport{
    private TestSupport(){}

    static OpenTrace tracer(){
        return OpenTrace.builder().serviceName("test").disableExporter().build();
    }

    static Span find(Trace trace, String name){
        for(Span s: trace.spans){
            if(name.equals(trace.resolveName(s.getNameId()))){return s;}
        }
        throw new AssertionError("span not found: "+name);
    }

    static Trace only(OpenTrace tracer){
        var traces=tracer.getTraceList();
        if(traces.size()!=1){throw new AssertionError("expected 1 trace, got "+traces.size());}
        return traces.get(0);
    }

    static final class CapturingExporter implements SpanExporter{
        final List<Trace> received=Collections.synchronizedList(new ArrayList<>());
        @Override
        public void export(List<Trace> traces){received.addAll(traces);}
    }
}