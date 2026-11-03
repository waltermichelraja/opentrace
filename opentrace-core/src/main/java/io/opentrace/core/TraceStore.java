package io.opentrace.core;

import io.opentrace.core.metrics.SpanMetricsCollector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

final class TraceStore{
    private final ArrayDeque<Trace> completedTraces=new ArrayDeque<>();
    private final int maxTraces;
    private final SpanMetricsCollector metrics=new SpanMetricsCollector();

    TraceStore(int maxTraces){
        this.maxTraces=maxTraces;
    }

    void record(Trace trace){
        metrics.record(trace);
        if(maxTraces==0){return;}
        synchronized(completedTraces){
            if(completedTraces.size()>=maxTraces){completedTraces.removeFirst();}
            completedTraces.addLast(trace);
        }
    }

    List<Trace> getCompletedTraces(){
        synchronized(completedTraces){
            return new ArrayList<>(completedTraces);
        }
    }

    SpanMetricsCollector getMetrics(){
        return metrics;
    }
}