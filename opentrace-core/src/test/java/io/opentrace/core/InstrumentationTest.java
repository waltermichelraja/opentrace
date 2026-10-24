package io.opentrace.core;

import io.opentrace.core.instrumentation.SelfAware;
import io.opentrace.core.instrumentation.Trace;
import io.opentrace.core.instrumentation.TracingProxy;
import org.junit.jupiter.api.Test;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class InstrumentationTest{
    interface Plain{ void a(); }

    @Trace
    interface Traced{ void validate(); void process(); int add(int x,int y); void fail(); }

    static class Impl implements Traced, SelfAware<Traced>{
        Traced self;
        public void setSelf(Traced s){self=s;}
        public void validate(){self.process();}
        public void process(){}
        public int add(int x,int y){return x+y;}
        public void fail(){throw new IllegalArgumentException("nope");}
    }

    @Test
    void annotatedInterfaceMethodsGetSpansAndSelfCallsNest(){
        var tracer=tracer();
        Traced svc=TracingProxy.create(new Impl(),tracer,Traced.class);
        try(var r=tracer.root("r")){svc.validate();}
        io.opentrace.core.Trace t=only(tracer);
        assertEquals(find(t,"validate").getSpanId(),find(t,"process").getParentSpanId());
    }

    @Test
    void returnValuesPassThrough(){
        var tracer=tracer();
        Traced svc=TracingProxy.create(new Impl(),tracer,Traced.class);
        try(var r=tracer.root("r")){assertEquals(5,svc.add(2,3));}
    }

    @Test
    void originalExceptionTypeIsPreservedAndSpanMarkedError(){
        var tracer=tracer();
        Traced svc=TracingProxy.create(new Impl(),tracer,Traced.class);
        try(var r=tracer.root("r")){
            assertThrows(IllegalArgumentException.class,svc::fail);
        }
        assertEquals(SpanStatus.ERROR,find(only(tracer),"fail").getStatus());
    }

    @Test
    void unannotatedTypesAreNotTraced(){
        var tracer=tracer();
        Plain p=TracingProxy.create(()->{},tracer,Plain.class);
        try(var r=tracer.root("r")){p.a();}
        assertEquals(1,only(tracer).spans.size());
    }

    @Test
    void proxyOutsideAnActiveTraceDoesNothingHarmful(){
        var tracer=tracer();
        Traced svc=TracingProxy.create(new Impl(),tracer,Traced.class);
        assertEquals(3,svc.add(1,2));
        assertTrue(tracer.getTraceList().isEmpty());
    }
}