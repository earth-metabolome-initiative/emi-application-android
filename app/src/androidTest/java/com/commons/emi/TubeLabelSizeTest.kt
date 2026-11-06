package com.commons.emi

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class TubeLabelSizeTest {
    @Test fun renderedStickersKeepExactQrPayloadAtPrinterResolution() {
        val scanner = BarcodeScanning.getClient()
        try {
            for (size in TubeLabelSize.entries) {
                val dpi = if (size == TubeLabelSize.BIG) 300 else 203
                for (code in listOf("mnsl_000068", "container_1x1_000068")) for (rotated in listOf(false, true)) {
                    val bitmap = renderTubeLabel(code, size, dpi, rotated)
                    // Brady maps image width to feed length and image height to
                    // tape width, regardless of the sticker's orientation.
                    val lengthDots = (size.lengthInches * dpi).roundToInt()
                    val widthDots = (size.widthInches * dpi).roundToInt()
                    assertEquals(if (rotated) widthDots else lengthDots, bitmap.width)
                    assertEquals(if (rotated) lengthDots else widthDots, bitmap.height)
                    var left = bitmap.width
                    var right = -1
                    var top = bitmap.height
                    var bottom = -1
                    for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                        if (bitmap.getPixel(x, y) != android.graphics.Color.WHITE) {
                            left = minOf(left, x)
                            right = maxOf(right, x)
                            top = minOf(top, y)
                            bottom = maxOf(bottom, y)
                        }
                    }
                    val longAxisInk = if (rotated) bottom - top else right - left
                    val shortAxisInk = if (rotated) right - left else bottom - top
                    assertTrue("Ink covers too little label length for $code at ${size.displayName} (rotated=$rotated)",
                        longAxisInk > lengthDots * 0.7)
                    assertTrue("Ink covers too little label width for $code at ${size.displayName} (rotated=$rotated)",
                        shortAxisInk > widthDots * 0.45)
                    val barcodes = Tasks.await(scanner.process(InputImage.fromBitmap(bitmap, 0)), 20, TimeUnit.SECONDS)
                    assertTrue("QR failed to decode $code at ${size.displayName}",
                        barcodes.any { it.format == Barcode.FORMAT_QR_CODE && it.rawValue == code })
                }
            }
        } finally {
            scanner.close()
        }
    }

    @Test fun compactBigOnM511StockKeepsQrAndWrappedTextAcrossTheTape() {
        val size = TubeLabelSize.BIG
        val dpi = 300
        val scanner = BarcodeScanning.getClient()
        try {
            for (printableWidth in listOf(1.44, 35.6 / 25.4)) {
                assertTrue(rotateTubeLabelOnTape(size, printableWidth))
                for (code in listOf("mnsl_000036", "container_1x1_000068")) {
                    val bitmap = renderTubeLabel(code, size, dpi, true, printableWidth)
                    assertEquals((size.widthInches * dpi).roundToInt(), bitmap.width)
                    assertEquals((printableWidth * dpi).roundToInt(), bitmap.height)
                    val qrStart = bitmap.height - bitmap.width +
                        (0.6 / 25.4 * dpi).roundToInt()
                    var firstLineInk = false
                    var secondLineInk = false
                    for (y in 0 until qrStart) for (x in 0 until bitmap.width) {
                        if (bitmap.getPixel(x, y) != android.graphics.Color.WHITE) {
                            if (x < bitmap.width / 2) firstLineInk = true else secondLineInk = true
                        }
                    }
                    assertTrue("Text missing on one side for $code", firstLineInk && secondLineInk)
                    val barcodes = Tasks.await(scanner.process(InputImage.fromBitmap(bitmap, 0)), 20, TimeUnit.SECONDS)
                    assertTrue("Compact Big QR failed to decode $code",
                        barcodes.any { it.format == Barcode.FORMAT_QR_CODE && it.rawValue == code })
                }
            }
        } finally {
            scanner.close()
        }
    }

    @Test fun rejectsTapesThatCannotProduceTheSelectedSize() {
        assertNotNull(tubeLabelSupplyError(TubeLabelSize.BIG, 0.75, false))
        assertNull(tubeLabelSupplyError(TubeLabelSize.MEDIUM, 0.75, false))
        assertNull(tubeLabelSupplyError(TubeLabelSize.SMALL, 0.5, false))
        assertNotNull(tubeLabelSupplyError(TubeLabelSize.BIG, 1.5, true))
        assertNull(tubeLabelSupplyError(TubeLabelSize.BIG, 1.5, false))
        assertNotNull(tubeLabelSupplyError(TubeLabelSize.MEDIUM, Double.NaN, false))
    }
}
