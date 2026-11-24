/*
 * SPDX-FileCopyrightText: 2026 judebun
 * SPDX-License-Identifier: Apache-2.0
 */

import org.lineageos.aperture.utils.PhysicalCameraZoom
import kotlin.math.abs

fun main() {
    val lenses = listOf(
        PhysicalCameraZoom.Lens("2", 1f, 1f, 10f),
        PhysicalCameraZoom.Lens("3", 0.8f, 1f, 10f),
        PhysicalCameraZoom.Lens("4", 2.9f, 1f, 10f),
    )
    val router = PhysicalCameraZoom(lenses)
    val expected = listOf(
        1f to "2", 2f to "2", 2.89f to "2", 2.9f to "4", 3f to "4",
        5.6f to "4", 6f to "4", 3f to "4", 2.89f to "2", 1f to "2",
        0.9f to "3", 0.8f to "3", 1f to "2",
    )
    expected.forEach { (zoom, id) ->
        val target = router.target(zoom)
        check(target.cameraId == id) { "$zoom should select $id, got $target" }
        val lens = lenses.first { it.cameraId == id }
        check(abs(target.zoomRatio * lens.intrinsicZoomRatio - zoom) < 0.0001f)
    }
    // Fast forward/reverse drags preserve FOV and never send an out-of-range native request.
    val sweep = (80..2900).map { it / 100f }
    (sweep + sweep.asReversed()).forEach { zoom ->
        val target = router.target(zoom)
        val lens = lenses.first { it.cameraId == target.cameraId }
        check(target.zoomRatio in lens.minZoomRatio..lens.maxZoomRatio)
        check(abs(target.globalZoomRatio - zoom) < 0.0001f)
    }
    (0..1000).forEach {
        val progress = it / 1000f
        val zoom = router.ratioFromLinearZoom(progress)
        check(abs(router.linearZoomFromRatio(zoom) - progress) < 0.00001f)
    }
    check(router.target(-100f).globalZoomRatio == router.minZoomRatio)
    check(router.target(1000f).globalZoomRatio == router.maxZoomRatio)
    check(runCatching { router.target(Float.NaN) }.isFailure)
    check(runCatching { PhysicalCameraZoom(emptyList()) }.isFailure)
    check(runCatching { PhysicalCameraZoom(lenses + lenses.first()) }.isFailure)
    // Cameras whose native minimum is not one must switch at the actual supported FOV.
    val cropped = PhysicalCameraZoom(listOf(
        PhysicalCameraZoom.Lens("main", 1f, 1f, 10f),
        PhysicalCameraZoom.Lens("tele", 3f, 1.2f, 10f),
    ))
    check(cropped.target(3.5f).cameraId == "main")
    check(cropped.target(3.7f).cameraId == "tele")
    val extendedLenses = listOf(
        PhysicalCameraZoom.Lens("2", 1f, 1f, 10f, 100f),
        PhysicalCameraZoom.Lens("3", 0.8f, 1f, 10f, 100f),
        PhysicalCameraZoom.Lens("4", 2.86f, 1f, 10f, 100f),
    )
    val extended = PhysicalCameraZoom(extendedLenses, 120f)
    check(abs(extended.maxZoomRatio - 120f) < 0.0001f)
    check(extended.maxZoomRatioForCamera("2") == 10f)
    for (zoom in listOf(3f, 28.6f, 30f, 60f, 120f, 60f, 30f, 28.6f, 3f, 1f)) {
        val target = extended.target(zoom)
        check(target.cameraId == if (zoom < 2.86f) "2" else "4")
        check(abs(target.globalZoomRatio - zoom) < 0.0001f)
        check(abs(extended.ratioFromLinearZoom(extended.linearZoomFromRatio(zoom)) - zoom) < 0.002f)
    }
    check(abs(extended.target(120f).zoomRatio - 41.95804f) < 0.0001f)
    check(abs(PhysicalCameraZoom(extendedLenses).maxZoomRatio - 28.6f) < 0.0001f)
    val limited = PhysicalCameraZoom(extendedLenses.map {
        if (it.cameraId == "4") it.copy(extendedMaxZoomRatio = 20f) else it
    }, 120f)
    check(abs(limited.maxZoomRatio - 57.2f) < 0.0001f)
    val unsupported = PhysicalCameraZoom(extendedLenses.map {
        it.copy(extendedMaxZoomRatio = it.maxZoomRatio)
    }, 120f)
    check(abs(unsupported.maxZoomRatio - 28.6f) < 0.0001f)
    println("PASS: opt-in 120x, actual native conversion, reverse zoom, vendor capability caps, safe fallback")
    println("PASS: lens handoffs, reverse zoom, FOV continuity, native limits, slider round trips, invalid inputs")
}
