package com.nkwabyte.cropdiseasedetection.common.utils

import kotlin.math.abs
import kotlin.math.round

/**
 * Fixed-decimal formatting that works in commonMain.
 *
 * `String.format` / `"%.2f".format(x)` is a JVM-only API: it compiles on
 * Android and fails on Kotlin/Native, which is why the shared benchmark UI and
 * the iOS detector could not be compiled for iOS at all. This is the one
 * formatter both platforms use for latency figures.
 *
 * Non-finite values are spelled out rather than rounded, so a NaN in an export
 * is visible instead of being silently turned into a plausible-looking number.
 */
fun Double.formatDecimals(decimals: Int = 2): String {
    if (isNaN()) return "NaN"
    if (isInfinite()) return if (this > 0) "Infinity" else "-Infinity"
    require(decimals in 0..9) { "decimals must be 0..9, was $decimals" }

    // Beyond this magnitude the scaled value no longer fits a Long, and a
    // rounded rendering would be meaningless anyway.
    if (abs(this) >= 1e15) return toString()

    var factor = 1L
    repeat(decimals) { factor *= 10 }

    val scaled = round(abs(this) * factor).toLong()
    val whole = scaled / factor
    val fraction = scaled % factor
    val sign = if (this < 0 && scaled != 0L) "-" else ""

    return if (decimals == 0) {
        "$sign$whole"
    } else {
        "$sign$whole.${fraction.toString().padStart(decimals, '0')}"
    }
}
