package com.king.zxing

import android.content.Intent
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.king.zxing.Intents.Scan
import java.util.Arrays
import java.util.EnumSet
import java.util.HashMap
import java.util.regex.Pattern

/*
 * Copyright (C) 2010 ZXing authors
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
 */
object DecodeFormatManager {
    private val COMMA_PATTERN = Pattern.compile(",")
    var PRODUCT_FORMATS: Set<BarcodeFormat>? = null
    var INDUSTRIAL_FORMATS: Set<BarcodeFormat>? = null
    var ONE_D_FORMATS: MutableSet<BarcodeFormat>? = null
    val QR_CODE_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.QR_CODE)
    val DATA_MATRIX_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.DATA_MATRIX)
    val AZTEC_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.AZTEC)
    val PDF417_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.PDF_417)
    private var FORMATS_FOR_MODE: MutableMap<String?, Set<BarcodeFormat>?> = HashMap()

    fun parseDecodeFormats(intent: Intent): Set<BarcodeFormat>? {
        var scanFormats: Iterable<String?>? = null
        val scanFormatsString: CharSequence? = intent.getStringExtra(Scan.FORMATS)
        if (scanFormatsString != null) {
            scanFormats =
                Arrays.asList(*COMMA_PATTERN.split(scanFormatsString))
        }
        return parseDecodeFormats(scanFormats, intent.getStringExtra(Scan.MODE))
    }

    fun parseDecodeFormats(inputUri: Uri): Set<BarcodeFormat>? {
        var formats = inputUri.getQueryParameters(Scan.FORMATS)
        if (formats != null && formats.size == 1 && formats[0] != null) {
            formats = Arrays.asList(*COMMA_PATTERN.split(formats[0]))
        }
        return parseDecodeFormats(formats, inputUri.getQueryParameter(Scan.MODE))
    }

    private fun parseDecodeFormats(
        scanFormats: Iterable<String?>?, decodeMode: String?
    ): Set<BarcodeFormat>? {
        if (scanFormats != null) {
            val formats: MutableSet<BarcodeFormat> =
                EnumSet.noneOf(BarcodeFormat::class.java)
            try {
                for (format in scanFormats) {
                    formats.add(BarcodeFormat.valueOf(format!!))
                }
                return formats
            } catch (iae: IllegalArgumentException) {
                // ignore it then
            }
        }
        return if (decodeMode != null) {
            FORMATS_FOR_MODE[decodeMode]
        } else null
    }

    init {
        PRODUCT_FORMATS = EnumSet.of(BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.RSS_14,
            BarcodeFormat.RSS_EXPANDED)
        INDUSTRIAL_FORMATS = EnumSet.of(BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.CODE_128,
            BarcodeFormat.ITF,
            BarcodeFormat.CODABAR).also {
            ONE_D_FORMATS = EnumSet.copyOf(PRODUCT_FORMATS)
            ONE_D_FORMATS?.addAll(it)
        }

        FORMATS_FOR_MODE[Scan.ONE_D_MODE] = ONE_D_FORMATS
        FORMATS_FOR_MODE[Scan.PRODUCT_MODE] = PRODUCT_FORMATS
        FORMATS_FOR_MODE[Scan.QR_CODE_MODE] = QR_CODE_FORMATS
        FORMATS_FOR_MODE[Scan.DATA_MATRIX_MODE] = DATA_MATRIX_FORMATS
        FORMATS_FOR_MODE[Scan.AZTEC_MODE] = AZTEC_FORMATS
        FORMATS_FOR_MODE[Scan.PDF417_MODE] = PDF417_FORMATS
    }
}