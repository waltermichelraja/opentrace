package io.opentrace.core;

import java.util.List;

public final class Trace{
    public final long traceId;
    public final List<Span> spans;
    private final NameRegistry nameRegistry;
    public final String serviceName;
    public final String environment;
    public final String serviceVersion;

    Trace(long traceId,List<Span> spans, NameRegistry nameRegistry, String serviceName, String environment, String serviceVersion){
        this.traceId=traceId;
        this.spans=spans;
        this.nameRegistry=nameRegistry;
        this.serviceName=serviceName;
        this.environment=environment;
        this.serviceVersion=serviceVersion;
    }

    public String resolveName(int nameId){
        return nameRegistry.name(nameId);
    }
}