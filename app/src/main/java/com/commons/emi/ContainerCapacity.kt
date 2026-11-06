package com.commons.emi

// Container_Models has no is_finite field: (-1, -1) means unlimited places.
// Zero means invalid dimensions, not an empty container.
internal fun capacityFromDimensions(columns: Int, rows: Int): Int {
    if (columns == -1 && rows == -1) return -1
    val places = columns.toLong() * rows
    return if (columns > 0 && rows > 0 && places <= Int.MAX_VALUE) places.toInt() else 0
}
