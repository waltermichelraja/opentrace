package io.opentrace.core.processor;

import io.opentrace.core.Span;
import io.opentrace.core.Trace;

public interface SpanProcessor{
    default void onStart(Span span){}

    void onEnd(Trace trace);

    default void shutdown(){}
}