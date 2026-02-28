package io.opentrace.core;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

final class TraceState{
    long traceId;

    final Deque<Span> stack=new ArrayDeque<>();
    final Queue<Span> spans;

    TraceState(long traceId, Queue<Span> sharedSpans){
        this.traceId=traceId;
        this.spans=sharedSpans;
    }
}