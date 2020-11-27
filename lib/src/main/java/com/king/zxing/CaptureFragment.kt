/*
 * Copyright (C) 2019 Jenly Yu
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
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.king.zxing.Intents.Scan
import com.king.zxing.camera.CameraManager

/**
 * @author [Jenly](mailto:jenly1314@gmail.com)
 */
class CaptureFragment : Fragment(), OnCaptureCallback {
    //--------------------------------------------
    var rootView: View? = null
    private var surfaceView: SurfaceView? = null
    private var viewfinderView: ViewfinderView? = null
    private var ivTorch: View? = null

    /**
     * Get [CaptureHelper]
     * @return [.mCaptureHelper]
     */
    var captureHelper: CaptureHelper? = null
        private set

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val layoutId = layoutId
        if (isContentView(layoutId)) {
            rootView = inflater.inflate(layoutId, container, false)
        }
        initUI()
        return rootView
    }

    /**
     * 初始化
     */
    fun initUI() {
        surfaceView = rootView!!.findViewById(surfaceViewId)
        val viewfinderViewId = viewfinderViewId
        if (viewfinderViewId != 0) {
            viewfinderView = rootView!!.findViewById(viewfinderViewId)
        }
        val ivTorchId = ivTorchId
        if (ivTorchId != 0) {
            ivTorch = rootView!!.findViewById(ivTorchId)
            ivTorch?.setVisibility(View.INVISIBLE)
        }
        initCaptureHelper()
    }

    fun initCaptureHelper() {
        captureHelper = CaptureHelper(this, surfaceView, viewfinderView, ivTorch)
        captureHelper!!.setOnCaptureCallback(this)
    }

    /**
     * 返回true时会自动初始化[.mRootView]，返回为false时需自己去通过[.setRootView]初始化[.mRootView]
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
    val layoutId: Int
        get() = R.layout.zxl_capture

    /**
     * [ViewfinderView] 的 id
     * @return 默认返回{@code R.id.viewfinderView}, 如果不需要扫码框可以返回0
     */
    val viewfinderViewId: Int
        get() = R.id.viewfinderView

    /**
     * 预览界面[.surfaceView] 的id
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

    //--------------------------------------------
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        captureHelper!!.onCreate()
    }

    override fun onResume() {
        super.onResume()
        captureHelper!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        captureHelper!!.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        captureHelper!!.onDestroy()
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
        fun newInstance(): CaptureFragment {
            val args = Bundle()
            val fragment = CaptureFragment()
            fragment.arguments = args
            return fragment
        }
    }
}