/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.repositories

import android.content.Context
import androidx.camera.video.Quality
import org.lineageos.aperture.R
import org.lineageos.aperture.ext.getOrCreate
import org.lineageos.aperture.models.FrameRate
import kotlin.math.absoluteValue

/**
 * Overlays repository.
 */
class OverlaysRepository(private val context: Context) {
    private inner class RROResources(private val packageName: String) {
        private val resources = context.packageManager.getResourcesForApplication(packageName)

        fun getBoolean(id: Int): Boolean = resources.getBoolean(getIdentifier(id))

        fun getStringArray(id: Int): Array<String> = resources.getStringArray(getIdentifier(id))

        @Suppress("DiscouragedApi")
        fun getStringArray(name: String): Array<String> = resources.getStringArray(
            resources.getIdentifier(name, "array", packageName)
        )

        @Suppress("DiscouragedApi")
        fun getInteger(name: String): Int = resources.getInteger(
            resources.getIdentifier(name, "integer", packageName)
        )

        @Suppress("DiscouragedApi")
        private fun getIdentifier(id: Int) = resources.getIdentifier(
            context.resources.getResourceEntryName(id),
            context.resources.getResourceTypeName(id),
            packageName,
        )
    }

    private val rroResources = listOf(
        "org.lineageos.aperture.auto_generated_rro_product__",
        "org.lineageos.aperture.auto_generated_rro_vendor__",
    ).mapNotNull {
        runCatching { RROResources(it) }.getOrNull()
    }

    /**
     * @see R.bool.config_enableAuxCameras
     */
    val enableAuxCameras = getBoolean(R.bool.config_enableAuxCameras)

    /** Standalone rear camera IDs to use for continuous zoom in ordinary photo mode. */
    @Suppress("DiscouragedApi")
    val physicalZoomCameraIds = (rroResources.firstNotNullOfOrNull {
        runCatching { it.getStringArray("config_physicalZoomCameraIds") }.getOrNull()
    } ?: context.resources.getIdentifier(
        "config_physicalZoomCameraIds", "array", context.packageName
    ).takeIf { it != 0 }?.let { context.resources.getStringArray(it) })?.toSet().orEmpty()

    /** Optional global photo zoom ceiling, bounded by the long lens's advertised extended range. */
    @Suppress("DiscouragedApi")
    val physicalZoomMaxRatio = (rroResources.firstNotNullOfOrNull {
        runCatching { it.getInteger("config_physicalZoomMaxRatio") }.getOrNull()
    } ?: context.resources.getIdentifier(
        "config_physicalZoomMaxRatio", "integer", context.packageName
    ).takeIf { it != 0 }?.let { context.resources.getInteger(it) } ?: 0)
        .takeIf { it > 0 }?.toFloat()

    /**
     * @see R.array.config_ignoredAuxCameraIds
     */
    val ignoredAuxCameraIds = getStringArray(R.array.config_ignoredAuxCameraIds)

    /**
     * @see R.bool.config_ignoreLogicalAuxCameras
     */
    val ignoreLogicalAuxCameras = getBoolean(R.bool.config_ignoreLogicalAuxCameras)

    /**
     * @see R.array.config_backwardCompatibleCameraIds
     */
    val backwardCompatibleCameraIds = getStringArray(R.array.config_backwardCompatibleCameraIds)

    /**
     * @see R.array.config_additionalVideoConfigurations
     */
    val additionalVideoConfigurations =
        buildMap<String, MutableMap<Quality, MutableMap<FrameRate, Boolean>>> {
            getStringArray(R.array.config_additionalVideoConfigurations)
                .let {
                    if (it.size % 3 != 0) {
                        // Invalid configuration
                        return@buildMap
                    }

                    for (i in it.indices step 3) {
                        val cameraId = it[i]
                        val frameRates = it[i + 2].split("|").mapNotNull { frameRate ->
                            FrameRate.fromValue(frameRate.toInt().absoluteValue)?.let { value ->
                                value to frameRate.startsWith('-')
                            }
                        }.toMap()

                        it[i + 1].split("|").mapNotNull { quality ->
                            when (quality) {
                                "sd" -> Quality.SD
                                "hd" -> Quality.HD
                                "fhd" -> Quality.FHD
                                "qhd" -> Quality.QHD
                                "uhd" -> Quality.UHD
                                else -> null
                            }
                        }.distinct().forEach { quality ->
                            getOrCreate(cameraId).apply {
                                getOrCreate(quality).apply {
                                    putAll(frameRates)
                                }
                            }
                        }
                    }
                }
        }.map { a ->
            a.key to a.value.map { b ->
                b.key to b.value.toMap()
            }.toMap()
        }.toMap()

    /**
     * @see R.array.config_logicalZoomRatios
     */
    val logicalZoomRatios = buildMap<String, MutableMap<Float, Float>> {
        getStringArray(R.array.config_logicalZoomRatios).let {
            if (it.size % 3 != 0) {
                // Invalid configuration
                return@buildMap
            }

            for (i in it.indices step 3) {
                val cameraId = it[i]
                val approximateZoomRatio = it[i + 1].toFloat()
                val exactZoomRatio = it[i + 2].toFloat()

                getOrCreate(cameraId).apply {
                    this[approximateZoomRatio] = exactZoomRatio
                }
            }
        }
    }.map { a ->
        a.key to a.value.toMap()
    }.toMap()

    /**
     * @see R.bool.config_enableHighResolution
     */
    val enableHighResolution = getBoolean(R.bool.config_enableHighResolution)

    private fun getBoolean(id: Int) = rroResources.firstNotNullOfOrNull {
        runCatching {
            it.getBoolean(id)
        }.getOrNull()
    } ?: context.resources.getBoolean(id)

    private fun getStringArray(id: Int) = rroResources.firstNotNullOfOrNull {
        runCatching {
            it.getStringArray(id)
        }.getOrNull()
    } ?: context.resources.getStringArray(id)
}
