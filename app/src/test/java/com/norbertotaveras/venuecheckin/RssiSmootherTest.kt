package com.norbertotaveras.venuecheckin

import com.norbertotaveras.venuecheckin.beacon.RssiSmoother
import org.junit.Assert.assertEquals
import org.junit.Test

class RssiSmootherTest {

    @Test
    fun `returns average of RSSI values`() {
        val smoother = RssiSmoother(
            windowSize = 5
        )

        smoother.add(-60)
        smoother.add(-62)
        smoother.add(-65)
        smoother.add(-61)

        val result = smoother.add(-64)

        assertEquals(
            -62,
            result
        )
    }

    @Test
    fun `keeps only values inside the window`() {
        val smoother = RssiSmoother(
            windowSize = 3
        )

        smoother.add(-50)
        smoother.add(-60)
        smoother.add(-70)

        val result = smoother.add(-80)

        assertEquals(
            -70,
            result
        )
    }

    @Test
    fun `clear removes previous values`() {
        val smoother = RssiSmoother()

        smoother.add(-50)
        smoother.add(-60)

        smoother.clear()

        val result = smoother.add(-80)

        assertEquals(
            -80,
            result
        )
    }
}