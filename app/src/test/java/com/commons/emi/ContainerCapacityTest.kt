package com.commons.emi

import org.junit.Assert.assertEquals
import org.junit.Test

class ContainerCapacityTest {
    @Test
    fun finiteModelDimensionsDeterminePlaces() {
        assertEquals(81, capacityFromDimensions(9, 9))
        assertEquals(1, capacityFromDimensions(1, 1))
    }

    @Test
    fun unlimitedModelUsesPairedSentinels() {
        assertEquals(-1, capacityFromDimensions(-1, -1))
        assertEquals(0, capacityFromDimensions(-1, 9))
    }

    @Test
    fun invalidOrOverflowingDimensionsCannotBecomeCapacity() {
        assertEquals(0, capacityFromDimensions(0, 9))
        assertEquals(0, capacityFromDimensions(Int.MAX_VALUE, 2))
    }
}
