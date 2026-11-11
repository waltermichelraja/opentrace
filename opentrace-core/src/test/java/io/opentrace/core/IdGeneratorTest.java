package io.opentrace.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class IdGeneratorTest{
    @Test
    void traceIdsAreValidAndDistinct(){
        var g=new IdGenerator();
        Set<TraceId> seen=new HashSet<>();
        for(int i=0;i<10_000;i++){
            TraceId id=g.nextTraceId();
            assertTrue(id.isValid());
            assertTrue(seen.add(id),"duplicate trace id");
        }
    }

    @Test
    void traceIdsUseAll128Bits(){
        var g=new IdGenerator();
        for(int i=0;i<1000;i++){
            assertNotEquals(0L,g.nextTraceId().high());
        }
    }

    @Test
    void spanIdsAreNonZeroAndDistinct(){
        var g=new IdGenerator();
        Set<Long> seen=new HashSet<>();
        for(int i=0;i<10_000;i++){
            long id=g.nextSpanId();
            assertNotEquals(0L,id);
            assertTrue(seen.add(id),"duplicate span id");
        }
    }

    @Test
    void concurrentGenerationProducesNoDuplicates() throws Exception{
        var g=new IdGenerator();
        Set<Long> ids=ConcurrentHashMap.newKeySet();
        List<Thread> threads=new ArrayList<>();
        for(int t=0;t<8;t++){
            Thread th=new Thread(()->{for(int i=0;i<20_000;i++){ids.add(g.nextSpanId());}});
            threads.add(th);th.start();
        }
        for(Thread th: threads){th.join();}
        assertEquals(160_000,ids.size());
    }

    @Test
    void tracerAssignsDistinctValidIdsToEveryTrace(){
        var tracer=tracer();
        for(int i=0;i<200;i++){try(var r=tracer.root("r")){}}
        Set<TraceId> ids=new HashSet<>();
        for(Trace t: tracer.getTraceList()){
            assertTrue(t.traceId.isValid());
            ids.add(t.traceId);
        }
        assertEquals(200,ids.size());
    }
}