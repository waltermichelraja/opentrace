package io.opentrace.core;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;

final class TraceState{
    final TraceId traceId;
    final ConcurrentLinkedQueue<Span> spans;
    final Deque<Span> stack=new ArrayDeque<>();
    final Map<String,String> baggage;

    TraceState(TraceId traceId,
               ConcurrentLinkedQueue<Span> spans, 
               Map<String,String> baggage){
        this.traceId=traceId;
        this.spans=spans;
        this.baggage=baggage;
    }
}