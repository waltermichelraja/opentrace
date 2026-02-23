package io.opentrace.core;

import java.util.ArrayDeque;
import java.util.ArrayList;

final class TraceState{
    public long traceId;

    final ArrayDeque<Span> stack=new ArrayDeque<>();
    final ArrayList<Span> spans=new ArrayList<>(16);
}