package com.king.zxing.camera

import android.graphics.Point
import android.graphics.Rect
import android.hardware.Camera
import android.os.Build
import com.king.zxing.util.LogUtils
import java.util.Arrays
import java.util.regex.Pattern

/*
 * Copyright (C) 2014 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ /**
 * Utility methods for configuring the Android camera.
 *
 * @author Sean Owen
 */
// camera APIs
object CameraConfigurationUtils {
    private val SEMICOLON = Pattern.compile(";")
    private const val MIN_PREVIEW_PIXELS = 480 * 320 // normal screen
    private const val MAX_EXPOSURE_COMPENSATION = 1.5f
    private const val MIN_EXPOSURE_COMPENSATION = 0.0f
    private const val MAX_ASPECT_DISTORTION = 0.05
    private const val MIN_FPS = 10
    private const val MAX_FPS = 20
    private const val AREA_PER_1000 = 400
    fun setFocus(
        parameters: Camera.Parameters,
        autoFocus: Boolean,
        disableContinuous: Boolean,
        safeMode: Boolean
    ) {
        val supportedFocusModes = parameters.supportedFocusModes
        var focusMode: String? = null
        if (autoFocus) {
            focusMode = if (safeMode || disableContinuous) {
                findSettableValue("focus mode",
                    supportedFocusModes,
                    Camera.Parameters.FOCUS_MODE_AUTO)
            } else {
                findSettableValue("focus mode",
                    supportedFocusModes,
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
                    Camera.Parameters.FOCUS_MODE_AUTO)
            }
        }
        // Maybe selected auto-focus but not available, so fall through here:
        if (!safeMode && focusMode == null) {
            focusMode = findSettableValue("focus mode",
                supportedFocusModes,
                Camera.Parameters.FOCUS_MODE_MACRO,
                Camera.Parameters.FOCUS_MODE_EDOF)
        }
        if (focusMode != null) {
            if (focusMode == parameters.focusMode) {
                LogUtils.Companion.d("Focus mode already set to $focusMode")
            } else {
                parameters.focusMode = focusMode
            }
        }
    }

    fun setTorch(parameters: Camera.Parameters, on: Boolean) {
        val supportedFlashModes = parameters.supportedFlashModes
        val flashMode: String?
        flashMode = if (on) {
            findSettableValue("flash mode",
                supportedFlashModes,
                Camera.Parameters.FLASH_MODE_TORCH,
                Camera.Parameters.FLASH_MODE_ON)
        } else {
            findSettableValue("flash mode",
                supportedFlashModes,
                Camera.Parameters.FLASH_MODE_OFF)
        }
        if (flashMode != null) {
            if (flashMode == parameters.flashMode) {
                LogUtils.Companion.d("Flash mode already set to $flashMode")
            } else {
                LogUtils.Companion.d("Setting flash mode to $flashMode")
                parameters.flashMode = flashMode
            }
        }
    }

    fun setBestExposure(parameters: Camera.Parameters, lightOn: Boolean) {
        val minExposure = parameters.minExposureCompensation
        val maxExposure = parameters.maxExposureCompensation
        val step = parameters.exposureCompensationStep
        if ((minExposure != 0 || maxExposure != 0) && step > 0.0f) {
            // Set low when light is on
            val targetCompensation =
                if (lightOn) MIN_EXPOSURE_COMPENSATION else MAX_EXPOSURE_COMPENSATION
            var compensationSteps = Math.round(targetCompensation / step)
            val actualCompensation = step * compensationSteps
            // Clamp value:
            compensationSteps = Math.max(Math.min(compensationSteps, maxExposure), minExposure)
            if (parameters.exposureCompensation == compensationSteps) {
                LogUtils.Companion.d(
                    "Exposure compensation already set to $compensationSteps / $actualCompensation")
            } else {
                LogUtils.Companion.d(
                    "Setting exposure compensation to $compensationSteps / $actualCompensation")
                parameters.exposureCompensation = compensationSteps
            }
        } else {
            LogUtils.Companion.d("Camera does not support exposure compensation")
        }
    }

    fun setBestPreviewFPS(parameters: Camera.Parameters) {
        setBestPreviewFPS(parameters, MIN_FPS,
            MAX_FPS)
    }

    fun setBestPreviewFPS(parameters: Camera.Parameters, minFPS: Int, maxFPS: Int) {
        val supportedPreviewFpsRanges = parameters.supportedPreviewFpsRange
        LogUtils.Companion.d("Supported FPS ranges: " + toString(supportedPreviewFpsRanges))
        if (supportedPreviewFpsRanges != null && !supportedPreviewFpsRanges.isEmpty()) {
            var suitableFPSRange: IntArray? = null
            for (fpsRange in supportedPreviewFpsRanges) {
                val thisMin = fpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]
                val thisMax = fpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
                if (thisMin >= minFPS * 1000 && thisMax <= maxFPS * 1000) {
                    suitableFPSRange = fpsRange
                    break
                }
            }
            if (suitableFPSRange == null) {
                LogUtils.Companion.d("No suitable FPS range?")
            } else {
                val currentFpsRange = IntArray(2)
                parameters.getPreviewFpsRange(currentFpsRange)
                if (Arrays.equals(currentFpsRange, suitableFPSRange)) {
                    LogUtils.Companion.d("FPS range already set to " + Arrays.toString(suitableFPSRange))
                } else {
                    LogUtils.Companion.d("Setting FPS range to " + Arrays.toString(suitableFPSRange))
                    parameters.setPreviewFpsRange(
                        suitableFPSRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                        suitableFPSRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX])
                }
            }
        }
    }

    fun setFocusArea(parameters: Camera.Parameters) {
        if (parameters.maxNumFocusAreas > 0) {
            LogUtils.Companion.d("Old focus areas: " + toString(parameters.focusAreas))
            val middleArea =
                buildMiddleArea(AREA_PER_1000)
            LogUtils.Companion.d("Setting focus area to : " + toString(middleArea))
            parameters.focusAreas = middleArea
        } else {
            LogUtils.Companion.d("Device does not support focus areas")
        }
    }

    fun setMetering(parameters: Camera.Parameters) {
        if (parameters.maxNumMeteringAreas > 0) {
            LogUtils.Companion.d("Old metering areas: " + parameters.meteringAreas)
            val middleArea =
                buildMiddleArea(AREA_PER_1000)
            LogUtils.Companion.d("Setting metering area to : " + toString(middleArea))
            parameters.meteringAreas = middleArea
        } else {
            LogUtils.Companion.d("Device does not support metering areas")
        }
    }

    private fun buildMiddleArea(areaPer1000: Int): List<Camera.Area> {
        return listOf(
            Camera.Area(Rect(-areaPer1000, -areaPer1000, areaPer1000, areaPer1000),
                1))
    }

    fun setVideoStabilization(parameters: Camera.Parameters) {
        if (parameters.isVideoStabilizationSupported) {
            if (parameters.videoStabilization) {
                LogUtils.Companion.d("Video stabilization already enabled")
            } else {
                LogUtils.Companion.d("Enabling video stabilization...")
                parameters.videoStabilization = true
            }
        } else {
            LogUtils.Companion.d("This device does not support video stabilization")
        }
    }

    fun setBarcodeSceneMode(parameters: Camera.Parameters) {
        if (Camera.Parameters.SCENE_MODE_BARCODE == parameters.sceneMode) {
            LogUtils.Companion.d("Barcode scene mode already set")
            return
        }
        val sceneMode = findSettableValue("scene mode",
            parameters.supportedSceneModes,
            Camera.Parameters.SCENE_MODE_BARCODE)
        if (sceneMode != null) {
            parameters.sceneMode = sceneMode
        }
    }

    fun setZoom(parameters: Camera.Parameters, targetZoomRatio: Double) {
        if (parameters.isZoomSupported) {
            val zoom = indexOfClosestZoom(parameters, targetZoomRatio) ?: return
            if (parameters.zoom == zoom) {
                LogUtils.Companion.d("Zoom is already set to $zoom")
            } else {
                LogUtils.Companion.d("Setting zoom to $zoom")
                parameters.zoom = zoom
            }
        } else {
            LogUtils.Companion.d("Zoom is not supported")
        }
    }

    private fun indexOfClosestZoom(
        parameters: Camera.Parameters, targetZoomRatio: Double
    ): Int? {
        val ratios = parameters.zoomRatios
        LogUtils.Companion.d("Zoom ratios: $ratios")
        val maxZoom = parameters.maxZoom
        if (ratios == null || ratios.isEmpty() || ratios.size != maxZoom + 1) {
            LogUtils.Companion.w("Invalid zoom ratios!")
            return null
        }
        val target100 = 100.0 * targetZoomRatio
        var smallestDiff = Double.POSITIVE_INFINITY
        var closestIndex = 0
        for (i in ratios.indices) {
            val diff = Math.abs(ratios[i] - target100)
            if (diff < smallestDiff) {
                smallestDiff = diff
                closestIndex = i
            }
        }
        LogUtils.Companion.d("Chose zoom ratio of " + ratios[closestIndex] / 100.0)
        return closestIndex
    }

    fun setInvertColor(parameters: Camera.Parameters) {
        if (Camera.Parameters.EFFECT_NEGATIVE == parameters.colorEffect) {
            LogUtils.Companion.d("Negative effect already set")
            return
        }
        val colorMode = findSettableValue("color effect",
            parameters.supportedColorEffects,
            Camera.Parameters.EFFECT_NEGATIVE)
        if (colorMode != null) {
            parameters.colorEffect = colorMode
        }
    }

    fun findBestPreviewSizeValue(
        parameters: Camera.Parameters, screenResolution: Point?
    ): Point {
        val rawSupportedSizes =
            parameters.supportedPreviewSizes
        if (rawSupportedSizes == null) {
            LogUtils.Companion.w("Device returned no supported preview sizes; using default")
            val defaultSize =
                parameters.previewSize ?: throw IllegalStateException("Parameters contained no preview size!")
            return Point(defaultSize.width, defaultSize.height)
        }
        if (LogUtils.Companion.isShowLog()) {
            val previewSizesString = StringBuilder()
            for (size in rawSupportedSizes) {
                previewSizesString.append(size.width).append('x').append(size.height).append(' ')
            }
            LogUtils.Companion.d("Supported preview sizes: $previewSizesString")
        }
        val screenAspectRatio: Double
        screenAspectRatio = if (screenResolution!!.x < screenResolution.y) {
            screenResolution.x / screenResolution.y.toDouble()
        } else {
            screenResolution.y / screenResolution.x.toDouble()
        }
        LogUtils.Companion.d("screenAspectRatio: $screenAspectRatio")
        // Find a suitable size, with max resolution
        var maxResolution = 0
        var maxResPreviewSize: Camera.Size? = null
        for (size in rawSupportedSizes) {
            val realWidth = size.width
            val realHeight = size.height
            val resolution = realWidth * realHeight
            if (resolution < MIN_PREVIEW_PIXELS) {
                continue
            }
            val isCandidatePortrait = realWidth < realHeight
            val maybeFlippedWidth = if (isCandidatePortrait) realWidth else realHeight
            val maybeFlippedHeight = if (isCandidatePortrait) realHeight else realWidth
            LogUtils.Companion.d(String.format("maybeFlipped:%d * %d", maybeFlippedWidth, maybeFlippedHeight))
            val aspectRatio = maybeFlippedWidth / maybeFlippedHeight.toDouble()
            LogUtils.Companion.d("aspectRatio: $aspectRatio")
            val distortion = Math.abs(aspectRatio - screenAspectRatio)
            LogUtils.Companion.d("distortion: $distortion")
            if (distortion > MAX_ASPECT_DISTORTION) {
                continue
            }
            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                val exactPoint = Point(realWidth, realHeight)
                LogUtils.Companion.d("Found preview size exactly matching screen size: $exactPoint")
                return exactPoint
            }

            // Resolution is suitable; record the one with max resolution
            if (resolution > maxResolution) {
                maxResolution = resolution
                maxResPreviewSize = size
            }
        }

        // If no exact match, use largest preview size. This was not a great idea on older devices because
        // of the additional computation needed. We're likely to get here on newer Android 4+ devices, where
        // the CPU is much more powerful.
        if (maxResPreviewSize != null) {
            val largestSize =
                Point(maxResPreviewSize.width, maxResPreviewSize.height)
            LogUtils.Companion.d("Using largest suitable preview size: $largestSize")
            return largestSize
        }

        // If there is nothing at all suitable, return current preview size
        val defaultPreview =
            parameters.previewSize ?: throw IllegalStateException("Parameters contained no preview size!")
        val defaultSize = Point(defaultPreview.width, defaultPreview.height)
        LogUtils.Companion.d("No suitable preview sizes, using default: $defaultSize")
        return defaultSize
    }

    private fun findSettableValue(
        name: String,
        supportedValues: Collection<String>?,
        vararg desiredValues: String
    ): String? {
        LogUtils.Companion.d("Requesting " + name + " value from among: " + Arrays.toString(desiredValues))
        LogUtils.Companion.d("Supported $name values: $supportedValues")
        if (supportedValues != null) {
            for (desiredValue in desiredValues) {
                if (supportedValues.contains(desiredValue)) {
                    LogUtils.Companion.d("Can set $name to: $desiredValue")
                    return desiredValue
                }
            }
        }
        LogUtils.Companion.d("No supported values match")
        return null
    }

    private fun toString(arrays: Collection<IntArray>?): String {
        if (arrays == null || arrays.isEmpty()) {
            return "[]"
        }
        val buffer = StringBuilder()
        buffer.append('[')
        val it = arrays.iterator()
        while (it.hasNext()) {
            buffer.append(Arrays.toString(it.next()))
            if (it.hasNext()) {
                buffer.append(", ")
            }
        }
        buffer.append(']')
        return buffer.toString()
    }

    private fun toString(areas: Iterable<Camera.Area>?): String? {
        if (areas == null) {
            return null
        }
        val result = StringBuilder()
        for (area in areas) {
            result.append(area.rect).append(':').append(area.weight).append(' ')
        }
        return result.toString()
    }

    fun collectStats(parameters: Camera.Parameters): String {
        return collectStats(parameters.flatten())
    }

    fun collectStats(flattenedParams: CharSequence?): String {
        val result = StringBuilder(1000)
        result.append("BOARD=").append(Build.BOARD).append('\n')
        result.append("BRAND=").append(Build.BRAND).append('\n')
        result.append("CPU_ABI=").append(Build.CPU_ABI).append('\n')
        result.append("DEVICE=").append(Build.DEVICE).append('\n')
        result.append("DISPLAY=").append(Build.DISPLAY).append('\n')
        result.append("FINGERPRINT=").append(Build.FINGERPRINT).append('\n')
        result.append("HOST=").append(Build.HOST).append('\n')
        result.append("ID=").append(Build.ID).append('\n')
        result.append("MANUFACTURER=").append(Build.MANUFACTURER).append('\n')
        result.append("MODEL=").append(Build.MODEL).append('\n')
        result.append("PRODUCT=").append(Build.PRODUCT).append('\n')
        result.append("TAGS=").append(Build.TAGS).append('\n')
        result.append("TIME=").append(Build.TIME).append('\n')
        result.append("TYPE=").append(Build.TYPE).append('\n')
        result.append("USER=").append(Build.USER).append('\n')
        result.append("VERSION.CODENAME=").append(Build.VERSION.CODENAME).append('\n')
        result.append("VERSION.INCREMENTAL=").append(Build.VERSION.INCREMENTAL).append('\n')
        result.append("VERSION.RELEASE=").append(Build.VERSION.RELEASE).append('\n')
        result.append("VERSION.SDK_INT=").append(Build.VERSION.SDK_INT).append('\n')
        if (flattenedParams != null) {
            val params = SEMICOLON.split(flattenedParams)
            Arrays.sort(params)
            for (param in params) {
                result.append(param).append('\n')
            }
        }
        return result.toString()
    }
}