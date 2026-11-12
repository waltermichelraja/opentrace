package io.opentrace.core;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

final class TraceState{
    final TraceId traceId;
    final ConcurrentLinkedQueue<Span> spans;
    final Deque<Span> stack=new ArrayDeque<>();
    final Map<String,String> baggage;
    final AtomicInteger pendingAsyncSpans=new AtomicInteger();
    final AtomicBoolean completed=new AtomicBoolean();
    volatile boolean rootEnded;

    TraceState(
        TraceId traceId,
        ConcurrentLinkedQueue<Span> spans,
        Map<String,String> baggage
    ){
        this.traceId=traceId;
        this.spans=spans;
        this.baggage=baggage;
    }
}