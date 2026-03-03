package io.opentrace.core;

public final class SpanLink{
    private final long traceId;
    private final long spanId;

    public SpanLink(long traceId, long spanId){
        this.traceId=traceId;
        this.spanId=spanId;
    }

    public long getTraceId(){
        return traceId;
    }

    public long getSpanId(){
        return spanId;
    }
}