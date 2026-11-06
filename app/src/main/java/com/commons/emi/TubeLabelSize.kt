package com.commons.emi

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class TubeLabelSize(val displayName: String, val lengthMm: Double, val widthMm: Double) {
    SMALL("Small — L4731 (25.4 × 10 mm)", 25.4, 10.0),
    MEDIUM("Medium — L4732 (35.6 × 16.9 mm)", 35.6, 16.9),
    BIG("Big — L4736 (45.7 × 21.2 mm; compact on wide tape)", 45.7, 21.2);

    val lengthInches: Double get() = lengthMm / 25.4
    val widthInches: Double get() = widthMm / 25.4
}

// Brady reports the printable width (smaller than the nominal tape width).
// The physical sticker cannot be narrower than the installed tape.
internal fun tubeLabelSupplyError(size: TubeLabelSize, supplyWidthInches: Double, preSized: Boolean): String? = when {
    preSized -> "Load continuous Brady tape to select a custom label length."
    !supplyWidthInches.isFinite() || supplyWidthInches <= 0.0 ->
        "Cannot read the installed tape width. Reconnect the printer."
    supplyWidthInches + 0.001 < size.widthInches ->
        "${size.displayName} needs at least ${size.widthMm} mm tape; installed tape is ${"%.1f".format(supplyWidthInches * 25.4)} mm wide."
    else -> null
}

// The SDK's supplyWidth already excludes the non-printable tape edges; the
// bitmap leaves another 0.6 mm inside its own edges. On 38.1 mm stock the M511
// reports 1.44 in (36.6 mm) printable width. A Big label can be reflowed into
// 35.6 mm or more: its 21.2 mm QR still leaves room for wrapped text.
internal fun rotateTubeLabelOnTape(size: TubeLabelSize, supplyWidthInches: Double): Boolean =
    supplyWidthInches.isFinite() && supplyWidthInches >=
        if (size == TubeLabelSize.BIG) 35.6 / 25.4 else size.lengthInches

private val qrHints = mapOf(
    EncodeHintType.CHARACTER_SET to "UTF-8",
    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
)

internal fun renderTubeLabel(
    code: String, size: TubeLabelSize, dpi: Int, rotated: Boolean = false,
    layoutLengthInches: Double = size.lengthInches
): Bitmap {
    require(code.isNotBlank()) { "Scan a sample QR code first." }
    require(dpi > 0) { "Printer resolution is unavailable." }

    require(layoutLengthInches.isFinite() && layoutLengthInches >= size.widthInches) {
        "The selected tape is too narrow for this label."
    }
    val width = (size.widthInches * dpi).roundToInt()
    val length = (layoutLengthInches * dpi).roundToInt()
    val padding = (0.6 / 25.4 * dpi).roundToInt().coerceAtLeast(2)
    val qrSide = width - padding * 2
    require(qrSide >= 29) { "Tape is too narrow to print a QR code." }

    val textStart = qrSide + padding * 2
    val textLength = length - textStart - padding
    require(textLength > 0) { "The selected tape leaves no room for the identifier." }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = min(width * 0.30f, textLength * 0.22f)
    }
    val measured = paint.measureText(code)
    if (measured > textLength) paint.textSize *= textLength / measured
    var firstLine = code
    var secondLine: String? = null
    if (paint.textSize < dpi * 1.7 / 25.4 ||
        (rotated && size == TubeLabelSize.BIG && layoutLengthInches < size.lengthInches)) {
        val middle = code.length / 2
        var splitAt = middle
        var distance = Int.MAX_VALUE
        for (i in 1 until code.length) {
            if (code[i - 1] == '_' && abs(i - middle) < distance) {
                splitAt = i
                distance = abs(i - middle)
            }
        }
        firstLine = code.substring(0, splitAt)
        secondLine = code.substring(splitAt)
        paint.textSize = min(width * 0.30f, textLength * 0.22f)
        val longest = max(paint.measureText(firstLine), paint.measureText(secondLine))
        if (longest > textLength) paint.textSize *= textLength / longest
    }
    require(paint.textSize >= dpi * 1.7 / 25.4) {
        "QR code text is too long for the selected tape. Use wider tape."
    }
    val matrix = QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, qrSide, qrSide, qrHints)
    require(matrix.width == qrSide && matrix.height == qrSide) {
        "QR code is too dense for ${size.displayName}. Choose a larger label."
    }
    // Brady fits bitmap WIDTH along the feed and bitmap HEIGHT across the tape.
    // When the long label side fits across the tape, swap those axes to save feed.
    val bitmap = Bitmap.createBitmap(if (rotated) width else length,
        if (rotated) length else width, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)
    val pixels = IntArray(qrSide * qrSide) { index ->
        if (matrix[index % qrSide, index / qrSide]) Color.BLACK else Color.WHITE
    }
    bitmap.setPixels(pixels, 0, qrSide,
        if (rotated) padding else length - padding - qrSide,
        if (rotated) length - padding - qrSide else padding, qrSide, qrSide)
    if (rotated) {
        canvas.save()
        canvas.translate(width.toFloat(), 0f)
        canvas.rotate(90f)
    }
    val centerX = padding + textLength / 2f
    val baseline = width / 2f - (paint.ascent() + paint.descent()) / 2f
    val lineOffset = if (secondLine == null) 0f else paint.textSize * 0.6f
    canvas.drawText(firstLine, centerX - paint.measureText(firstLine) / 2f, baseline - lineOffset, paint)
    if (secondLine != null) {
        canvas.drawText(secondLine, centerX - paint.measureText(secondLine) / 2f, baseline + lineOffset, paint)
    }
    if (rotated) canvas.restore()
    return bitmap
}
