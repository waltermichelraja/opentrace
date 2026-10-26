package io.opentrace.core;

import io.opentrace.core.metrics.SpanMetrics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetricsTest{
    private static SpanMetrics of(int n){
        var m=new SpanMetrics();
        for(int i=1;i<=n;i++){m.record(i*1_000_000L,false);}
        return m;
    }

    @Test
    void emptyMetricsAreZero(){
        var m=new SpanMetrics();
        assertEquals(0,m.count());
        assertEquals(0,m.avgMs());
        assertEquals(0,m.percentile(0.95));
    }

    @Test
    void averageIsCorrect(){
        assertEquals(50.5,of(100).avgMs(),0.0001);
    }

    @Test
    void p95AndP99UseNearestRank(){
        var m=of(100);
        assertEquals(95.0,m.percentile(0.95),0.0001);
        assertEquals(99.0,m.percentile(0.99),0.0001);
    }

    @Test
    void p95OfTwentySamplesIsNotTheMax(){
        assertEquals(19.0,of(20).percentile(0.95),0.0001);
    }

    @Test
    void percentileOfSingleSampleIsThatSample(){
        assertEquals(1.0,of(1).percentile(0.99),0.0001);
    }

    @Test
    void countsAndErrorsAreTracked(){
        var m=new SpanMetrics();
        m.record(1,false);m.record(2,true);m.record(3,true);
        assertEquals(3,m.count());
        assertEquals(2,m.errors());
    }
}