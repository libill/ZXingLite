/*
 * Copyright (C) 2018 Jenly Yu
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
package com.king.zxing.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.king.zxing.CaptureActivity
import com.king.zxing.app.util.immersiveStatusBar

/**
 * @author Jenly [Jenly](mailto:jenly1314@gmail.com)
 */
class EasyCaptureActivity : CaptureActivity() {
    override fun getLayoutId(): Int {
        return R.layout.easy_capture_activity
    }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        immersiveStatusBar(this, toolbar, 0.2f)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        tvTitle.text = intent.getStringExtra(MainActivity.Companion.KEY_TITLE)
        captureHelper //                .decodeFormats(DecodeFormatManager.QR_CODE_FORMATS)//设置只识别二维码会提升速度
            .playBeep(true)
            .vibrate(true)
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.ivLeft -> onBackPressed()
        }
    }
}