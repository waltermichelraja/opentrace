package io.opentrace.core.exporter;

import io.opentrace.core.Trace;

import java.util.List;

public final class ConsoleExporter implements SpanExporter{
    @Override
    public void export(List<Trace> traces){
        for(Trace trace:traces){
            System.out.println("traceId="+trace.traceId+" spans="+trace.spans.size());
        }
    }
}