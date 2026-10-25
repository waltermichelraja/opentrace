package io.opentrace.core;

import io.opentrace.core.processor.*;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class ExportPipelineTest{
    @Test
    void disabledExporterNeverReceivesTraces(){
        var tracer=OpenTrace.builder().disableExporter().build();
        try(var r=tracer.root("r")){}
        tracer.shutdown();
        assertEquals(1,tracer.getTraceList().size());
    }

    @Test
    void customProcessorsAreCalledOncePerTrace(){
        var tracer=tracer();
        var calls=new AtomicInteger();
        tracer.addProcessor(t->calls.incrementAndGet());
        for(int i=0;i<5;i++){try(var r=tracer.root("r")){}}
        assertEquals(5,calls.get());
    }

    @Test
    void simpleProcessorExportsImmediately(){
        var exp=new CapturingExporter();
        var tracer=tracer();
        tracer.addProcessor(new SimpleSpanProcessor(exp));
        try(var r=tracer.root("r")){}
        assertEquals(1,exp.received.size());
    }

    @Test
    void addingProcessorsWhileTracingIsSafe() throws Exception{
        var tracer=tracer();
        var stop=new java.util.concurrent.atomic.AtomicBoolean();
        var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread producer=new Thread(()->{
            try{while(!stop.get()){try(var r=tracer.root("r")){}}}
            catch(Throwable t){failure.set(t);}
        });
        producer.start();
        for(int i=0;i<200;i++){tracer.addProcessor(t->{});}
        stop.set(true);
        producer.join();
        assertNull(failure.get());
    }

    @Test
    void exporterBuilderGuardsAgainstDisabledState(){
        var b=OpenTrace.builder().disableExporter();
        assertThrows(IllegalStateException.class,()->b.exporter(new CapturingExporter()));
    }
}