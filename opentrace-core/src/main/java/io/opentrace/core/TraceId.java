package io.opentrace.core;


public final class TraceId{
    public static final TraceId INVALID=new TraceId(0L,0L);

    private static final char[] HEX="0123456789abcdef".toCharArray();

    private final long high;
    private final long low;

    public TraceId(long high, long low){
        this.high=high;
        this.low=low;
    }

    public static TraceId fromLong(long low){
        return new TraceId(0L,low);
    }

    public static TraceId fromHex(String hex){
        TraceId id=tryFromHex(hex);
        if(id==null){throw new IllegalArgumentException("invalid trace id: "+hex);}
        return id;
    }

    public static TraceId tryFromHex(String hex){
        if(hex==null || hex.length()!=32){return null;}
        long high=0L,low=0L;
        for(int i=0;i<32;i++){
            int digit=digit(hex.charAt(i));
            if(digit<0){return null;}
            if(i<16){high=(high<<4)|digit;}
            else{low=(low<<4)|digit;}
        }
        if(high==0L && low==0L){return null;}
        return new TraceId(high,low);
    }

    public long high(){
        return high;
    }

    public long low(){
        return low;
    }

    public boolean isValid(){
        return high!=0L || low!=0L;
    }

    public String toHex(){
        char[] out=new char[32];
        writeHex(high,out,0);
        writeHex(low,out,16);
        return new String(out);
    }

    private static void writeHex(long value,char[] out,int offset){
        for(int i=15;i>=0;i--){
            out[offset+i]=HEX[(int)(value & 0xF)];
            value>>>=4;
        }
    }

    private static int digit(char c){
        if(c>='0' && c<='9'){return c-'0';}
        if(c>='a' && c<='f'){return c-'a'+10;}
        return -1;
    }

    @Override
    public boolean equals(Object o){
        if(this==o){return true;}
        if(!(o instanceof TraceId)){return false;}
        TraceId other=(TraceId)o;
        return high==other.high && low==other.low;
    }

    @Override
    public int hashCode(){
        return 31*Long.hashCode(high)+Long.hashCode(low);
    }

    @Override
    public String toString(){
        return toHex();
    }
}