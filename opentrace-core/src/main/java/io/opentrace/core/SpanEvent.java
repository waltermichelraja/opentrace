package io.opentrace.core;

public final class SpanEvent{
    private final String name;
    private final long timestamp;

    public SpanEvent(String name, long timestamp){
        this.name=name;
        this.timestamp=timestamp;
    }

    public String getName(){
        return name;
    }

    public long getTimestamp(){
        return timestamp;
    }
}