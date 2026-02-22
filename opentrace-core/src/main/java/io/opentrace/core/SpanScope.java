package io.opentrace.core;

public final class SpanScope implements AutoCloseable{
    private final OpenTrace tracer;
    private boolean closed=false;

    SpanScope(OpenTrace tracer){
        this.tracer=tracer;
    }

    @Override
    public void close(){
        if(!closed){
            tracer.endSpan();
            closed=true;
        }
    }
}