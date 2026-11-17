package io.opentrace.core;

import io.opentrace.core.TestSupport.CapturingExporter;
import io.opentrace.core.processor.BatchSpanProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BatchSpanProcessorLifecycleTest{

    private Trace createTrace(){
        var tracer=OpenTrace.builder()
            .disableExporter()
            .build();
        try{
            try(var root=tracer.root("root")){}
            return tracer.getTraceList().get(0);
        }finally{
            tracer.shutdown();
        }
    }

    @Test
    void shutdownCanBeCalledMoreThanOnce(){
        var exporter=new CapturingExporter();
        var processor=new BatchSpanProcessor(10,10,exporter);
        processor.onEnd(createTrace());
        processor.shutdown();
        processor.shutdown();
        assertEquals(1,exporter.received.size());
        assertEquals(1,processor.stats().exported());
    }

    @Test
    void tracesSubmittedAfterShutdownAreRejected(){
        var exporter=new CapturingExporter();
        var processor=new BatchSpanProcessor(10,10,exporter);
        processor.shutdown();
        processor.onEnd(createTrace());
        assertEquals(0,exporter.received.size());
        assertEquals(1,processor.stats().dropped());
        assertEquals(0,processor.stats().exported());
    }

    @Test
    void shutdownFlushesQueuedTraces(){
        var exporter=new CapturingExporter();
        var processor=new BatchSpanProcessor(100,100,exporter);
        for(int i=0;i<20;i++){
            processor.onEnd(createTrace());
        }
        processor.shutdown();
        assertEquals(20,exporter.received.size());
        assertEquals(20,processor.stats().exported());
        assertEquals(0,processor.stats().dropped());
    }
}
