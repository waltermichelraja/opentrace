package io.opentrace.core.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.opentrace.core.Span;
import io.opentrace.core.Trace;

public final class SpanMetricsCollector{
    private final Map<String,SpanMetrics> metrics=new ConcurrentHashMap<>();

    public void record(Trace trace){
        for(Span span:trace.spans){
            String name=trace.resolveName(span.getNameId());
            SpanMetrics m=metrics.computeIfAbsent(name, k->new SpanMetrics());
            boolean error=span.getStatus().name().equals("ERROR");
            m.record(span.getDuration(), error);
        }
    }

    public Map<String,SpanMetrics> getMetrics(){
        return metrics;
    }

}