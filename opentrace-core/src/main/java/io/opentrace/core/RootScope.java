package io.opentrace.core;

import java.util.concurrent.atomic.AtomicBoolean;

public final class RootScope implements AutoCloseable{
    private final OpenTrace tracer;
    private final TraceState state;
    private final Span span;
    private final AtomicBoolean closed=new AtomicBoolean();

    RootScope(OpenTrace tracer,TraceState state,Span span){
        this.tracer=tracer;
        this.state=state;
        this.span=span;
    }

    public RootScope setAttribute(String key,Object value){
        if(span!=null&&!closed.get()){
            span.putAttribute(key,value);
        }
        return this;
    }

    public void error(Throwable error){
        if(span!=null&&!closed.get()&&error!=null){
            span.status=SpanStatus.ERROR;
            span.errorType=error.getClass().getSimpleName();
            span.errorMessage=error.getMessage();
        }
    }

    public void makeCurrent(){
        if(!closed.get()){
            tracer.makeCurrent(state);
        }
    }

    public void detach(){
        tracer.detach(state);
    }

    @Override
    public void close(){
        if(closed.compareAndSet(false,true)){
            tracer.endRoot(state);
        }
    }
}
