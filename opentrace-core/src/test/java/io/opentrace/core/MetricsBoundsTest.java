package io.opentrace.core;

import io.opentrace.core.metrics.SpanMetrics;
import io.opentrace.core.metrics.SpanMetricsCollector;
import org.junit.jupiter.api.Test;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class MetricsBoundsTest{
    private static final long MS=1_000_000L;

    @Test
    void percentilesCoverOnlyTheMostRecentWindow(){
        var m=new SpanMetrics(100);
        for(int i=1;i<=1000;i++){m.record(i*MS,false);}
        // the window holds 901..1000 ms, so p95 is the 95th of those
        assertEquals(995.0,m.percentile(0.95),0.0001);
    }

    @Test
    void countErrorsAndAverageCoverEverySample(){
        var m=new SpanMetrics(10);
        for(int i=1;i<=100;i++){m.record(i*MS,i%10==0);}
        assertEquals(100,m.count());
        assertEquals(10,m.errors());
        assertEquals(50.5,m.avgMs(),0.0001);
        assertEquals(100.0,m.percentile(0.99),0.0001);   // from the last 10 samples only
    }

    @Test
    void defaultWindowIsOneThousandSamples(){
        var m=new SpanMetrics();
        for(int i=1;i<=2000;i++){m.record(i*MS,false);}
        assertEquals(1001.0,m.percentile(0.001),0.0001); // oldest sample still in the window
    }

    @Test
    void aMillionSamplesDoNotBreakAnything(){
        var m=new SpanMetrics(100);
        for(int i=0;i<1_000_000;i++){m.record(5*MS,false);}
        assertEquals(1_000_000L,m.count());
        assertEquals(5.0,m.percentile(0.99),0.0001);
    }

    @Test
    void invalidSizesAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new SpanMetrics(0));
        assertThrows(IllegalArgumentException.class,()->new SpanMetricsCollector(0,10));
        assertThrows(IllegalArgumentException.class,()->new SpanMetricsCollector(10,0));
    }

    private static SpanMetricsCollector collect(int maxNames,String... rootNames){
        var tracer=tracer();
        for(String n: rootNames){try(var r=tracer.root(n)){}}
        var collector=new SpanMetricsCollector(maxNames,10);
        for(Trace t: tracer.getTraceList()){collector.record(t);}
        return collector;
    }

    @Test
    void distinctNamesAreCappedAndOverflowIsFoldedIntoOneBucket(){
        String[] names=new String[20];
        for(int i=0;i<20;i++){names[i]="n"+i;}
        var c=collect(5,names);
        assertEquals(6,c.getMetrics().size());           // 5 real names + "_other"
        assertEquals(15,c.getMetrics().get("_other").count());
    }

    @Test
    void namesAlreadyTrackedKeepRecordingAfterTheCapIsReached(){
        var c=collect(2,"a","b","c","a","a");
        assertEquals(3,c.getMetrics().get("a").count());
        assertEquals(1,c.getMetrics().get("b").count());
        assertEquals(1,c.getMetrics().get("_other").count());
        assertNull(c.getMetrics().get("c"));
    }
}