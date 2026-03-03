package io.opentrace.core;

import java.util.Map;
import java.util.Collections;

public final class OpenTraceContext{
    private final long traceId;
    private final long parentSpanId;
    private final Map<String,String> baggage;

    public OpenTraceContext(long traceId, long parentSpanId, Map<String,String> baggage){
        this.traceId=traceId;
        this.parentSpanId=parentSpanId;
        this.baggage=baggage==null?Collections.emptyMap():Collections.unmodifiableMap(baggage);
    }

    public long getTraceId(){
        return traceId;
    }

    public long getParentSpanId(){
        return parentSpanId;
    }

    public Map<String,String> getBaggage(){
        return baggage;
    }
}