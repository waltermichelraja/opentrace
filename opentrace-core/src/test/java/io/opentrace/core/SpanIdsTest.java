package io.opentrace.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class SpanIdsTest{
    @Test
    void toHexIsSixteenLowercaseDigitsZeroPadded(){
        assertEquals("0000000000000001",SpanIds.toHex(1L));
        assertEquals("abcdef0123456789",SpanIds.toHex(0xABCDEF0123456789L));
        assertEquals("ffffffffffffffff",SpanIds.toHex(-1L));
        assertEquals("8000000000000000",SpanIds.toHex(Long.MIN_VALUE));
    }

    @Test
    void parsesTheW3CExample(){
        assertEquals(0x00f067aa0ba902b7L,SpanIds.tryFromHex("00f067aa0ba902b7"));
    }

    @Test
    void idsWithTheTopBitSetComeBackNegative(){
        assertEquals(-1L,SpanIds.tryFromHex("ffffffffffffffff"));
        assertEquals(Long.MIN_VALUE,SpanIds.tryFromHex("8000000000000000"));
    }

    @Test
    void invalidTextIsRejected(){
        String[] bad={
            null,
            "",
            "00f067aa0ba902b",          // 15 chars
            "00f067aa0ba902b77",        // 17 chars
            "00F067AA0BA902B7",         // uppercase is not allowed by W3C
            "00f067aa0ba902zz",         // not hex
            " 00f067aa0ba902b",         // whitespace
            "0000000000000000"          // all zeros is invalid
        };
        for(String s: bad){
            assertNull(SpanIds.tryFromHex(s),"should be rejected: "+s);
        }
    }

    @Test
    void randomValuesRoundTrip(){
        var rnd=ThreadLocalRandom.current();
        for(int i=0;i<1000;i++){
            long id=rnd.nextLong();
            if(id==0L){continue;}
            assertEquals(id,SpanIds.tryFromHex(SpanIds.toHex(id)));
        }
    }
}