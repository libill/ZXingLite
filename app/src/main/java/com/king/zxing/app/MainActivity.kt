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

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import com.king.zxing.CaptureActivity
import com.king.zxing.Intents
import com.king.zxing.app.util.getImagePath
import com.king.zxing.util.CodeUtils
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks

/**
 * 扫码Demo示例说明
 *
 * 快速实现扫码有以下几种方式：
 *
 * 1、直接使用CaptureActivity或者CaptureFragment。(纯洁的扫码，无任何添加剂)
 *
 * 2、通过继承CaptureActivity或者CaptureFragment并自定义布局。（适用于大多场景，并无需关心扫码相关逻辑，自定义布局时需覆写getLayoutId方法）
 *
 * 3、在你项目的Activity或者Fragment中创建一个CaptureHelper并在相应的生命周期中调用CaptureHelper的周期。（适用于想在扫码界面写交互逻辑，又因为项目架构或其它原因，无法直接或间接继承CaptureActivity或CaptureFragment时使用）
 *
 * 4、参照CaptureHelper写一个自定义的扫码帮助类，其它步骤同方式3。（扩展高级用法，谨慎使用）
 *
 */
class MainActivity : AppCompatActivity(), PermissionCallbacks {
    private var cls: Class<*>? = null
    private var title: String? = null
    private var isContinuousScan = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_SCAN -> {
                    val result = data.getStringExtra(Intents.Scan.RESULT)
                    Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
                }
                REQUEST_CODE_PHOTO -> parsePhoto(data)
            }
        }
    }

    private fun parsePhoto(data: Intent) {
        val path: String? = getImagePath(this, data)
        Log.d("Jenly", "path:$path")
        if (TextUtils.isEmpty(path)) {
            return
        }
        //异步解析
        asyncThread(Runnable {
            val result = CodeUtils.parseCode(path)
            runOnUiThread {
                Log.d("Jenly", "result:$result")
                Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(
        requestCode: Int, list: List<String>
    ) {
        // Some permissions have been granted
    }

    override fun onPermissionsDenied(
        requestCode: Int, list: List<String>
    ) {
        // Some permissions have been denied
        // ...
    }

    /**
     * 检测拍摄权限
     */
    @AfterPermissionGranted(RC_CAMERA)
    private fun checkCameraPermissions() {
        val perms = arrayOf(Manifest.permission.CAMERA)
        if (EasyPermissions.hasPermissions(this, *perms)) { //有权限
            startScan(cls, title)
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.permission_camera),
                RC_CAMERA, *perms)
        }
    }

    private fun asyncThread(runnable: Runnable) {
        Thread(runnable).start()
    }

    /**
     * 扫码
     * @param cls
     * @param title
     */
    private fun startScan(cls: Class<*>?, title: String?) {
        val optionsCompat =
            ActivityOptionsCompat.makeCustomAnimation(this, R.anim.`in`, R.anim.out)
        val intent = Intent(this, cls)
        intent.putExtra(KEY_TITLE, title)
        intent.putExtra(KEY_IS_CONTINUOUS, isContinuousScan)
        ActivityCompat.startActivityForResult(this, intent, REQUEST_CODE_SCAN,
            optionsCompat.toBundle())
    }

    /**
     * 生成二维码/条形码
     * @param isQRCode
     */
    private fun startCode(isQRCode: Boolean) {
        val intent = Intent(this, CodeActivity::class.java)
        intent.putExtra(KEY_IS_QR_CODE, isQRCode)
        intent.putExtra(KEY_TITLE,
            if (isQRCode) getString(R.string.qr_code) else getString(R.string.bar_code))
        startActivity(intent)
    }

    private fun startPhotoCode() {
        val pickIntent = Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(pickIntent, REQUEST_CODE_PHOTO)
    }

    @AfterPermissionGranted(RC_READ_PHOTO)
    private fun checkExternalStoragePermissions() {
        val perms =
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (EasyPermissions.hasPermissions(this, *perms)) { //有权限
            startPhotoCode()
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.permission_external_storage),
                RC_READ_PHOTO, *perms)
        }
    }

    fun onClick(v: View) {
        isContinuousScan = false
        when (v.id) {
            R.id.btn0 -> {
                cls = CaptureActivity::class.java
                title = (v as Button).text.toString()
                checkCameraPermissions()
            }
            R.id.btn1 -> {
                cls = CustomCaptureActivity::class.java
                title = (v as Button).text.toString()
                isContinuousScan = true
                checkCameraPermissions()
            }
            R.id.btn2 -> {
                cls = CaptureFragmentActivity::class.java
                title = (v as Button).text.toString()
                checkCameraPermissions()
            }
            R.id.btn3 -> {
                cls = EasyCaptureActivity::class.java
                title = (v as Button).text.toString()
                checkCameraPermissions()
            }
            R.id.btn4 -> {
                cls = CustomActivity::class.java
                title = (v as Button).text.toString()
                checkCameraPermissions()
            }
            R.id.btn5 -> startCode(false)
            R.id.btn6 -> startCode(true)
            R.id.btn7 -> checkExternalStoragePermissions()
        }
    }

    companion object {
        const val KEY_TITLE = "key_title"
        const val KEY_IS_QR_CODE = "key_code"
        const val KEY_IS_CONTINUOUS = "key_continuous_scan"
        const val REQUEST_CODE_SCAN = 0X01
        const val REQUEST_CODE_PHOTO = 0X02
        const val RC_CAMERA = 0X01
        const val RC_READ_PHOTO = 0X02
    }
}