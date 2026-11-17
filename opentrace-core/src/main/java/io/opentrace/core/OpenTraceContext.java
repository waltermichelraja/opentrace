package io.opentrace.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class OpenTraceContext{
    private final TraceId traceId;
    private final long parentSpanId;
    private final Map<String,String> baggage;

    public OpenTraceContext(TraceId traceId,long parentSpanId,Map<String,String> baggage){
        this.traceId=Objects.requireNonNull(traceId,"traceId cannot be null");
        this.parentSpanId=parentSpanId;
        this.baggage=baggage==null
            ?Collections.emptyMap()
            :Collections.unmodifiableMap(new HashMap<>(baggage));
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