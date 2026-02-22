package io.opentrace.core;

import java.util.List;

public final class Trace{
    public final long traceId;
    public final List<Span> spans;
    private final NameRegistry nameRegistry;

    Trace(long traceId,List<Span> spans,NameRegistry nameRegistry){
        this.traceId=traceId;
        this.spans=spans;
        this.nameRegistry=nameRegistry;
    }

    public String resolveName(int nameId){
        return nameRegistry.name(nameId);
    }
}