package io.opentrace.core;

import io.opentrace.core.sampling.Sampler;

public final class OpenTrace{
    private final IdGenerator idGenerator=new IdGenerator();
    private final NameRegistry nameRegistry=new NameRegistry();
    private final ThreadLocal<TraceState> current=new ThreadLocal<>();
    private final TraceBatcher batcher;
    private final Sampler sampler;
    private final String serviceName;
    private final String environment;
    private final String serviceVersion;

    OpenTrace(OpenTraceConfig config){
        this.batcher=new TraceBatcher(config);
        this.sampler=config.sampler;
        this.serviceName=config.serviceName;
        this.environment=config.environment;
        this.serviceVersion=config.serviceVersion;
    }

    public static OpenTraceBuilder builder(){
        return new OpenTraceBuilder();
    }

    public void startRoot(String name){
        if(!sampler.shouldSample()){
            current.remove();
            return;
        }
        TraceState state=new TraceState();
        state.traceId=idGenerator.next();
        current.set(state);
        startSpan(name);
    }

    public Span startSpan(String name){
        TraceState state=current.get();
        if(state==null){return null;}
        Span span=new Span();
        span.spanId=idGenerator.next();
        span.parentSpanId=state.stack.isEmpty()?0:state.stack.peek().spanId;
        span.startTime=System.nanoTime();
        span.nameId=nameRegistry.id(name);
        state.stack.push(span);
        return span;
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
        Trace trace=new Trace(state.traceId, state.spans, nameRegistry, serviceName, environment, serviceVersion);
        batcher.submit(trace);
        current.remove();
    }

    public void shutdown(){
        batcher.shutdown();
    }

    public SpanScope span(String name){
        Span span=startSpan(name);
        return new SpanScope(this, span);
    }

    public RootScope root(String name){
        if(current.get()!=null){throw new IllegalStateException("root span already active in this thread");}
        startRoot(name);
        return new RootScope(this);
    }

    public RootScope root(String name, OpenTraceContext incoming){
        if(current.get()!=null){throw new IllegalStateException("root span already active in this thread");}
        if(incoming==null){
            startRoot(name);
            return new RootScope(this);
        }
        if(!sampler.shouldSample()){
            current.remove();
            return new RootScope(this);
        }
        TraceState state=new TraceState();
        state.traceId=incoming.getTraceId();
        current.set(state);
        Span span=new Span();
        span.spanId=idGenerator.next();
        span.parentSpanId=incoming.getParentSpanId();
        span.startTime=System.nanoTime();
        span.nameId=nameRegistry.id(name);
        state.stack.push(span);
        return new RootScope(this);
    }

    public void trace(String name, Runnable block){
        Span span=startSpan(name);
        try{
            block.run();
        }catch(Throwable t){
            if(span!=null){
                span.status=SpanStatus.ERROR;
                span.errorType=t.getClass().getSimpleName();
                span.errorMessage=t.getMessage();
            }
            throw t;
        }finally{endSpan();}
    }

    public <T> T trace(String name, java.util.function.Supplier<T> block){
        Span span=startSpan(name);
        try{
            return block.get();
        }catch(Throwable t){
            if(span!=null){
                span.status=SpanStatus.ERROR;
                span.errorType=t.getClass().getSimpleName();
                span.errorMessage=t.getMessage();
            }
            throw t;
        }finally{endSpan();}
    }

    public OpenTraceContext currentContext(){
        TraceState state=current.get();
        if(state==null){return null;}
        long parentSpanId=state.stack.isEmpty()?0:state.stack.peek().spanId;
        return new OpenTraceContext(state.traceId,parentSpanId);
    }

    public java.util.Map<String,String> inject(OpenTraceContext ctx){
        if(ctx==null){return java.util.Collections.emptyMap();}
        java.util.Map<String,String> map=new java.util.HashMap<>();
        map.put(
            io.opentrace.core.propagation.OpenTraceHeaders.TRACE_ID,
            String.valueOf(ctx.getTraceId()));
        map.put(
            io.opentrace.core.propagation.OpenTraceHeaders.PARENT_ID,
            String.valueOf(ctx.getParentSpanId()));
        return map;
    }

    public OpenTraceContext extract(java.util.Map<String, String> headers){
    if(headers==null){return null;}
    String traceId=headers.get(
        io.opentrace.core.propagation.OpenTraceHeaders.TRACE_ID);
    String parentId=headers.get(
        io.opentrace.core.propagation.OpenTraceHeaders.PARENT_ID);
    if(traceId==null){return null;}
    long tid=Long.parseLong(traceId);
    long pid=parentId==null?0:Long.parseLong(parentId);
    return new OpenTraceContext(tid, pid);
}
}