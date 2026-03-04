package io.opentrace.core.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpanMetrics{
    private final List<Long> durations=new ArrayList<>();
    private long errors;

    public synchronized void record(long duration,boolean error){
        durations.add(duration);
        if(error){errors++;}
    }

    public synchronized int count(){
        return durations.size();
    }

    public synchronized long errors(){
        return errors;
    }

    public synchronized double avgMs(){
        if(durations.isEmpty()){return 0;}
        long sum=0;
        for(long d:durations){sum+=d;}
        return (sum/durations.size())/1_000_000.0;
    }

    public synchronized double percentile(double p){
        if(durations.isEmpty()){return 0;}
        List<Long> copy=new ArrayList<>(durations);
        Collections.sort(copy);
        int index=(int)(p*copy.size());
        if(index>=copy.size()){index=copy.size()-1;}
        return copy.get(index)/1_000_000.0;
    }
}