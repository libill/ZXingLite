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
package com.king.zxing

import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.king.zxing.Intents.Scan
import com.king.zxing.camera.CameraManager

/**
 * @author [Jenly](mailto:jenly1314@gmail.com)
 */
open class CaptureActivity : AppCompatActivity(), OnCaptureCallback {
    private var surfaceView: SurfaceView? = null
    private var viewfinderView: ViewfinderView? = null
    private var ivTorch: View? = null

    /**
     * Get [CaptureHelper]
     * @return [.mCaptureHelper]
     */
    var captureHelper: CaptureHelper? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layoutId = layoutId
        if (isContentView(layoutId)) {
            setContentView(layoutId)
        }
        initUI()
        captureHelper!!.onCreate()
    }

    /**
     * 初始化
     */
    fun initUI() {
        surfaceView = findViewById(surfaceViewId)
        val viewfinderViewId = viewfinderViewId
        if (viewfinderViewId != 0) {
            viewfinderView = findViewById(viewfinderViewId)
        }
        val ivTorchId = ivTorchId
        if (ivTorchId != 0) {
            ivTorch = findViewById(ivTorchId)
            ivTorch?.setVisibility(View.INVISIBLE)
        }
        initCaptureHelper()
    }

    fun initCaptureHelper() {
        captureHelper = CaptureHelper(this, surfaceView, viewfinderView, ivTorch)
        captureHelper!!.setOnCaptureCallback(this)
    }

    /**
     * 返回true时会自动初始化[.setContentView]，返回为false是需自己去初始化[.setContentView]
     * @param layoutId
     * @return 默认返回true
     */
    fun isContentView(@LayoutRes layoutId: Int): Boolean {
        return true
    }

    /**
     * 布局id
     * @return
     */
    open val layoutId: Int
        get() = R.layout.zxl_capture

    /**
     * [.viewfinderView] 的 ID
     * @return 默认返回{@code R.id.viewfinderView}, 如果不需要扫码框可以返回0
     */
    val viewfinderViewId: Int
        get() = R.id.viewfinderView

    /**
     * 预览界面[.surfaceView] 的ID
     * @return
     */
    val surfaceViewId: Int
        get() = R.id.surfaceView

    /**
     * 获取 [.ivTorch] 的ID
     * @return  默认返回{@code R.id.ivTorch}, 如果不需要手电筒按钮可以返回0
     */
    val ivTorchId: Int
        get() = R.id.ivTorch

    /**
     * Get [CameraManager] use [.getCaptureHelper]
     * @return [.mCaptureHelper]
     */
    @get:Deprecated("")
    val cameraManager: CameraManager?
        get() = captureHelper?.cameraManager

    public override fun onResume() {
        super.onResume()
        captureHelper!!.onResume()
    }

    public override fun onPause() {
        super.onPause()
        captureHelper!!.onPause()
    }

    public override fun onDestroy() {
        super.onDestroy()
        captureHelper!!.onDestroy()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        captureHelper!!.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    /**
     * 接收扫码结果回调
     * @param result 扫码结果
     * @return 返回true表示拦截，将不自动执行后续逻辑，为false表示不拦截，默认不拦截
     */
    override fun onResultCallback(result: String?): Boolean {
        return false
    }

    companion object {
        val KEY_RESULT: String? = Scan.RESULT
    }
}