package io.opentrace.core.exporter;

import io.opentrace.core.Trace;

import java.util.List;

/** prints each trace as a single line of JSON on stdout. */
public final class ConsoleExporter implements SpanExporter{
    @Override
    public void export(List<Trace> traces){
        for(Trace trace:traces){
            System.out.println(trace.toJson());
        }
    }
}