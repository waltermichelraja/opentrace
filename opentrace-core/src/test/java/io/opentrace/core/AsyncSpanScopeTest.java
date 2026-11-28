package io.opentrace.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class AsyncSpanScopeTest{

    @Test
    void asyncSpanKeepsTraceOpenAfterRootCloses(){
        OpenTrace tracer=OpenTrace.builder().build();
        AsyncSpanScope asyncSpan;
        try(var root=tracer.root("root")){
            asyncSpan=tracer.asyncSpan("http.client");
        }
        assertTrue(tracer.getTraceList().isEmpty());
        asyncSpan.close();
        assertEquals(1,tracer.getTraceList().size());
        Trace trace=tracer.getTraceList().get(0);
        assertEquals(2,trace.spans.size());
        Span rootSpan=trace.spans.stream()
            .filter(span->trace.resolveName(span.getNameId()).equals("root"))
            .findFirst()
            .orElseThrow();
        Span clientSpan=trace.spans.stream()
            .filter(span->trace.resolveName(span.getNameId()).equals("http.client"))
            .findFirst()
            .orElseThrow();
        assertEquals(rootSpan.getSpanId(),clientSpan.getParentSpanId());
        tracer.shutdown();
    }

    @Test
    void asyncSpanCanCloseBeforeRoot(){
        OpenTrace tracer=OpenTrace.builder().build();
        try(var root=tracer.root("root")){
            try(var asyncSpan=tracer.asyncSpan("http.client")){
                asyncSpan.setAttribute("http.method","GET");
            }
            assertTrue(tracer.getTraceList().isEmpty());
        }
        assertEquals(1,tracer.getTraceList().size());
        assertEquals(2,tracer.getTraceList().get(0).spans.size());
        tracer.shutdown();
    }

    @Test
    void asyncSpanErrorIsRecorded(){
        OpenTrace tracer=OpenTrace.builder().build();
        AsyncSpanScope asyncSpan;
        try(var root=tracer.root("root")){
            asyncSpan=tracer.asyncSpan("http.client");
            asyncSpan.error(new IllegalStateException("request failed"));
        }
        asyncSpan.close();
        Trace trace=tracer.getTraceList().get(0);
        Span span=trace.spans.stream()
            .filter(item->trace.resolveName(item.getNameId()).equals("http.client"))
            .findFirst()
            .orElseThrow();
        assertEquals(SpanStatus.ERROR,span.getStatus());
        assertEquals("IllegalStateException",span.getErrorType());
        assertEquals("request failed",span.getErrorMessage());
        tracer.shutdown();
    }

    @Test
    void closingAsyncSpanMoreThanOnceDoesNotDuplicateIt(){
        OpenTrace tracer=OpenTrace.builder().build();
        AsyncSpanScope asyncSpan;
        try(var root=tracer.root("root")){
            asyncSpan=tracer.asyncSpan("http.client");
        }
        asyncSpan.close();
        asyncSpan.close();
        assertEquals(1,tracer.getTraceList().size());
        assertEquals(2,tracer.getTraceList().get(0).spans.size());
        tracer.shutdown();
    }

    @Test
    void rootCloseOverlappingWorkerChildScopeDoesNotLoseOrDuplicateSpans()throws InterruptedException{
        OpenTrace tracer=OpenTrace.builder().build();
        RootScope root=tracer.root("http.server");
        root.detach();
        CountDownLatch childStarted=new CountDownLatch(1);
        CountDownLatch allowChildClose=new CountDownLatch(1);
        Thread worker=new Thread(()->{
            root.makeCurrent();
            SpanScope child=tracer.span("request.handler");
            child.setAttribute("handler.name","test");
            childStarted.countDown();
            try{
                allowChildClose.await();
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
            }finally{
                child.close();
                root.detach();
            }
        });
        worker.start();
        assertTrue(childStarted.await(5,TimeUnit.SECONDS));
        root.close();
        allowChildClose.countDown();
        worker.join(5000);
        assertFalse(worker.isAlive());
        assertEquals(1,tracer.getTraceList().size());
        Trace trace=tracer.getTraceList().get(0);
        assertEquals(2,trace.spans.size());
        Span rootSpan=trace.getRoot();
        assertNotNull(rootSpan);
        Span child=trace.spans.stream()
            .filter(span->span.getParentSpanId()==rootSpan.getSpanId())
            .findFirst()
            .orElseThrow();
        assertEquals("request.handler",trace.resolveName(child.getNameId()));
        assertEquals("test",child.getAttributes().get("handler.name"));
        assertEquals(
            2,
            trace.spans.stream()
                .map(Span::getSpanId)
                .distinct()
                .count()
        );
    }

    @Test
    void concurrentRootAndAsyncSpanCompletionProducesSingleCompleteTrace()throws InterruptedException{
        for(int iteration=0;iteration<50;iteration++){
            OpenTrace tracer=OpenTrace.builder().build();
            RootScope root=tracer.root("root");
            AsyncSpanScope async1=tracer.asyncSpan("async.one");
            AsyncSpanScope async2=tracer.asyncSpan("async.two");
            CountDownLatch ready=new CountDownLatch(3);
            CountDownLatch start=new CountDownLatch(1);
            Thread rootCloser=new Thread(()->{
                ready.countDown();
                try{
                    start.await();
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    return;
                }
                root.close();
            });
            Thread asyncCloser1=new Thread(()->{
                ready.countDown();
                try{
                    start.await();
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    return;
                }
                async1.close();
            });
            Thread asyncCloser2=new Thread(()->{
                ready.countDown();
                try{
                    start.await();
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    return;
                }
                async2.close();
            });
            rootCloser.start();
            asyncCloser1.start();
            asyncCloser2.start();
            assertTrue(ready.await(5,TimeUnit.SECONDS));
            start.countDown();
            rootCloser.join(5000);
            asyncCloser1.join(5000);
            asyncCloser2.join(5000);
            assertFalse(rootCloser.isAlive());
            assertFalse(asyncCloser1.isAlive());
            assertFalse(asyncCloser2.isAlive());
            assertEquals(1,tracer.getTraceList().size());
            Trace trace=tracer.getTraceList().get(0);
            assertEquals(3,trace.spans.size());
            assertEquals(
                3,
                trace.spans.stream()
                    .map(Span::getSpanId)
                    .distinct()
                    .count()
            );
            Span rootSpan=trace.getRoot();
            assertNotNull(rootSpan);
            assertEquals(
                2,
                trace.spans.stream()
                    .filter(span->span.getParentSpanId()==rootSpan.getSpanId())
                    .count()
            );
            tracer.shutdown();
        }
    }
}