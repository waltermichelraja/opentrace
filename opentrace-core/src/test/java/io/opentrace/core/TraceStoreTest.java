package io.opentrace.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class TraceStoreTest{
    private static OpenTrace tracerWithCap(int cap){
        return OpenTrace.builder().serviceName("test").disableExporter().maxStoredTraces(cap).build();
    }

    private static List<String> rootNames(OpenTrace tracer){
        List<String> names=new ArrayList<>();
        for(Trace t: tracer.getTraceList()){
            names.add(t.resolveName(t.getRoot().getNameId()));
        }
        return names;
    }

    @Test
    void keepsOnlyTheMostRecentTracesInOrder(){
        var tracer=tracerWithCap(5);
        for(int i=0;i<12;i++){try(var r=tracer.root("r"+i)){}}
        assertEquals(List.of("r7","r8","r9","r10","r11"),rootNames(tracer));
    }

    @Test
    void defaultCapIsOneThousand(){
        var tracer=tracer();
        for(int i=0;i<1500;i++){try(var r=tracer.root("r")){}}
        assertEquals(1000,tracer.getTraceList().size());
    }

    @Test
    void zeroKeepsNoTracesButMetricsAreStillRecorded(){
        var tracer=tracerWithCap(0);
        for(int i=0;i<3;i++){try(var r=tracer.root("r")){}}
        assertTrue(tracer.getTraceList().isEmpty());
        assertEquals(3,tracer.getMetrics().getMetrics().get("r").count());
    }

    @Test
    void negativeCapIsRejected(){
        assertThrows(IllegalArgumentException.class,()->OpenTrace.builder().maxStoredTraces(-1));
    }

    @Test
    void returnedListIsASnapshot(){
        var tracer=tracerWithCap(10);
        try(var r=tracer.root("a")){}
        List<Trace> snapshot=tracer.getTraceList();
        try(var r=tracer.root("b")){}
        assertEquals(1,snapshot.size());
        assertEquals(2,tracer.getTraceList().size());
    }

    @Test
    void concurrentWritersNeverExceedTheCap() throws Exception{
        var tracer=tracerWithCap(50);
        List<Thread> threads=new ArrayList<>();
        for(int t=0;t<8;t++){
            Thread th=new Thread(()->{for(int i=0;i<500;i++){try(var r=tracer.root("r")){}}});
            threads.add(th);th.start();
        }
        for(Thread th: threads){th.join();}
        assertEquals(50,tracer.getTraceList().size());
    }
}