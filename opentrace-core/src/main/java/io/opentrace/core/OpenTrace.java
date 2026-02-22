package io.opentrace.core;

public final class OpenTrace{
    private final IdGenerator idGenerator=new IdGenerator();
    private final ThreadLocal<TraceState> current=new ThreadLocal<>();
    private final TraceBatcher batcher;

    OpenTrace(OpenTraceConfig config){this.batcher=new TraceBatcher(config);}

    public static OpenTraceBuilder builder(){return new OpenTraceBuilder();}

    public void startRoot(String name){
        TraceState state=new TraceState();
        state.traceId=idGenerator.next();
        current.set(state);
        startSpan(name);
    }

    public void startSpan(String name){
        TraceState state=current.get();
        if(state==null){return;}
        Span span=new Span();
        span.spanId=idGenerator.next();
        span.parentSpanId=state.stack.isEmpty()?0:state.stack.peek().spanId;
        span.startTime=System.nanoTime();
        span.nameId=name.hashCode();
        state.stack.push(span);
    }

    public void endSpan(){
        TraceState state=current.get();
        if(state==null || state.stack.isEmpty()){return;}
        Span span=state.stack.pop();
        span.duration=System.nanoTime()-span.startTime;
        state.spans.add(span);
    }

    public void endRoot(){
        TraceState state=current.get();
        if(state==null){return;}
        while(!state.stack.isEmpty()){endSpan();}
        Trace trace=new Trace(state.traceId,state.spans);
        batcher.submit(trace);
        current.remove();
    }

    public void shutdown(){batcher.shutdown();}
}