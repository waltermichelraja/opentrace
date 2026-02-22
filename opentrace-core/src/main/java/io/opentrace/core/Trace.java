package io.opentrace.core;

import java.util.List;

public final class Trace{
    public final long traceId;
    public final List<Span> spans;

    public Trace(long traceId,List<Span> spans){
        this.traceId=traceId;
        this.spans=spans;
    }
}