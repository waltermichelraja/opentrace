package io.opentrace.core.processor;

import io.opentrace.core.Trace;
import io.opentrace.core.exporter.SpanExporter;

import java.util.List;

public final class SimpleSpanProcessor implements SpanProcessor{
    private final SpanExporter exporter;

    public SimpleSpanProcessor(SpanExporter exporter){
        this.exporter=exporter;
    }

    @Override
    public void onEnd(Trace trace){
        exporter.export(List.of(trace));
    }

    @Override
    public void shutdown(){}
}