package io.opentrace.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class AsyncPropagationTest{
    @Test
    void wrappedRunnableAttachesToCallerSpan(){
        var tracer=tracer();
        try(var root=tracer.root("r")){
            try(var s=tracer.span("parent")){
                CompletableFuture.runAsync(tracer.wrap(()->{
                    try(var c=tracer.span("async-child")){}
                })).join();
            }
        }
        Trace t=only(tracer);
        assertEquals(find(t,"parent").getSpanId(),find(t,"async-child").getParentSpanId());
    }

    @Test
    void wrappedSupplierReturnsValueAndPropagates(){
        var tracer=tracer();
        try(var root=tracer.root("r")){
            int v=CompletableFuture.supplyAsync(tracer.wrap(()->{
                try(var c=tracer.span("calc")){return 7;}
            })).join();
            assertEquals(7,v);
        }
        Trace t=only(tracer);
        assertEquals(find(t,"r").getSpanId(),find(t,"calc").getParentSpanId());
    }

    @Test
    void wrappedExecutorServicePropagatesContext() throws Exception{
        var tracer=tracer();
        ExecutorService pool=Executors.newFixedThreadPool(2);
        ExecutorService wrapped=tracer.wrapExecutorService(pool);
        try(var root=tracer.root("r")){
            wrapped.submit(()->{try(var s=tracer.span("pooled")){}}).get();
        }
        pool.shutdown();
        assertEquals(find(only(tracer),"r").getSpanId(),find(only(tracer),"pooled").getParentSpanId());
    }

    @Test
    void threadStateDoesNotLeakIntoPoolThreads() throws Exception{
        var tracer=tracer();
        ExecutorService pool=Executors.newSingleThreadExecutor();
        try(var root=tracer.root("r")){
            pool.submit(tracer.wrap(()->{})).get();
        }
        // a plain task on the same pool thread must see no active trace
        Object ctx=pool.submit(()->tracer.currentContext()).get();
        pool.shutdown();
        assertNull(ctx);
    }

    @Test
    void wrapWithoutActiveTraceJustRunsTheTask(){
        var tracer=tracer();
        int[] ran={0};
        tracer.wrap((Runnable)()->{ran[0]++;}).run();
        assertEquals(1,ran[0]);
    }

    @Test
    void concurrentChildrenAreAllRecorded() throws Exception{
        var tracer=tracer();
        ExecutorService pool=Executors.newFixedThreadPool(8);
        try(var root=tracer.root("r")){
            var futures=new java.util.ArrayList<CompletableFuture<Void>>();
            for(int i=0;i<200;i++){
                futures.add(CompletableFuture.runAsync(tracer.wrap(()->{
                    try(var s=tracer.span("w")){s.setAttribute("k","v");}
                }),pool));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
        pool.shutdown();
        assertEquals(201,only(tracer).spans.size());
    }
}