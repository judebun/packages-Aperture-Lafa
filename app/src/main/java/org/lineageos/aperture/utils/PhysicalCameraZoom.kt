/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

/** Converts a single field-of-view zoom range to independent camera zoom requests. */
class PhysicalCameraZoom(lenses: List<Lens>, requestedMaxZoomRatio: Float? = null) {
    data class Lens(
        val cameraId: String,
        val intrinsicZoomRatio: Float,
        val minZoomRatio: Float,
        val maxZoomRatio: Float,
        val extendedMaxZoomRatio: Float = maxZoomRatio,
    )

    data class Target(val cameraId: String, val zoomRatio: Float, val globalZoomRatio: Float)

    private val longestLensId = lenses.maxByOrNull { it.intrinsicZoomRatio }?.cameraId
    private val lenses = lenses.map { lens ->
        if (lens.cameraId == longestLensId && requestedMaxZoomRatio != null &&
            requestedMaxZoomRatio.isFinite() && requestedMaxZoomRatio > 0f &&
            lens.extendedMaxZoomRatio.isFinite() && lens.extendedMaxZoomRatio > lens.maxZoomRatio) {
            lens.copy(maxZoomRatio = maxOf(lens.maxZoomRatio, minOf(
                lens.extendedMaxZoomRatio, requestedMaxZoomRatio / lens.intrinsicZoomRatio
            )))
        } else lens
    }.sortedBy { it.intrinsicZoomRatio * it.minZoomRatio }

    init {
        require(this.lenses.isNotEmpty())
        require(this.lenses.all {
            it.intrinsicZoomRatio.isFinite() && it.intrinsicZoomRatio > 0f &&
                    it.minZoomRatio.isFinite() && it.minZoomRatio > 0f &&
                    it.maxZoomRatio.isFinite() && it.maxZoomRatio >= it.minZoomRatio
        })
        require(this.lenses.map { it.cameraId }.distinct().size == this.lenses.size)
    }

    val minZoomRatio = this.lenses.minOf { it.intrinsicZoomRatio * it.minZoomRatio }
    val maxZoomRatio = this.lenses.maxOf { it.intrinsicZoomRatio * it.maxZoomRatio }

    fun maxZoomRatioForCamera(cameraId: String): Float? =
        lenses.firstOrNull { it.cameraId == cameraId }?.maxZoomRatio

    fun target(zoomRatio: Float): Target {
        require(zoomRatio.isFinite())
        val globalZoomRatio = zoomRatio.coerceIn(minZoomRatio, maxZoomRatio)
        val lens = lenses.lastOrNull {
            globalZoomRatio >= it.intrinsicZoomRatio * it.minZoomRatio &&
                    globalZoomRatio <= it.intrinsicZoomRatio * it.maxZoomRatio
        } ?: lenses.minBy {
            kotlin.math.abs(globalZoomRatio - globalZoomRatio.coerceIn(
                it.intrinsicZoomRatio * it.minZoomRatio,
                it.intrinsicZoomRatio * it.maxZoomRatio,
            ))
        }
        val nativeZoomRatio = (globalZoomRatio / lens.intrinsicZoomRatio)
            .coerceIn(lens.minZoomRatio, lens.maxZoomRatio)
        return Target(lens.cameraId, nativeZoomRatio, nativeZoomRatio * lens.intrinsicZoomRatio)
    }

    fun ratioFromLinearZoom(linearZoom: Float): Float {
        require(linearZoom.isFinite())
        val progress = linearZoom.coerceIn(0f, 1f)
        return 1f / ((1f - progress) / minZoomRatio + progress / maxZoomRatio)
    }

    fun linearZoomFromRatio(zoomRatio: Float): Float = when (minZoomRatio) {
        maxZoomRatio -> 0f
        else -> ((1f / minZoomRatio - 1f / zoomRatio.coerceIn(minZoomRatio, maxZoomRatio)) /
                (1f / minZoomRatio - 1f / maxZoomRatio)).coerceIn(0f, 1f)
    }
}
