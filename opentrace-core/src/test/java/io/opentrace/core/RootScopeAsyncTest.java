package io.opentrace.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RootScopeAsyncTest{

    @Test
    void rootScopeCanBeResumedAndClosedAcrossThreads()throws InterruptedException{
        OpenTrace tracer=OpenTrace.builder().build();
        RootScope root=tracer.root("http.server");

        root.detach();

        Thread dispatch=new Thread(()->{
            root.makeCurrent();
            try(SpanScope child=tracer.span("request.handler")){
                child.setAttribute("handler.name","test");
            }finally{
                root.detach();
            }
        });

        dispatch.start();
        dispatch.join();
        root.close();

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
    }
}