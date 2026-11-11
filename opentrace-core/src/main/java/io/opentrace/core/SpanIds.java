package io.opentrace.core;

public final class SpanIds{
    private static final char[] HEX="0123456789abcdef".toCharArray();

    private SpanIds(){}

    public static String toHex(long id){
        char[] out=new char[16];
        for(int i=15;i>=0;i--){
            out[i]=HEX[(int)(id & 0xF)];
            id>>>=4;
        }
        return new String(out);
    }

    public static Long tryFromHex(String hex){
        if(hex==null || hex.length()!=16){return null;}
        long id=0L;
        for(int i=0;i<16;i++){
            char c=hex.charAt(i);
            int digit;
            if(c>='0' && c<='9'){digit=c-'0';}
            else if(c>='a' && c<='f'){digit=c-'a'+10;}
            else{return null;}
            id=(id<<4)|digit;
        }
        if(id==0L){return null;}
        return id;
    }
}