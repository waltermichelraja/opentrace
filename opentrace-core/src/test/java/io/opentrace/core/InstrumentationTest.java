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

    interface MethodAnnotatedService{
        @Trace("interface.custom")
        void execute();
    }

    static class MethodAnnotatedServiceImpl implements MethodAnnotatedService{
        public void execute(){}
    }

    interface ClassAnnotatedService{
        void defaultName();
        void customName();
    }

    @Trace
    static class ClassAnnotatedServiceImpl implements ClassAnnotatedService{
        public void defaultName(){}

        @Trace("custom.operation")
        public void customName(){}
    }

    interface AnnotationPrecedenceService{
        @Trace("interface.operation")
        void execute();
    }

    static class AnnotationPrecedenceServiceImpl implements AnnotationPrecedenceService{
        @Trace("implementation.operation")
        public void execute(){}
    }

    @Trace
    interface CheckedExceptionService{
        void execute()throws java.io.IOException;
    }

    static class CheckedExceptionServiceImpl implements CheckedExceptionService{
        public void execute()throws java.io.IOException{
            throw new java.io.IOException("disk unavailable");
        }
    }

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

    @Test
    void interfaceMethodAnnotationUsesCustomSpanName(){
        var tracer=tracer();
        MethodAnnotatedService svc=TracingProxy.create(
            new MethodAnnotatedServiceImpl(),
            tracer,
            MethodAnnotatedService.class
        );
        try(var r=tracer.root("root")){svc.execute();}
        assertNotNull(find(only(tracer),"interface.custom"));
    }

    @Test
    void implementationClassAnnotationTracesPublicInterfaceMethods(){
        var tracer=tracer();
        ClassAnnotatedService svc=TracingProxy.create(
            new ClassAnnotatedServiceImpl(),
            tracer,
            ClassAnnotatedService.class
        );
        try(var r=tracer.root("root")){
            svc.defaultName();
            svc.customName();
        }
        io.opentrace.core.Trace trace=only(tracer);
        assertNotNull(find(trace,"defaultName"));
        assertNotNull(find(trace,"custom.operation"));
    }

    @Test
    void implementationMethodAnnotationTakesPrecedence(){
        var tracer=tracer();
        AnnotationPrecedenceService svc=TracingProxy.create(
            new AnnotationPrecedenceServiceImpl(),
            tracer,
            AnnotationPrecedenceService.class
        );
        try(var r=tracer.root("root")){svc.execute();}
        io.opentrace.core.Trace trace=only(tracer);
        assertNotNull(find(trace,"implementation.operation"));
    }

    @Test
    void checkedExceptionTypeIsPreservedAndSpanMarkedError(){
        var tracer=tracer();
        CheckedExceptionService svc=TracingProxy.create(
            new CheckedExceptionServiceImpl(),
            tracer,
            CheckedExceptionService.class
        );
        try(var r=tracer.root("root")){
            java.io.IOException exception=assertThrows(
                java.io.IOException.class,
                svc::execute
            );
            assertEquals("disk unavailable",exception.getMessage());
        }
        assertEquals(SpanStatus.ERROR,find(only(tracer),"execute").getStatus());
    }
}