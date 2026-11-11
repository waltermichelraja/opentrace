package io.opentrace.core.propagation;

import io.opentrace.core.SpanIds;
import io.opentrace.core.TraceId;

public final class Traceparent{
    private static final char[] HEX="0123456789abcdef".toCharArray();
    private static final int LENGTH=55;

    private final TraceId traceId;
    private final long parentSpanId;
    private final int flags;

    private Traceparent(TraceId traceId, long parentSpanId, int flags){
        this.traceId=traceId;
        this.parentSpanId=parentSpanId;
        this.flags=flags;
    }

    public static Traceparent of(TraceId traceId, long parentSpanId, boolean sampled){
        if(traceId==null || !traceId.isValid()){throw new IllegalArgumentException("invalid trace id");}
        if(parentSpanId==0L){throw new IllegalArgumentException("invalid parent span id");}
        return new Traceparent(traceId,parentSpanId,sampled?1:0);
    }

    public static Traceparent tryParse(String header){
        if(header==null || header.length()<LENGTH){return null;}
        int version=twoDigits(header,0);
        if(version<0 || version==0xff){return null;}
        if(header.charAt(2)!='-' || header.charAt(35)!='-' || header.charAt(52)!='-'){return null;}
        if(version==0){
            if(header.length()!=LENGTH){return null;}
        }else if(header.length()>LENGTH && header.charAt(LENGTH)!='-'){
            return null;
        }
        TraceId traceId=TraceId.tryFromHex(header.substring(3,35));
        if(traceId==null){return null;}
        Long parent=SpanIds.tryFromHex(header.substring(36,52));
        if(parent==null){return null;}
        int flags=twoDigits(header,53);
        if(flags<0){return null;}
        return new Traceparent(traceId,parent,flags);
    }

    public TraceId traceId(){
        return traceId;
    }

    public long parentSpanId(){
        return parentSpanId;
    }

    public int flags(){
        return flags;
    }

    public boolean isSampled(){
        return (flags & 1)!=0;
    }

    public String toHeader(){
        return "00-"+traceId.toHex()+"-"+SpanIds.toHex(parentSpanId)+"-"
            +HEX[(flags>>4)&0xF]+HEX[flags&0xF];
    }

    private static int twoDigits(String s,int at){
        int high=digit(s.charAt(at));
        int low=digit(s.charAt(at+1));
        if(high<0 || low<0){return -1;}
        return (high<<4)|low;
    }

    private static int digit(char c){
        if(c>='0' && c<='9'){return c-'0';}
        if(c>='a' && c<='f'){return c-'a'+10;}
        return -1;
    }

    @Override
    public boolean equals(Object o){
        if(this==o){return true;}
        if(!(o instanceof Traceparent)){return false;}
        Traceparent other=(Traceparent)o;
        return traceId.equals(other.traceId) && parentSpanId==other.parentSpanId && flags==other.flags;
    }

    @Override
    public int hashCode(){
        return 31*(31*traceId.hashCode()+Long.hashCode(parentSpanId))+flags;
    }

    @Override
    public String toString(){
        return toHeader();
    }
}