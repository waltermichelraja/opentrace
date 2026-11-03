package io.opentrace.core;

import io.opentrace.core.exporter.SpanExporter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class ExportStatsTest{
    private static final class BlockingExporter implements SpanExporter{
        final CountDownLatch entered=new CountDownLatch(1);
        final CountDownLatch release=new CountDownLatch(1);
        @Override
        public void export(List<Trace> traces){
            entered.countDown();
            try{release.await(10,TimeUnit.SECONDS);}
            catch(InterruptedException e){Thread.currentThread().interrupt();}
        }
    }

    @Test
    void droppedTracesAreCountedWhenTheQueueIsFull() throws Exception{
        var blocker=new BlockingExporter();
        var tracer=OpenTrace.builder().exporter(blocker).queueCapacity(2).batchSize(1).build();
        try{
            try(var r=tracer.root("first")){}                 // the worker takes it and blocks in export()
            assertTrue(blocker.entered.await(5,TimeUnit.SECONDS));
            for(int i=0;i<10;i++){try(var r=tracer.root("r"+i)){}}   // 2 fit in the queue, 8 are dropped
            assertEquals(8,tracer.getExportStats().dropped());
        }finally{
            blocker.release.countDown();
        }
        tracer.shutdown();
        var stats=tracer.getExportStats();
        assertEquals(3,stats.exported());                      // 1 in flight + 2 queued
        assertEquals(8,stats.dropped());
        assertEquals(0,stats.failed());
    }

    @Test
    void exportedTracesAreCounted(){
        var exp=new CapturingExporter();
        var tracer=OpenTrace.builder().exporter(exp).build();
        for(int i=0;i<20;i++){try(var r=tracer.root("r")){}}
        tracer.shutdown();
        var stats=tracer.getExportStats();
        assertEquals(20,stats.exported());
        assertEquals(0,stats.dropped());
        assertEquals(0,stats.failed());
    }

    @Test
    void tracesInFailedBatchesAreCountedAsFailed(){
        SpanExporter alwaysFails=traces->{throw new RuntimeException("collector down");};
        var tracer=OpenTrace.builder().exporter(alwaysFails).batchSize(1).build();
        for(int i=0;i<3;i++){try(var r=tracer.root("r")){}}
        tracer.shutdown();
        var stats=tracer.getExportStats();
        assertEquals(3,stats.failed());
        assertEquals(0,stats.exported());
    }

    @Test
    void statsAreZeroWithoutAnExporter(){
        var tracer=tracer();
        try(var r=tracer.root("r")){}
        var stats=tracer.getExportStats();
        assertEquals(0,stats.exported());
        assertEquals(0,stats.dropped());
        assertEquals(0,stats.failed());
    }

    @Test
    void dropWarningsAreRateLimited() throws Exception{
        var blocker=new BlockingExporter();
        var tracer=OpenTrace.builder().exporter(blocker).queueCapacity(1).batchSize(1).build();
        var captured=new ByteArrayOutputStream();
        PrintStream old=System.err;
        System.setErr(new PrintStream(captured));
        try{
            try(var r=tracer.root("first")){}
            assertTrue(blocker.entered.await(5,TimeUnit.SECONDS));
            for(int i=0;i<5000;i++){try(var r=tracer.root("r")){}}
        }finally{
            System.setErr(old);
            blocker.release.countDown();
        }
        tracer.shutdown();
        long warnings=captured.toString().lines().filter(l->l.contains("export queue full")).count();
        assertTrue(tracer.getExportStats().dropped()>4000);
        assertTrue(warnings>=1 && warnings<=8,"warnings="+warnings);
    }
}