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
package com.king.zxing.app.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.annotation.FloatRange
import androidx.appcompat.widget.Toolbar
import com.king.zxing.app.R

/**
 * @author Jenly [Jenly](mailto:jenly1314@gmail.com)
 */

fun immersiveStatusBar(
    activity: Activity, toolbar: Toolbar?,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 0.0f
) {
    val window = activity.window
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    } else {
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    }
    val decorView = window.decorView as ViewGroup
    val contentView =
        window.decorView.findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT)
    val rootView = contentView.getChildAt(0)
    if (rootView != null) {
        rootView.fitsSystemWindows = false
    }
    toolbar?.setPadding(0, getStatusBarHeight(activity), 0, 0)
    decorView.addView(createStatusBarView(activity, alpha))
}

private fun createStatusBarView(
    activity: Activity, @FloatRange(from = 0.0, to = 1.0) alpha: Float
): View {
    // 绘制一个和状态栏一样高的矩形
    val statusBarView = View(activity)
    val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        getStatusBarHeight(activity))
    statusBarView.layoutParams = params
    statusBarView.setBackgroundColor(Color.argb((alpha * 255).toInt(), 0, 0, 0))
    statusBarView.id = R.id.translucent_view
    return statusBarView
}

/** 获取状态栏高度  */
fun getStatusBarHeight(context: Context): Int {
    return context.resources.getDimensionPixelSize(R.dimen.status_bar_height)
}