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
    java.util.Map<String,Object> attributes;

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
    public java.util.Map<String,Object> getAttributes(){
        return attributes;
    }
}