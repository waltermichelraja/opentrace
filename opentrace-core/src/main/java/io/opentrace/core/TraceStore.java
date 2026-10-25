package io.opentrace.core;

import io.opentrace.core.metrics.SpanMetricsCollector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class TraceStore{
    private final List<Trace> completedTraces=new CopyOnWriteArrayList<>();
    private final SpanMetricsCollector metrics=new SpanMetricsCollector();

    void record(Trace trace){
        metrics.record(trace);
        completedTraces.add(trace);
    }

    List<Trace> getCompletedTraces(){
        return completedTraces;
    }

    SpanMetricsCollector getMetrics(){
        return metrics;
    }
}