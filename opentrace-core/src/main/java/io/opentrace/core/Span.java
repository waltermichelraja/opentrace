package io.opentrace.core;

public final class Span{
    public long spanId;
    public long parentSpanId;
    public long startTime;
    public long duration;
    public int nameId;
    public boolean error;
    public String errorType;
    public String errorMessage;
}