package io.opentrace.core.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.opentrace.core.Span;
import io.opentrace.core.Trace;

public final class SpanMetricsCollector{
    public static final int DEFAULT_MAX_NAMES=500;
    public static final String OVERFLOW_NAME="_other";

    private final Map<String,SpanMetrics> metrics=new ConcurrentHashMap<>();
    private final int maxNames;
    private final int window;

    public SpanMetricsCollector(){
        this(DEFAULT_MAX_NAMES, SpanMetrics.DEFAULT_WINDOW);
    }

    public SpanMetricsCollector(int maxNames, int window){
        if(maxNames<=0){throw new IllegalArgumentException("maxNames must be positive");}
        if(window<=0){throw new IllegalArgumentException("window must be positive");}
        this.maxNames=maxNames;
        this.window=window;
    }

    public void record(Trace trace){
        for(Span span: trace.spans){
            String name=trace.resolveName(span.getNameId());
            SpanMetrics m=metrics.get(name);
            if(m==null){
                // soft cap: concurrent first-time names may overshoot by a few
                if(metrics.size()>=maxNames){name=OVERFLOW_NAME;}
                m=metrics.computeIfAbsent(name, k->new SpanMetrics(window));
            }
            boolean error=span.getStatus()==io.opentrace.core.SpanStatus.ERROR;
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
            sb.append("\"name\":").append(io.opentrace.core.Json.quote(name)).append(",");
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