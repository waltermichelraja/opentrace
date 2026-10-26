package io.opentrace.core;

public final class Span{
    long spanId;
    long parentSpanId;
    long startTime;
    long duration;
    int nameId;
    SpanStatus status=SpanStatus.UNSET;
    String errorType;
    String errorMessage;
    private java.util.Map<String,Object> attributes;
    private final java.util.List<SpanEvent> events=java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    private final java.util.List<SpanLink> links=java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    public long getSpanId(){
        return spanId;
    }
    public long getParentSpanId(){
        return parentSpanId;
    }
    public long getStartTime(){
        return startTime;
    }
    public long getDuration(){
        return duration;
    }
    public int getNameId(){
        return nameId;
    }
    public SpanStatus getStatus(){
        return status;
    }
    public String getErrorType(){
        return errorType;
    }
    public String getErrorMessage(){
        return errorMessage;
    }
    synchronized void putAttribute(String key, Object value){
        if(attributes==null){attributes=new java.util.LinkedHashMap<>();}
        attributes.put(key, value);
    }
    public synchronized java.util.Map<String,Object> getAttributes(){
        if(attributes==null){return java.util.Collections.emptyMap();}
        return java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(attributes));
    }
    public void addEvent(String name){
        events.add(new SpanEvent(name, System.nanoTime()));
    }
    public java.util.List<SpanEvent> getEvents(){
        return events;
    }
    public void addLink(long traceId, long spanId){
        links.add(new SpanLink(traceId, spanId));
    }

    public java.util.List<SpanLink> getLinks(){
        return links;
    }
}