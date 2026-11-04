package io.opentrace.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class TraceIdTest{
    @Test
    void toHexIsThirtyTwoLowercaseDigitsZeroPadded(){
        assertEquals("00000000000000010000000000000002",new TraceId(1L,2L).toHex());
        assertEquals("abcdef0123456789fedcba9876543210",
            new TraceId(0xABCDEF0123456789L,0xFEDCBA9876543210L).toHex());
    }

    @Test
    void parsesTheW3CExample(){
        TraceId id=TraceId.fromHex("4bf92f3577b34da6a3ce929d0e0e4736");
        assertEquals(0x4bf92f3577b34da6L,id.high());
        assertEquals(0xa3ce929d0e0e4736L,id.low());
        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736",id.toHex());
    }

    @Test
    void invalidTextIsRejected(){
        String[] bad={
            null,
            "",
            "4bf92f3577b34da6a3ce929d0e0e473",        // 31 chars
            "4bf92f3577b34da6a3ce929d0e0e47366",      // 33 chars
            "4BF92F3577B34DA6A3CE929D0E0E4736",       // uppercase is not allowed by W3C
            "4bf92f3577b34da6a3ce929d0e0e47zz",       // not hex
            " 4bf92f3577b34da6a3ce929d0e0e4736",      // whitespace
            "00000000000000000000000000000000"        // all zeros is invalid
        };
        for(String s: bad){
            assertNull(TraceId.tryFromHex(s),"should be rejected: "+s);
            assertThrows(IllegalArgumentException.class,()->TraceId.fromHex(s),"should throw: "+s);
        }
    }

    @Test
    void equalsAndHashCodeUseBothHalves(){
        assertEquals(new TraceId(1,2),new TraceId(1,2));
        assertEquals(new TraceId(1,2).hashCode(),new TraceId(1,2).hashCode());
        assertNotEquals(new TraceId(1,2),new TraceId(1,3));
        assertNotEquals(new TraceId(1,2),new TraceId(9,2));
        assertNotEquals(new TraceId(1,2),null);
        Set<TraceId> set=new HashSet<>();
        set.add(new TraceId(5,6));
        set.add(new TraceId(5,6));
        assertEquals(1,set.size());
    }

    @Test
    void allZeroIsNotValid(){
        assertFalse(new TraceId(0,0).isValid());
        assertFalse(TraceId.INVALID.isValid());
        assertTrue(new TraceId(0,1).isValid());
        assertTrue(new TraceId(1,0).isValid());
    }

    @Test
    void fromLongPadsTheHighHalfWithZeros(){
        TraceId id=TraceId.fromLong(255);
        assertEquals(0L,id.high());
        assertEquals(255L,id.low());
        assertEquals("000000000000000000000000000000ff",id.toHex());
        assertTrue(id.isValid());
    }

    @Test
    void toStringIsTheHexForm(){
        assertEquals("00000000000000010000000000000002",new TraceId(1,2).toString());
    }

    @Test
    void randomValuesRoundTrip(){
        var rnd=ThreadLocalRandom.current();
        for(int i=0;i<1000;i++){
            TraceId id=new TraceId(rnd.nextLong(),rnd.nextLong());
            if(!id.isValid()){continue;}
            assertEquals(id,TraceId.fromHex(id.toHex()));
        }
    }
}