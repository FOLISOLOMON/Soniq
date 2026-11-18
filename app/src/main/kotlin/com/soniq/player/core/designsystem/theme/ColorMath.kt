package com.soniq.player.core.designsystem.theme

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Small, dependency-free color utilities operating on packed ARGB integers so they can be unit
 * tested on the JVM without Android or Compose runtime support.
 */
internal object ColorMath {

    /** HSL triple: hue in [0, 360), saturation and lightness in [0, 1]. */
    data class Hsl(val hue: Float, val saturation: Float, val lightness: Float)

    fun alpha(argb: Int): Int = (argb ushr 24) and 0xFF
    fun red(argb: Int): Int = (argb shr 16) and 0xFF
    fun green(argb: Int): Int = (argb shr 8) and 0xFF
    fun blue(argb: Int): Int = argb and 0xFF

    fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
        (alpha.coerceIn(0, 255) shl 24) or
            (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)

    fun toHsl(argb: Int): Hsl {
        val r = red(argb) / 255f
        val g = green(argb) / 255f
        val b = blue(argb) / 255f
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC
        val lightness = (maxC + minC) / 2f
        if (delta == 0f) return Hsl(0f, 0f, lightness)
        val saturation = delta / (1f - abs(2f * lightness - 1f))
        val hue = when (maxC) {
            r -> 60f * (((g - b) / delta).mod(6f))
            g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        return Hsl(hue.mod(360f), saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))
    }

    fun fromHsl(hsl: Hsl, alpha: Int = 0xFF): Int {
        val h = hsl.hue.mod(360f)
        val s = hsl.saturation.coerceIn(0f, 1f)
        val l = hsl.lightness.coerceIn(0f, 1f)
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f).mod(2f) - 1f))
        val m = l - c / 2f
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return argb(
            alpha,
            Math.round((r1 + m) * 255f),
            Math.round((g1 + m) * 255f),
            Math.round((b1 + m) * 255f),
        )
    }

    /** WCAG relative luminance in [0, 1]. */
    fun relativeLuminance(argb: Int): Double {
        fun channel(value: Int): Double {
            val c = value / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(red(argb)) + 0.7152 * channel(green(argb)) + 0.0722 * channel(blue(argb))
    }

    /** WCAG contrast ratio between two opaque colors, in [1, 21]. */
    fun contrastRatio(first: Int, second: Int): Double {
        val l1 = relativeLuminance(first)
        val l2 = relativeLuminance(second)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    /**
     * Returns pure white or pure black, whichever contrasts better against [background].
     * Using true extremes guarantees a WCAG contrast ratio of at least sqrt(21) (~4.58:1)
     * for every possible RGB color.
     */
    fun readableOn(background: Int): Int {
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        return if (contrastRatio(background, white) >= contrastRatio(background, black)) white else black
    }

    /** Linear blend of two opaque colors; [fraction] 0 returns [from], 1 returns [to]. */
    fun blend(from: Int, to: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        fun mix(a: Int, b: Int) = Math.round(a + (b - a) * f)
        return argb(0xFF, mix(red(from), red(to)), mix(green(from), green(to)), mix(blue(from), blue(to)))
    }
}
