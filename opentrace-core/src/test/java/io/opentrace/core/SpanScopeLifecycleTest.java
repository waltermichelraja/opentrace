package io.opentrace.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpanScopeLifecycleTest{

    @Test
    void closingScopeMoreThanOnceEndsSpanOnlyOnce(){
        var tracer=OpenTrace.builder()
            .disableExporter()
            .build();
        try{
            try(var root=tracer.root("root")){
                var scope=tracer.span("child");
                scope.close();
                scope.close();
                try(var next=tracer.span("next")){}
            }
            Trace trace=tracer.getTraceList().get(0);
            assertEquals(3,trace.spans.size());
            assertEquals("root",trace.resolveName(trace.getRoot().getNameId()));
            assertEquals(2,trace.spans.stream()
                .filter(span->span.getParentSpanId()==trace.getRoot().getSpanId())
                .count());
        }finally{
            tracer.shutdown();
        }
    }

    @Test
    void closedScopeRejectsFurtherMutations(){
        var tracer=OpenTrace.builder()
            .disableExporter()
            .build();
        try{
            try(var root=tracer.root("root")){
                var scope=tracer.span("child");
                scope.setAttribute("before","value");
                scope.addEvent("before-close");
                scope.close();
                scope.setAttribute("after","ignored");
                scope.addEvent("after-close");
                scope.addLink(TraceId.fromLong(2),10L);
                scope.error(new IllegalStateException("after-close"));
            }
            Trace trace=tracer.getTraceList().get(0);
            Span child=trace.spans.stream()
                .filter(span->"child".equals(trace.resolveName(span.getNameId())))
                .findFirst()
                .orElseThrow();

            assertEquals(Map.of("before","value"),child.getAttributes());
            assertEquals(1,child.getEvents().size());
            assertTrue(child.getLinks().isEmpty());
            assertEquals(SpanStatus.UNSET,child.getStatus());
        }finally{
            tracer.shutdown();
        }
    }

    @Test
    void closingAnUnsampledScopeIsSafe(){
        var tracer=OpenTrace.builder()
            .samplingRate(0.0)
            .disableExporter()
            .build();
        try{
            var scope=tracer.span("ignored");
            assertDoesNotThrow(scope::close);
            assertDoesNotThrow(scope::close);
            assertTrue(tracer.getTraceList().isEmpty());
        }finally{
            tracer.shutdown();
        }
    }
}