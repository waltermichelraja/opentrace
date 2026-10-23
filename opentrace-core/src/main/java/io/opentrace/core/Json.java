package io.opentrace.core;

public final class Json{
    private Json(){}

    /** returns {@code s} as a quoted, escaped JSON string literal ({@code null} becomes {@code null}). */
    public static String quote(String s){
        if(s==null){return "null";}
        StringBuilder sb=new StringBuilder(s.length()+2);
        sb.append('"');
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            switch(c){
                case '"'->sb.append("\\\"");
                case '\\'->sb.append("\\\\");
                case '\n'->sb.append("\\n");
                case '\r'->sb.append("\\r");
                case '\t'->sb.append("\\t");
                case '\b'->sb.append("\\b");
                case '\f'->sb.append("\\f");
                default->{
                    if(c<0x20){sb.append(String.format("\\u%04x",(int)c));}
                    else{sb.append(c);}
                }
            }
        }
        return sb.append('"').toString();
    }

    public static String value(Object v){
        if(v==null){return "null";}
        if(v instanceof Boolean){return v.toString();}
        if(v instanceof Number){
            double d=((Number)v).doubleValue();
            if(Double.isNaN(d)||Double.isInfinite(d)){return quote(v.toString());}
            return v.toString();
        }
        return quote(v.toString());
    }
}