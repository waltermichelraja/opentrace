package io.opentrace.core;

public final class RootScope implements AutoCloseable{
    private final OpenTrace tracer;
    private boolean closed=false;

    RootScope(OpenTrace tracer){
        this.tracer=tracer;
    }

    @Override
    public void close(){
        if(!closed){
            tracer.endRoot();
            closed=true;
        }
    }
}