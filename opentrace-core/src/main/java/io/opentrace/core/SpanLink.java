package io.opentrace.core;

public final class SpanLink{
    private final TraceId traceId;
    private final long spanId;

    public SpanLink(TraceId traceId, long spanId){
        this.traceId=traceId;
        this.spanId=spanId;
    }

    public TraceId getTraceId(){
        return traceId;
    }

    public long getSpanId(){
        return spanId;
    }
}