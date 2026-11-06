package com.commons.emi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TubeLabelOrientationTest {
    @Test fun turnsLabelsOnlyWhenQrAndTextFitPrintableTapeWidth() {
        // The M511 reports 1.44 in printable width on nominal 1.5 in tape.
        assertTrue(rotateTubeLabelOnTape(TubeLabelSize.MEDIUM, 1.44))
        assertTrue(rotateTubeLabelOnTape(TubeLabelSize.SMALL, 1.44))
        assertFalse(rotateTubeLabelOnTape(TubeLabelSize.MEDIUM, 19.05 / 25.4))
        assertFalse(rotateTubeLabelOnTape(TubeLabelSize.MEDIUM, 35.5 / 25.4))
        assertTrue(rotateTubeLabelOnTape(TubeLabelSize.BIG, 1.44))
        assertFalse(rotateTubeLabelOnTape(TubeLabelSize.BIG, 35.5 / 25.4))
        assertFalse(rotateTubeLabelOnTape(TubeLabelSize.BIG, 0.75))
        assertFalse(rotateTubeLabelOnTape(TubeLabelSize.MEDIUM, Double.NaN))
    }
}
