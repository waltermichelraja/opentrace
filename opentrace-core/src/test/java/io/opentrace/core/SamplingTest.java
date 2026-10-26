package io.opentrace.core;

import io.opentrace.core.sampling.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SamplingTest{
    @Test
    void alwaysOffRecordsNothingAndNeverThrows(){
        var tracer=OpenTrace.builder().sampler(new AlwaysOffSampler()).disableExporter().build();
        try(var root=tracer.root("r")){
            try(var s=tracer.span("child")){s.setAttribute("a",1);s.addEvent("e");s.error(new RuntimeException());}
            tracer.trace("t",()->{});
            tracer.putBaggage("k","v");
        }
        assertTrue(tracer.getTraceList().isEmpty());
        assertNull(tracer.currentContext());
    }

    @Test
    void alwaysOnRecordsEveryTrace(){
        var tracer=OpenTrace.builder().sampler(new AlwaysOnSampler()).disableExporter().build();
        for(int i=0;i<10;i++){try(var r=tracer.root("r")){}}
        assertEquals(10,tracer.getTraceList().size());
    }

    @Test
    void probabilitySamplerRoughlyHonoursTheRate(){
        var s=new ProbabilitySampler(0.5);
        int hits=0;
        for(int i=0;i<10_000;i++){if(s.shouldSample()){hits++;}}
        assertTrue(hits>4_500 && hits<5_500,"hits="+hits);
    }

    @Test
    void probabilitySamplerRejectsOutOfRangeRates(){
        assertThrows(IllegalArgumentException.class,()->new ProbabilitySampler(-0.1));
        assertThrows(IllegalArgumentException.class,()->new ProbabilitySampler(1.1));
    }

    @Test
    void zeroAndOneAreExact(){
        var never=new ProbabilitySampler(0.0);
        var always=new ProbabilitySampler(1.0);
        for(int i=0;i<1000;i++){assertFalse(never.shouldSample());assertTrue(always.shouldSample());}
    }
}