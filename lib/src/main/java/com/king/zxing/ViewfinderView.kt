package com.king.zxing

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.google.zxing.ResultPoint
import com.king.zxing.ViewfinderView.LaserStyle
import com.king.zxing.ViewfinderView.TextLocation
import java.util.ArrayList

/*
 * Copyright (C) 2008 ZXing authors
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
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
class ViewfinderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    /**
     * 画笔
     */
    private var paint: Paint? = null

    /**
     * 文本画笔
     */
    private var textPaint: TextPaint? = null

    /**
     * 扫码框外面遮罩颜色
     */
    private var maskColor = 0

    /**
     * 扫描区域边框颜色
     */
    private var frameColor = 0

    /**
     * 扫描线颜色
     */
    private var laserColor = 0

    /**
     * 扫码框四角颜色
     */
    private var cornerColor = 0

    /**
     * 结果点颜色
     */
    private var resultPointColor = 0

    /**
     * 提示文本与扫码框的边距
     */
    private var labelTextPadding = 0f

    /**
     * 提示文本的位置
     */
    private var labelTextLocation: TextLocation? = null

    /**
     * 扫描区域提示文本
     */
    private var labelText: String? = null

    /**
     * 扫描区域提示文本颜色
     */
    private var labelTextColor = 0

    /**
     * 提示文本字体大小
     */
    private var labelTextSize = 0f

    /**
     * 扫描线开始位置
     */
    var scannerStart = 0

    /**
     * 扫描线结束位置
     */
    var scannerEnd = 0

    /**
     * 设置显示结果点
     * @param showResultPoint 是否显示结果点
     */
    /**
     * 是否显示结果点
     */
    var isShowResultPoint = false

    /**
     * 屏幕宽
     */
    private var screenWidth = 0

    /**
     * 屏幕高
     */
    private var screenHeight = 0

    /**
     * 扫码框宽
     */
    private var frameWidth = 0

    /**
     * 扫码框高
     */
    private var frameHeight = 0

    /**
     * 扫描激光线风格
     */
    private var laserStyle: LaserStyle? = null

    /**
     * 网格列数
     */
    private var gridColumn = 0

    /**
     * 网格高度
     */
    private var gridHeight = 0

    /**
     * 扫码框
     */
    private var frame: Rect? = null

    /**
     * 扫描区边角的宽
     */
    private var cornerRectWidth = 0

    /**
     * 扫描区边角的高
     */
    private var cornerRectHeight = 0

    /**
     * 扫描线每次移动距离
     */
    private var scannerLineMoveDistance = 0

    /**
     * 扫描线高度
     */
    private var scannerLineHeight = 0

    /**
     * 边框线宽度
     */
    private var frameLineWidth = 0

    /**
     * 扫描动画延迟间隔时间 默认15毫秒
     */
    private var scannerAnimationDelay = 0

    /**
     * 扫码框占比
     */
    private var frameRatio = 0f
    private var possibleResultPoints: MutableList<ResultPoint>? = null
    private var lastPossibleResultPoints: List<ResultPoint>? = null

    enum class LaserStyle(val mValue: Int) {
        NONE(0), LINE(1), GRID(2);

        companion object {
            fun getFromInt(value: Int): LaserStyle {
                for (style in values()) {
                    if (style.mValue == value) {
                        return style
                    }
                }
                return LINE
            }
        }
    }

    enum class TextLocation(private val mValue: Int) {
        TOP(0), BOTTOM(1);

        companion object {
            fun getFromInt(value: Int): TextLocation {
                for (location in values()) {
                    if (location.mValue == value) {
                        return location
                    }
                }
                return TOP
            }
        }
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        //初始化自定义属性信息
        val array = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderView)
        maskColor = array.getColor(R.styleable.ViewfinderView_maskColor,
            ContextCompat.getColor(context, R.color.viewfinder_mask))
        frameColor = array.getColor(R.styleable.ViewfinderView_frameColor,
            ContextCompat.getColor(context, R.color.viewfinder_frame))
        cornerColor = array.getColor(R.styleable.ViewfinderView_cornerColor,
            ContextCompat.getColor(context, R.color.viewfinder_corner))
        laserColor = array.getColor(R.styleable.ViewfinderView_laserColor,
            ContextCompat.getColor(context, R.color.viewfinder_laser))
        resultPointColor = array.getColor(R.styleable.ViewfinderView_resultPointColor,
            ContextCompat.getColor(context, R.color.viewfinder_result_point_color))
        labelText = array.getString(R.styleable.ViewfinderView_labelText)
        labelTextColor = array.getColor(R.styleable.ViewfinderView_labelTextColor,
            ContextCompat.getColor(context, R.color.viewfinder_text_color))
        labelTextSize = array.getDimension(R.styleable.ViewfinderView_labelTextSize,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics))
        labelTextPadding = array.getDimension(R.styleable.ViewfinderView_labelTextPadding,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics))
        labelTextLocation =
            TextLocation.getFromInt(array.getInt(R.styleable.ViewfinderView_labelTextLocation, 0))
        isShowResultPoint = array.getBoolean(R.styleable.ViewfinderView_showResultPoint, false)
        frameWidth = array.getDimensionPixelSize(R.styleable.ViewfinderView_frameWidth, 0)
        frameHeight = array.getDimensionPixelSize(R.styleable.ViewfinderView_frameHeight, 0)
        laserStyle =
            LaserStyle.getFromInt(array.getInt(R.styleable.ViewfinderView_laserStyle, LaserStyle.LINE.mValue))
        gridColumn = array.getInt(R.styleable.ViewfinderView_gridColumn, 20)
        gridHeight = array.getDimension(R.styleable.ViewfinderView_gridHeight,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40f, resources.displayMetrics)).toInt()
        cornerRectWidth = array.getDimension(R.styleable.ViewfinderView_cornerRectWidth,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics)).toInt()
        cornerRectHeight = array.getDimension(R.styleable.ViewfinderView_cornerRectHeight,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)).toInt()
        scannerLineMoveDistance = array.getDimension(R.styleable.ViewfinderView_scannerLineMoveDistance,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)).toInt()
        scannerLineHeight = array.getDimension(R.styleable.ViewfinderView_scannerLineHeight,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics)).toInt()
        frameLineWidth = array.getDimension(R.styleable.ViewfinderView_frameLineWidth,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)).toInt()
        scannerAnimationDelay = array.getInteger(R.styleable.ViewfinderView_scannerAnimationDelay, 15)
        frameRatio = array.getFloat(R.styleable.ViewfinderView_frameRatio, 0.625f)
        array.recycle()
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        possibleResultPoints = ArrayList(5)
        lastPossibleResultPoints = null
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        val size = (Math.min(screenWidth, screenHeight) * frameRatio).toInt()
        if (frameWidth <= 0 || frameWidth > screenWidth) {
            frameWidth = size
        }
        if (frameHeight <= 0 || frameHeight > screenHeight) {
            frameHeight = size
        }
    }

    private val displayMetrics: DisplayMetrics
        private get() = resources.displayMetrics

    fun setLabelText(labelText: String?) {
        this.labelText = labelText
    }

    fun setLabelTextColor(@ColorInt color: Int) {
        labelTextColor = color
    }

    fun setLabelTextColorResource(@ColorRes id: Int) {
        labelTextColor = ContextCompat.getColor(context, id)
    }

    fun setLabelTextSize(textSize: Float) {
        labelTextSize = textSize
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        //扫码框默认居中，支持利用内距偏移扫码框
        val leftOffset = (screenWidth - frameWidth) / 2 + paddingLeft - paddingRight
        val topOffset = (screenHeight - frameHeight) / 2 + paddingTop - paddingBottom
        frame = Rect(leftOffset, topOffset, leftOffset + frameWidth, topOffset + frameHeight)
    }

    public override fun onDraw(canvas: Canvas) {
        if (frame == null) {
            return
        }
        if (scannerStart == 0 || scannerEnd == 0) {
            scannerStart = frame!!.top
            scannerEnd = frame!!.bottom - scannerLineHeight
        }
        val width = canvas.width
        val height = canvas.height

        // Draw the exterior (i.e. outside the framing rect) darkened
        drawExterior(canvas, frame!!, width, height)
        // Draw a red "laser scanner" line through the middle to show decoding is active
        drawLaserScanner(canvas, frame!!)
        // Draw a two pixel solid black border inside the framing rect
        drawFrame(canvas, frame!!)
        // 绘制边角
        drawCorner(canvas, frame!!)
        //绘制提示信息
        drawTextInfo(canvas, frame!!)
        //绘制扫码结果点
        drawResultPoint(canvas, frame!!)
        // Request another update at the animation interval, but only repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(scannerAnimationDelay.toLong(),
            frame!!.left - POINT_SIZE,
            frame!!.top - POINT_SIZE,
            frame!!.right + POINT_SIZE,
            frame!!.bottom + POINT_SIZE)
    }

    /**
     * 绘制文本
     * @param canvas
     * @param frame
     */
    private fun drawTextInfo(canvas: Canvas, frame: Rect) {
        if (!TextUtils.isEmpty(labelText)) {
            textPaint!!.color = labelTextColor
            textPaint!!.textSize = labelTextSize
            textPaint!!.textAlign = Paint.Align.CENTER
            val staticLayout =
                StaticLayout(labelText, textPaint, canvas.width, Layout.Alignment.ALIGN_NORMAL, 1.0f,
                    0.0f, true)
            if (labelTextLocation == TextLocation.BOTTOM) {
                canvas.translate(frame.left + frame.width() / 2.toFloat(), frame.bottom + labelTextPadding)
                staticLayout.draw(canvas)
            } else {
                canvas.translate(frame.left + frame.width() / 2.toFloat(),
                    frame.top - labelTextPadding - staticLayout.height)
                staticLayout.draw(canvas)
            }
        }
    }

    /**
     * 绘制边角
     * @param canvas
     * @param frame
     */
    private fun drawCorner(canvas: Canvas, frame: Rect) {
        paint!!.color = cornerColor
        //左上
        canvas.drawRect(frame.left.toFloat(), frame.top.toFloat(), frame.left + cornerRectWidth.toFloat(),
            frame.top + cornerRectHeight.toFloat(), paint)
        canvas.drawRect(frame.left.toFloat(), frame.top.toFloat(), frame.left + cornerRectHeight.toFloat(),
            frame.top + cornerRectWidth.toFloat(), paint)
        //右上
        canvas.drawRect(frame.right - cornerRectWidth.toFloat(), frame.top.toFloat(), frame.right.toFloat(),
            frame.top + cornerRectHeight.toFloat(), paint)
        canvas.drawRect(frame.right - cornerRectHeight.toFloat(), frame.top.toFloat(), frame.right.toFloat(),
            frame.top + cornerRectWidth.toFloat(), paint)
        //左下
        canvas.drawRect(frame.left.toFloat(), frame.bottom - cornerRectWidth.toFloat(),
            frame.left + cornerRectHeight.toFloat(), frame.bottom.toFloat(), paint)
        canvas.drawRect(frame.left.toFloat(), frame.bottom - cornerRectHeight.toFloat(),
            frame.left + cornerRectWidth.toFloat(), frame.bottom.toFloat(), paint)
        //右下
        canvas.drawRect(frame.right - cornerRectWidth.toFloat(), frame.bottom - cornerRectHeight.toFloat(),
            frame.right.toFloat(), frame.bottom.toFloat(), paint)
        canvas.drawRect(frame.right - cornerRectHeight.toFloat(), frame.bottom - cornerRectWidth.toFloat(),
            frame.right.toFloat(), frame.bottom.toFloat(), paint)
    }

    /**
     * 绘制激光扫描线
     * @param canvas
     * @param frame
     */
    private fun drawLaserScanner(canvas: Canvas, frame: Rect) {
        if (laserStyle != null) {
            paint!!.color = laserColor
            when (laserStyle) {
                LaserStyle.LINE -> drawLineScanner(canvas, frame)
                LaserStyle.GRID -> drawGridScanner(canvas, frame)
            }
            paint!!.shader = null
        }
    }

    /**
     * 绘制线性式扫描
     * @param canvas
     * @param frame
     */
    private fun drawLineScanner(canvas: Canvas, frame: Rect) {
        //线性渐变
        val linearGradient = LinearGradient(
            frame.left.toFloat(), scannerStart.toFloat(),
            frame.left.toFloat(), (scannerStart + scannerLineHeight).toFloat(),
            shadeColor(laserColor),
            laserColor,
            Shader.TileMode.MIRROR)
        paint!!.shader = linearGradient
        if (scannerStart <= scannerEnd) {
            //椭圆
            val rectF =
                RectF(
                    (frame.left + 2 * scannerLineHeight).toFloat(), scannerStart.toFloat(),
                    (frame.right - 2 * scannerLineHeight).toFloat(),
                    (scannerStart + scannerLineHeight).toFloat())
            canvas.drawOval(rectF, paint)
            scannerStart += scannerLineMoveDistance
        } else {
            scannerStart = frame.top
        }
    }

    /**
     * 绘制网格式扫描
     * @param canvas
     * @param frame
     */
    private fun drawGridScanner(canvas: Canvas, frame: Rect) {
        val stroke = 2
        paint!!.strokeWidth = stroke.toFloat()
        //计算Y轴开始位置
        val startY =
            if (gridHeight > 0 && scannerStart - frame.top > gridHeight) scannerStart - gridHeight else frame.top
        val linearGradient =
            LinearGradient((frame.left + frame.width() / 2).toFloat(), startY.toFloat(),
                (frame.left + frame.width() / 2).toFloat(), scannerStart.toFloat(),
                intArrayOf(shadeColor(laserColor), laserColor), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        //给画笔设置着色器
        paint!!.shader = linearGradient
        val wUnit = frame.width() * 1.0f / gridColumn
        //遍历绘制网格纵线
        for (i in 1 until gridColumn) {
            canvas.drawLine(frame.left + i * wUnit, startY.toFloat(), frame.left + i * wUnit, scannerStart.toFloat(),
                paint)
        }
        val height =
            if (gridHeight > 0 && scannerStart - frame.top > gridHeight) gridHeight else scannerStart - frame.top

        //遍历绘制网格横线
        var i = 0
        while (i <= height / wUnit) {
            canvas.drawLine(frame.left.toFloat(), scannerStart - i * wUnit, frame.right.toFloat(),
                scannerStart - i * wUnit, paint)
            i++
        }
        if (scannerStart < scannerEnd) {
            scannerStart += scannerLineMoveDistance
        } else {
            scannerStart = frame.top
        }
    }

    /**
     * 处理颜色模糊
     * @param color
     * @return
     */
    fun shadeColor(color: Int): Int {
        val hax = Integer.toHexString(color)
        val result = "01" + hax.substring(2)
        return Integer.valueOf(result, 16)
    }

    /**
     * 绘制扫描区边框
     * @param canvas
     * @param frame
     */
    private fun drawFrame(canvas: Canvas, frame: Rect) {
        paint!!.color = frameColor
        canvas.drawRect(frame.left.toFloat(), frame.top.toFloat(), frame.right.toFloat(),
            frame.top + frameLineWidth.toFloat(), paint)
        canvas.drawRect(frame.left.toFloat(), frame.top.toFloat(), frame.left + frameLineWidth.toFloat(),
            frame.bottom.toFloat(), paint)
        canvas.drawRect(frame.right - frameLineWidth.toFloat(), frame.top.toFloat(), frame.right.toFloat(),
            frame.bottom.toFloat(), paint)
        canvas.drawRect(frame.left.toFloat(), frame.bottom - frameLineWidth.toFloat(), frame.right.toFloat(),
            frame.bottom.toFloat(), paint)
    }

    /**
     * 绘制模糊区域
     * @param canvas
     * @param frame
     * @param width
     * @param height
     */
    private fun drawExterior(
        canvas: Canvas, frame: Rect, width: Int, height: Int
    ) {
        paint!!.color = maskColor
        canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), paint)
        canvas.drawRect(0f, frame.top.toFloat(), frame.left.toFloat(), frame.bottom.toFloat(), paint)
        canvas.drawRect(frame.right.toFloat(), frame.top.toFloat(), width.toFloat(), frame.bottom.toFloat(), paint)
        canvas.drawRect(0f, frame.bottom.toFloat(), width.toFloat(), height.toFloat(), paint)
    }

    /**
     * 绘制扫码结果点
     * @param canvas
     * @param frame
     */
    private fun drawResultPoint(canvas: Canvas, frame: Rect) {
        if (!isShowResultPoint) {
            return
        }
        val currentPossible: List<ResultPoint>? = possibleResultPoints
        val currentLast = lastPossibleResultPoints
        if (currentPossible!!.isEmpty()) {
            lastPossibleResultPoints = null
        } else {
            possibleResultPoints = ArrayList(5)
            lastPossibleResultPoints = currentPossible
            paint!!.alpha = CURRENT_POINT_OPACITY
            paint!!.color = resultPointColor
            synchronized(currentPossible) {
                val radius = POINT_SIZE / 2.0f
                for (point in currentPossible) {
                    canvas.drawCircle(point.x, point.y, radius, paint)
                }
            }
        }
        if (currentLast != null) {
            paint!!.alpha = CURRENT_POINT_OPACITY / 2
            paint!!.color = resultPointColor
            synchronized(currentLast) {
                val radius = POINT_SIZE / 2.0f
                for (point in currentLast) {
                    canvas.drawCircle(point.x, point.y, radius, paint)
                }
            }
        }
    }

    fun drawViewfinder() {
        invalidate()
    }

    fun setLaserStyle(laserStyle: LaserStyle?) {
        this.laserStyle = laserStyle
    }

    fun addPossibleResultPoint(point: ResultPoint) {
        if (isShowResultPoint) {
            val points = possibleResultPoints
            synchronized(points!!) {
                points.add(point)
                val size = points.size
                if (size > MAX_RESULT_POINTS) {
                    // trim it
                    points.subList(0, size - MAX_RESULT_POINTS / 2).clear()
                }
            }
        }
    }

    companion object {
        private const val CURRENT_POINT_OPACITY = 0xA0
        private const val MAX_RESULT_POINTS = 20
        private const val POINT_SIZE = 20
    }

    init {
        init(context, attrs)
    }
}