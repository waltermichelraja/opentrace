package io.opentrace.core;

import java.util.Map;
import java.util.Collections;

public final class OpenTraceContext{
    private final TraceId traceId;
    private final long parentSpanId;
    private final Map<String,String> baggage;

    public OpenTraceContext(TraceId traceId, long parentSpanId, Map<String,String> baggage){
        this.traceId=traceId;
        this.parentSpanId=parentSpanId;
        this.baggage=baggage==null?Collections.emptyMap():Collections.unmodifiableMap(baggage);
    }

    public TraceId getTraceId(){
        return traceId;
    }

    public long getParentSpanId(){
        return parentSpanId;
    }

    public Map<String,String> getBaggage(){
        return baggage;
    }
}