package io.opentrace.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class SpanEventTimestampTest{

    @Test
    void recordsEventTimestampInEpochNanoseconds() throws Exception{
        var tracer=tracer();
        Instant before=Instant.now();

        try(var root=tracer.root("root")){
            try(var span=tracer.span("work")){
                Thread.sleep(5);
                span.addEvent("cache-miss");
            }
        }

        Instant after=Instant.now();

        Span span=find(only(tracer),"work");
        SpanEvent event=span.getEvents().get(0);

        long earliest=epochNanos(before);
        long latest=epochNanos(after);

        assertTrue(event.getTimestamp()>=earliest);
        assertTrue(event.getTimestamp()<=latest);

        assertTrue(event.getTimestamp()>=span.getStartEpochNanos());
        assertTrue(event.getTimestamp()<=span.getEndEpochNanos());
    }

    @Test
    void serializesEventTimestampAsEpochNanoseconds(){
        var tracer=tracer();

        try(var root=tracer.root("root")){
            try(var span=tracer.span("work")){
                span.addEvent("cache-miss");
            }
        }

        Trace trace=only(tracer);
        Span span=find(trace,"work");
        long timestamp=span.getEvents().get(0).getTimestamp();

        assertTrue(trace.toJson().contains(
            "\"timestampNs\":"+timestamp
        ));
    }

    private static long epochNanos(Instant instant){
        return instant.getEpochSecond()*1_000_000_000L+instant.getNano();
    }
}