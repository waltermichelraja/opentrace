package io.opentrace.core.metrics;

import java.util.Arrays;

public final class SpanMetrics{
    public static final int DEFAULT_WINDOW=1000;

    private final long[] window;
    private int next;
    private int filled;
    private long count;
    private long errors;
    private long totalDuration;

    public SpanMetrics(){
        this(DEFAULT_WINDOW);
    }

    public SpanMetrics(int windowSize){
        if(windowSize<=0){throw new IllegalArgumentException("windowSize must be positive");}
        this.window=new long[windowSize];
    }

    public synchronized void record(long duration,boolean error){
        window[next]=duration;
        next=(next+1)%window.length;
        if(filled<window.length){filled++;}
        count++;
        totalDuration+=duration;
        if(error){errors++;}
    }

    public synchronized long count(){
        return count;
    }

    public synchronized long errors(){
        return errors;
    }

    public synchronized double avgMs(){
        if(count==0){return 0;}
        return (totalDuration/(double)count)/1_000_000.0;
    }

    public synchronized double percentile(double p){
        if(filled==0){return 0;}
        long[] copy=Arrays.copyOf(window,filled);
        Arrays.sort(copy);
        int index=(int)Math.ceil(p*copy.length)-1;
        if(index<0){index=0;}
        if(index>=copy.length){index=copy.length-1;}
        return copy[index]/1_000_000.0;
    }
}