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

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.king.zxing.app.MainActivity.Companion.KEY_IS_QR_CODE
import com.king.zxing.app.MainActivity.Companion.KEY_TITLE
import com.king.zxing.util.CodeUtils

/**
 * @author Jenly [Jenly](mailto:jenly1314@gmail.com)
 */
class CodeActivity : AppCompatActivity() {
    private var tvTitle: TextView? = null
    private var ivCode: ImageView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.code_activity)
        ivCode = findViewById(R.id.ivCode)
        tvTitle = findViewById(R.id.tvTitle)
        tvTitle?.text = intent.getStringExtra(KEY_TITLE)
        val isQRCode = intent.getBooleanExtra(KEY_IS_QR_CODE, false)
        if (isQRCode) {
            createQRCode(getString(R.string.app_name))
        } else {
            createBarCode("1234567890")
        }
    }

    /**
     * 生成二维码
     * @param content
     */
    private fun createQRCode(content: String) {
        Thread(Runnable {

            //生成二维码相关放在子线程里面
            val logo = BitmapFactory.decodeResource(resources, R.drawable.logo)
            val bitmap = CodeUtils.createQRCode(content, 600, logo)
            runOnUiThread {
                //显示二维码
                ivCode!!.setImageBitmap(bitmap)
            }
        }).start()
    }

    /**
     * 生成条形码
     * @param content
     */
    private fun createBarCode(content: String) {
        Thread(Runnable {

            //生成条形码相关放在子线程里面
            val bitmap = CodeUtils.createBarCode(content, BarcodeFormat.CODE_128, 800, 200, null, true)
            runOnUiThread {
                //显示条形码
                ivCode!!.setImageBitmap(bitmap)
            }
        }).start()
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.ivLeft -> onBackPressed()
        }
    }
}