package io.opentrace.core;

import io.opentrace.core.metrics.SpanMetrics;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricsConcurrencyTest{
    private static final long MS=1_000_000L;

    @Test
    void concurrentWritersDoNotLoseCountsErrorsOrTotals() throws Exception{
        var m=new SpanMetrics();
        List<Thread> threads=new ArrayList<>();
        for(int t=0;t<8;t++){
            Thread th=new Thread(()->{
                for(int i=1;i<=50_000;i++){m.record(4*MS,i%10==0);}
            });
            threads.add(th);th.start();
        }
        for(Thread th: threads){th.join();}
        assertEquals(400_000L,m.count());
        assertEquals(40_000L,m.errors());
        assertEquals(4.0,m.avgMs(),0.0001);
        assertEquals(4.0,m.percentile(0.99),0.0001);
    }

    @Test
    void samplesFromDifferentThreadsAllContributeToPercentiles() throws Exception{
        var m=new SpanMetrics();
        Thread a=new Thread(()->{for(int i=0;i<100;i++){m.record(10*MS,false);}});
        a.start();a.join();
        Thread b=new Thread(()->{for(int i=0;i<100;i++){m.record(20*MS,false);}});
        b.start();b.join();
        assertEquals(200,m.count());
        assertEquals(10.0,m.percentile(0.25),0.0001);
        assertEquals(20.0,m.percentile(0.75),0.0001);
        assertEquals(15.0,m.avgMs(),0.0001);
    }

    @Test
    void readingWhileWritingIsSafe() throws Exception{
        var m=new SpanMetrics(100);
        var stop=new java.util.concurrent.atomic.AtomicBoolean();
        var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        List<Thread> writers=new ArrayList<>();
        for(int t=0;t<4;t++){
            Thread th=new Thread(()->{while(!stop.get()){m.record(3*MS,false);}});
            writers.add(th);th.start();
        }
        try{
            for(int i=0;i<2000;i++){
                double p=m.percentile(0.95);
                assertTrue(p==0.0||p==3.0,"p95="+p);
                m.avgMs();m.count();m.errors();
            }
        }catch(Throwable t){failure.set(t);}
        stop.set(true);
        for(Thread th: writers){th.join();}
        assertNull(failure.get());
    }
}