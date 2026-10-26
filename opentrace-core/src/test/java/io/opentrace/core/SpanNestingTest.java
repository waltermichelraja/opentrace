package io.opentrace.core;

import org.junit.jupiter.api.Test;
import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class SpanNestingTest{
    @Test
    void childSpansPointToTheirParent(){
        var tracer=tracer();
        try(var root=tracer.root("order")){
            try(var a=tracer.span("validate")){
                try(var b=tracer.span("inner")){}
            }
            try(var c=tracer.span("process")){}
        }
        Trace t=only(tracer);
        Span order=find(t,"order"), validate=find(t,"validate"), inner=find(t,"inner"), process=find(t,"process");
        assertEquals(0,order.getParentSpanId());
        assertEquals(order.getSpanId(),validate.getParentSpanId());
        assertEquals(validate.getSpanId(),inner.getParentSpanId());
        assertEquals(order.getSpanId(),process.getParentSpanId());
        assertEquals(4,t.spans.size());
    }

    @Test
    void durationsAreMeasured() throws Exception{
        var tracer=tracer();
        try(var root=tracer.root("r")){
            try(var s=tracer.span("sleepy")){Thread.sleep(15);}
        }
        Span s=find(only(tracer),"sleepy");
        assertTrue(s.getDuration()>=10_000_000L,"duration was "+s.getDuration());
    }

    @Test
    void rootRejectsASecondActiveRoot(){
        var tracer=tracer();
        try(var root=tracer.root("a")){
            assertThrows(IllegalStateException.class,()->tracer.root("b"));
        }
    }

    @Test
    void traceIsUsableAgainAfterRootCloses(){
        var tracer=tracer();
        try(var r=tracer.root("one")){}
        try(var r=tracer.root("two")){}
        assertEquals(2,tracer.getTraceList().size());
    }

    @Test
    void attributesEventsAndLinksAreRecorded(){
        var tracer=tracer();
        try(var root=tracer.root("r")){
            try(var s=tracer.span("work")){
                s.setAttribute("userId",42).setAttribute("table","orders");
                s.addEvent("cache-miss");
                s.addLink(100,108);
            }
        }
        Span s=find(only(tracer),"work");
        assertEquals(42,s.getAttributes().get("userId"));
        assertEquals("orders",s.getAttributes().get("table"));
        assertEquals("cache-miss",s.getEvents().get(0).getName());
        assertEquals(100,s.getLinks().get(0).getTraceId());
        assertEquals(108,s.getLinks().get(0).getSpanId());
    }
}