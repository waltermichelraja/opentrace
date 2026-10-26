package io.opentrace.core;

import io.opentrace.core.TestSupport.CapturingExporter;
import io.opentrace.core.processor.*;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class ExportPipelineTest{
    @Test
    void everyTraceIsExportedExactlyOnce(){
        var exp=new CapturingExporter();
        var tracer=OpenTrace.builder().exporter(exp).build();
        for(int i=0;i<50;i++){try(var r=tracer.root("r"+i)){}}
        tracer.shutdown();
        assertEquals(50,exp.received.size());
        assertEquals(50,exp.received.stream().map(t->t.traceId).distinct().count());
    }

    @Test
    void shutdownFlushesEverythingStillQueued(){
        var exp=new CapturingExporter();
        var tracer=OpenTrace.builder().exporter(exp).batchSize(1000).build();
        for(int i=0;i<500;i++){try(var r=tracer.root("r")){}}
        tracer.shutdown();
        assertEquals(500,exp.received.size());
    }

    @Test
    void aFailingExporterDoesNotStopLaterExports(){
        var calls=new AtomicInteger();
        var good=new CapturingExporter();
        io.opentrace.core.exporter.SpanExporter flaky=traces->{
            if(calls.getAndIncrement()==0){throw new RuntimeException("boom");}
            good.export(traces);
        };
        var tracer=OpenTrace.builder().exporter(flaky).batchSize(1).build();
        try(var r=tracer.root("first")){}
        try{Thread.sleep(300);}catch(InterruptedException ignored){}
        try(var r=tracer.root("second")){}
        tracer.shutdown();
        assertEquals(1,good.received.size());
    }

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