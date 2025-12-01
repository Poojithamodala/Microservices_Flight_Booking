package com.flightapp.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FLIGHTTYPETest {

    @Test
    void testEnumValues() {
        // Check that ONE_WAY exists
        assertEquals("ONE_WAY", FLIGHTTYPE.ONE_WAY.name());
        // Check that ROUND_TRIP exists
        assertEquals("ROUND_TRIP", FLIGHTTYPE.ROUND_TRIP.name());
    }

    
   
}
