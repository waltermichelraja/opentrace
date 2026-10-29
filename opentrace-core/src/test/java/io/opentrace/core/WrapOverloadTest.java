package io.opentrace.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class WrapOverloadTest{
    @Test
    void expressionLambdaThatReturnsAValueCanBeWrappedAsARunnable(){
        var tracer=tracer();
        var counter=new AtomicInteger();
        try(var root=tracer.root("r")){
            CompletableFuture.runAsync(tracer.wrap(()->counter.incrementAndGet())).join();
        }
        assertEquals(1,counter.get());
    }

    @Test
    void collectionAddStyleLambdaIsAccepted(){
        var tracer=tracer();
        List<String> list=new ArrayList<>();
        try(var root=tracer.root("r")){
            CompletableFuture.runAsync(tracer.wrap(()->list.add("x"))).join();
        }
        assertEquals(List.of("x"),list);
    }

    @Test
    void wrapSupplierReturnsTheValueAndPropagatesContext(){
        var tracer=tracer();
        try(var root=tracer.root("r")){
            int v=CompletableFuture.supplyAsync(tracer.wrapSupplier(()->{
                try(var c=tracer.span("calc")){return 7;}
            })).join();
            assertEquals(7,v);
        }
        Trace t=only(tracer);
        assertEquals(find(t,"r").getSpanId(),find(t,"calc").getParentSpanId());
    }

    @Test
    void wrapSupplierWithoutAnActiveTraceJustRuns(){
        var tracer=tracer();
        assertEquals(5,tracer.wrapSupplier(()->5).get());
    }
}