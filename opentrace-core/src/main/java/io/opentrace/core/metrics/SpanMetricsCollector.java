package io.opentrace.core.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.opentrace.core.Span;
import io.opentrace.core.Trace;

public final class SpanMetricsCollector{
    private final Map<String,SpanMetrics> metrics=new ConcurrentHashMap<>();

    public void record(Trace trace){
        for(Span span: trace.spans){
            String name=trace.resolveName(span.getNameId());
            SpanMetrics m=metrics.computeIfAbsent(name, k->new SpanMetrics());
            boolean error=span.getStatus().name().equals("ERROR");
            m.record(span.getDuration(), error);
        }
    }

    public Map<String,SpanMetrics> getMetrics(){
        return metrics;
    }

    @Override
    public String toString(){
        StringBuilder sb=new StringBuilder();
        sb.append("{\"spanMetrics\":[");
        boolean first=true;
        for(Map.Entry<String,SpanMetrics> entry:metrics.entrySet()){
            if(!first){sb.append(",");}
            first=false;
            String name=entry.getKey();
            SpanMetrics m=entry.getValue();
            sb.append("{");
            sb.append("\"name\":\"").append(name).append("\",");
            sb.append("\"calls\":").append(m.count()).append(",");
            sb.append("\"errors\":").append(m.errors()).append(",");
            sb.append("\"avgMs\":").append(String.format("%.2f",m.avgMs())).append(",");
            sb.append("\"p95\":").append(String.format("%.2f",m.percentile(0.95))).append(",");
            sb.append("\"p99\":").append(String.format("%.2f",m.percentile(0.99)));
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }
}