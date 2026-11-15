package io.opentrace.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}
