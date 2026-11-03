package io.opentrace.core.metrics;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceArray;

public final class SpanMetrics{
    public static final int DEFAULT_WINDOW=1000;
    private static final int STRIPES=8;

    private final int windowSize;
    private final AtomicReferenceArray<Stripe> stripes=new AtomicReferenceArray<>(STRIPES);

    public SpanMetrics(){
        this(DEFAULT_WINDOW);
    }

    public SpanMetrics(int windowSize){
        if(windowSize<=0){throw new IllegalArgumentException("windowSize must be positive");}
        this.windowSize=windowSize;
    }

    public void record(long duration,boolean error){
        stripe().record(duration,error);
    }

    public long count(){
        long n=0;
        for(int i=0;i<STRIPES;i++){
            Stripe s=stripes.get(i);
            if(s!=null){n+=s.totals()[0];}
        }
        return n;
    }

    public long errors(){
        long n=0;
        for(int i=0;i<STRIPES;i++){
            Stripe s=stripes.get(i);
            if(s!=null){n+=s.totals()[1];}
        }
        return n;
    }

    public double avgMs(){
        long count=0,total=0;
        for(int i=0;i<STRIPES;i++){
            Stripe s=stripes.get(i);
            if(s!=null){
                long[] t=s.totals();
                count+=t[0];
                total+=t[2];
            }
        }
        if(count==0){return 0;}
        return (total/(double)count)/1_000_000.0;
    }

    public double percentile(double p){
        long[][] parts=new long[STRIPES][];
        int size=0;
        for(int i=0;i<STRIPES;i++){
            Stripe s=stripes.get(i);
            if(s!=null){
                parts[i]=s.samples();
                size+=parts[i].length;
            }
        }
        if(size==0){return 0;}
        long[] all=new long[size];
        int at=0;
        for(long[] part: parts){
            if(part!=null){
                System.arraycopy(part,0,all,at,part.length);
                at+=part.length;
            }
        }
        Arrays.sort(all);
        int index=(int)Math.ceil(p*all.length)-1;
        if(index<0){index=0;}
        if(index>=all.length){index=all.length-1;}
        return all[index]/1_000_000.0;
    }

    private Stripe stripe(){
        int i=(int)(Thread.currentThread().getId() & (STRIPES-1));
        Stripe s=stripes.get(i);
        if(s==null){
            s=new Stripe(windowSize);
            if(!stripes.compareAndSet(i,null,s)){s=stripes.get(i);}
        }
        return s;
    }

    private static final class Stripe{
        private final long[] window;
        private int next;
        private int filled;
        private long count;
        private long errors;
        private long totalDuration;

        Stripe(int windowSize){
            this.window=new long[windowSize];
        }

        synchronized void record(long duration,boolean error){
            window[next]=duration;
            next=(next+1)%window.length;
            if(filled<window.length){filled++;}
            count++;
            totalDuration+=duration;
            if(error){errors++;}
        }

        synchronized long[] totals(){
            return new long[]{count,errors,totalDuration};
        }

        synchronized long[] samples(){
            return Arrays.copyOf(window,filled);
        }
    }
}