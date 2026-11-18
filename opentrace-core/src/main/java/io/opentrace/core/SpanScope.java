package io.opentrace.core;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SpanScope implements AutoCloseable{
    private final OpenTrace tracer;
    private final Span span;
    private final AtomicBoolean closed=new AtomicBoolean();

    SpanScope(OpenTrace tracer,Span span){
        this.tracer=tracer;
        this.span=span;
    }

    public SpanScope setAttribute(String key,Object value){
        if(span!=null&&!closed.get()){
            span.putAttribute(key,value);
        }
        return this;
    }

    public void error(Throwable throwable){
        if(span!=null&&!closed.get()&&throwable!=null){
            span.status=SpanStatus.ERROR;
            span.errorType=throwable.getClass().getSimpleName();
            span.errorMessage=throwable.getMessage();
        }
    }

    public void addEvent(String name){
        if(span!=null&&!closed.get()){
            span.addEvent(name);
        }
    }

    public void addLink(TraceId traceId,long spanId){
        if(span!=null&&!closed.get()){
            span.addLink(traceId,spanId);
        }
    }

    @Override
    public void close(){
        if(closed.compareAndSet(false,true)){
            tracer.endSpan();
        }
    }
}