package com.ditherlab.ultra.engine.color

import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Saf matematiksel Oklab renk uzayı işlemleri.
 * sRGB -> LinearRGB -> Oklab dönüşümleri ve ΔE (Euclidean distance) hesaplamaları içerir.
 */
object OklabColorSpace {

    data class Oklab(val L: Float, val a: Float, val b: Float)

    /**
     * sRGB bileşenini Linear RGB'ye dönüştürür.
     */
    private fun sRgbToLinear(c: Float): Float {
        return if (c >= 0.04045f) {
            ((c + 0.055f) / 1.055f).pow(2.4f)
        } else {
            c / 12.92f
        }
    }

    /**
     * [0, 1] aralığındaki r, g, b değerlerini Oklab'a dönüştürür.
     */
    fun sRgbToOklab(r: Float, g: Float, b: Float): Oklab {
        val lr = sRgbToLinear(r)
        val lg = sRgbToLinear(g)
        val lb = sRgbToLinear(b)

        val l = 0.4122214708f * lr + 0.5363325363f * lg + 0.0514459929f * lb
        val m = 0.2119034982f * lr + 0.6806995451f * lg + 0.1073969566f * lb
        val s = 0.0883024619f * lr + 0.2817188376f * lg + 0.6299787005f * lb

        val l_ = cbrt(l.toDouble()).toFloat()
        val m_ = cbrt(m.toDouble()).toFloat()
        val s_ = cbrt(s.toDouble()).toFloat()

        return Oklab(
            L = 0.2104542553f * l_ + 0.7936177850f * m_ - 0.0040720468f * s_,
            a = 1.9779984951f * l_ - 2.4285922050f * m_ + 0.4505937099f * s_,
            b = 0.0259040371f * l_ + 0.7827717662f * m_ - 0.8086757660f * s_
        )
    }

    /**
     * Algısal parlaklık (Luminance) hesaplaması. Y = 0.2126R + 0.7152G + 0.0722B
     */
    fun calculateLuminance(r: Float, g: Float, b: Float): Float {
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    /**
     * İki Oklab rengi arasındaki Öklid (Euclidean) mesafesini (ΔE) hesaplar.
     */
    fun deltaE(color1: Oklab, color2: Oklab): Float {
        val dL = color1.L - color2.L
        val da = color1.a - color2.a
        val db = color1.b - color2.b
        return sqrt((dL * dL + da * da + db * db).toDouble()).toFloat()
    }
}
