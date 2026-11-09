package io.opentrace.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class SpanTimestampTest{

    @Test
    void recordsEpochTimestampsForRegularSpans(){
        var tracer=tracer();
        Instant before=Instant.now();

        try(var root=tracer.root("root")){
            try(var child=tracer.span("child")){
                try{
                    Thread.sleep(5);
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
        }

        Instant after=Instant.now();

        Span root=find(only(tracer),"root");
        Span child=find(only(tracer),"child");

        long earliest=epochNanos(before);
        long latest=epochNanos(after);

        assertTrue(root.getStartEpochNanos()>=earliest);
        assertTrue(root.getStartEpochNanos()<=latest);
        assertTrue(child.getStartEpochNanos()>=earliest);
        assertTrue(child.getStartEpochNanos()<=latest);

        assertTrue(root.getEndEpochNanos()>=root.getStartEpochNanos());
        assertTrue(child.getEndEpochNanos()>=child.getStartEpochNanos());

        assertEquals(root.getDuration(),root.getEndEpochNanos()-root.getStartEpochNanos());
        assertEquals(child.getDuration(),child.getEndEpochNanos()-child.getStartEpochNanos());
    }

    @Test
    void recordsEpochTimestampsForRemotelyContinuedRoot(){
        var tracer=tracer();
        Instant before=Instant.now();

        OpenTraceContext incoming=new OpenTraceContext(
            TraceId.fromLong(777),
            42,
            Map.of()
        );

        try(var root=tracer.root("remote-root",incoming)){}

        Instant after=Instant.now();

        Span root=find(only(tracer),"remote-root");

        assertTrue(root.getStartEpochNanos()>=epochNanos(before));
        assertTrue(root.getStartEpochNanos()<=epochNanos(after));
        assertTrue(root.getEndEpochNanos()>=root.getStartEpochNanos());
        assertEquals(root.getDuration(),root.getEndEpochNanos()-root.getStartEpochNanos());
        assertEquals(42,root.getParentSpanId());
    }

    private static long epochNanos(Instant instant){
        return instant.getEpochSecond()*1_000_000_000L+instant.getNano();
    }
}