package io.opentrace.core.exporter;

import io.opentrace.core.Trace;

import java.util.List;

public interface SpanExporter{
    public void export(List<Trace> traces);
}