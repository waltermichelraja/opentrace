package io.opentrace.core;

import java.util.concurrent.atomic.AtomicBoolean;

public final class AsyncSpanScope implements AutoCloseable{
    private final OpenTrace tracer;
    private final TraceState state;
    private final Span span;
    private final AtomicBoolean closed=new AtomicBoolean();

    AsyncSpanScope(OpenTrace tracer,TraceState state,Span span){
        this.tracer=tracer;
        this.state=state;
        this.span=span;
    }

    public AsyncSpanScope setAttribute(String key,Object value){
        if(span!=null&&!closed.get()){
            span.putAttribute(key,value);
        }
        return this;
    }

    public void error(Throwable throwable){
        if(span!=null&&!closed.get()){
            span.status=SpanStatus.ERROR;
            span.errorType=throwable.getClass().getSimpleName();
            span.errorMessage=throwable.getMessage();
        }
    }

    @Override
    public void close(){
        if(span==null||!closed.compareAndSet(false,true)){
            return;
        }

        span.finish();
        state.spans.add(span);
        state.pendingAsyncSpans.decrementAndGet();
        tracer.completeIfReady(state);
    }
}
