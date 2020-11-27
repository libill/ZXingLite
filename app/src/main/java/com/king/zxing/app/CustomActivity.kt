package com.king.zxing.app

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.king.zxing.CaptureHelper
import com.king.zxing.OnCaptureCallback
import com.king.zxing.ViewfinderView
import com.king.zxing.app.util.immersiveStatusBar

/**
 * 自定义扫码：当直接使用CaptureActivity
 * 自定义扫码，切记自定义扫码需在[Activity]或者[Fragment]相对应的生命周期里面调用[.mCaptureHelper]对应的生命周期
 * @author [Jenly](mailto:jenly1314@gmail.com)
 */
class CustomActivity : AppCompatActivity(), OnCaptureCallback {
    private var isContinuousScan = false
    private var mCaptureHelper: CaptureHelper? = null
    private var surfaceView: SurfaceView? = null
    private var viewfinderView: ViewfinderView? = null
    private var ivTorch: View? = null
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.custom_activity)
        initUI()
    }

    private fun initUI() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        immersiveStatusBar(this, toolbar, 0.2f)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        tvTitle.text = intent.getStringExtra(MainActivity.Companion.KEY_TITLE)
        surfaceView = findViewById(R.id.surfaceView)
        viewfinderView = findViewById(R.id.viewfinderView)
        ivTorch = findViewById(R.id.ivFlash)
        ivTorch?.visibility = View.INVISIBLE
        isContinuousScan = intent.getBooleanExtra(MainActivity.Companion.KEY_IS_CONTINUOUS, false)
        mCaptureHelper = CaptureHelper(this, surfaceView, viewfinderView, ivTorch)
        mCaptureHelper!!.setOnCaptureCallback(this)
        mCaptureHelper!!.onCreate()
        mCaptureHelper!!.vibrate(true)
            .fullScreenScan(true) //全屏扫码
            .supportVerticalCode(true) //支持扫垂直条码，建议有此需求时才使用。
            .supportLuminanceInvert(true) //是否支持识别反色码（黑白反色的码），增加识别率
            .continuousScan(isContinuousScan)
    }

    override fun onResume() {
        super.onResume()
        mCaptureHelper!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mCaptureHelper!!.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCaptureHelper!!.onDestroy()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mCaptureHelper!!.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    /**
     * 扫码结果回调
     * @param result 扫码结果
     * @return
     */
    override fun onResultCallback(result: String): Boolean {
        if (isContinuousScan) {
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
        }
        return false
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.ivLeft -> onBackPressed()
        }
    }
}