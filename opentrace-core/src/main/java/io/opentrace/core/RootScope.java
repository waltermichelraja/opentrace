package io.opentrace.core;

public final class RootScope implements AutoCloseable{
    private final OpenTrace tracer;
    private final Span span;
    private boolean closed=false;

    RootScope(OpenTrace tracer,Span span){
        this.tracer=tracer;
        this.span=span;
    }

    public RootScope setAttribute(String key,Object value){
        if(span!=null){
            span.putAttribute(key,value);
        }
        return this;
    }

    public void error(Throwable error){
        if(span!=null){
            span.status=SpanStatus.ERROR;
            span.errorType=error.getClass().getSimpleName();
            span.errorMessage=error.getMessage();
        }
    }

    @Override
    public void close(){
        if(!closed){
            tracer.endRoot();
            closed=true;
        }
    }
}