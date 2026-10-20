package io.opentrace.core;

import org.junit.jupiter.api.Test;
import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlingTest{
    @Test
    void traceWrapperMarksSpanAsErrorAndRethrows(){
        var tracer=tracer();
        try(var root=tracer.root("r")){
            assertThrows(IllegalStateException.class,
                ()->tracer.trace("boom",()->{throw new IllegalStateException("bad");}));
        }
        Span s=find(only(tracer),"boom");
        assertEquals(SpanStatus.ERROR,s.getStatus());
        assertEquals("IllegalStateException",s.getErrorType());
        assertEquals("bad",s.getErrorMessage());
    }

    @Test
    void supplierVariantReturnsValueAndMarksNoError(){
        var tracer=tracer();
        try(var root=tracer.root("r")){
            String v=tracer.trace("ok",()->"value");
            assertEquals("value",v);
        }
        assertNotEquals(SpanStatus.ERROR,find(only(tracer),"ok").getStatus());
    }

    @Test
    void spanScopeErrorMarksSpan(){
        var tracer=tracer();
        try(var root=tracer.root("r")){
            try(var s=tracer.span("risky")){
                s.error(new RuntimeException("simulated"));
            }
        }
        Span s=find(only(tracer),"risky");
        assertEquals(SpanStatus.ERROR,s.getStatus());
        assertEquals("simulated",s.getErrorMessage());
    }

    @Test
    void errorsShowUpInMetrics(){
        var tracer=tracer();
        try(var root=tracer.root("r")){
            try(var s=tracer.span("risky")){s.error(new RuntimeException("x"));}
        }
        assertEquals(1,tracer.getMetrics().getMetrics().get("risky").errors());
    }
}