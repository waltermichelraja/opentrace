package io.opentrace.core;

public final class OpenTraceContext{
    private final long traceId;
    private final long parentSpanId;

    public OpenTraceContext(long traceId,long parentSpanId){
        this.traceId=traceId;
        this.parentSpanId=parentSpanId;
    }

    public long getTraceId(){
        return traceId;
    }

    public long getParentSpanId(){
        return parentSpanId;
    }
}