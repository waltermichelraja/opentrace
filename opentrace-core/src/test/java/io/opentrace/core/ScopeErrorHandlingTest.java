package io.opentrace.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScopeErrorHandlingTest{

    @Test
    void nullErrorsAreIgnoredByAllScopeTypes(){
        var tracer=OpenTrace.builder()
            .disableExporter()
            .build();
        try{
            try(var root=tracer.root("root")){
                assertDoesNotThrow(()->root.error(null));
                try(var span=tracer.span("child")){
                    assertDoesNotThrow(()->span.error(null));
                }
                try(var async=tracer.asyncSpan("async")){
                    assertDoesNotThrow(()->async.error(null));
                }
            }
            Trace trace=tracer.getTraceList().get(0);
            assertTrue(trace.spans.stream()
                .allMatch(span->span.getStatus()==SpanStatus.UNSET));
        }finally{
            tracer.shutdown();
        }
    }

    @Test
    void asyncScopeStillRecordsNonNullErrors(){
        var tracer=OpenTrace.builder()
            .disableExporter()
            .build();
        try{
            try(var root=tracer.root("root")){
                try(var async=tracer.asyncSpan("async")){
                    async.error(new IllegalStateException("async failure"));
                }
            }
            Trace trace=tracer.getTraceList().get(0);
            Span asyncSpan=trace.spans.stream()
                .filter(span->"async".equals(trace.resolveName(span.getNameId())))
                .findFirst()
                .orElseThrow();
            assertEquals(SpanStatus.ERROR,asyncSpan.getStatus());
            assertEquals("IllegalStateException",asyncSpan.getErrorType());
            assertEquals("async failure",asyncSpan.getErrorMessage());
        }finally{
            tracer.shutdown();
        }
    }
}