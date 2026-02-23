package io.opentrace.core;

public final class SpanScope implements AutoCloseable{
    private final OpenTrace tracer;
    private final Span span;
    private boolean closed=false;

    SpanScope(OpenTrace tracer, Span span){
        this.tracer=tracer;
        this.span=span;
    }

    public SpanScope setAttribute(String key, Object value){
        if(span!=null){
            if(span.attributes==null){
                span.attributes=new java.util.LinkedHashMap<>();
            }
            span.attributes.put(key, value);
        }
        return this;
    }

    public void error(Throwable t){
        if(span!=null){
            span.status=SpanStatus.ERROR;
            span.errorType=t.getClass().getSimpleName();
            span.errorMessage=t.getMessage();
        }
    }

    @Override
    public void close(){
        if(!closed){
            tracer.endSpan();
            closed=true;
        }
    }
}